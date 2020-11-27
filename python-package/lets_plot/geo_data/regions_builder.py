from typing import Optional, List, Union, Tuple

from .gis.geocoding_service import GeocodingService
from .gis.geometry import GeoPoint
from .gis.request import MapRegion, RegionQuery, RequestBuilder, RequestKind, PayloadKind, AmbiguityResolver, \
    IgnoringStrategyKind
from .gis.response import LevelKind, Response, SuccessResponse, GeoRect
from .regions import _to_level_kind, request_types, parent_types, scope_types, Regions, _raise_exception, \
    _ensure_is_list, _make_parent_region, _to_scope

NAMESAKE_MAX_COUNT = 10

ShapelyPointType = 'shapely.geometry.Point'
ShapelyPolygonType = 'shapely.geometry.Polygon'


def _make_ambiguity_resolver(ignoring_strategy: Optional[IgnoringStrategyKind] = None,
                             within: ShapelyPolygonType = None,
                             near: Optional[Union[Regions, ShapelyPointType]] = None):
    box = None
    if LazyShapely.is_polygon(within):
        box = GeoRect(min_lon=within.bounds[0], min_lat=within.bounds[1], max_lon=within.bounds[2], max_lat=within.bounds[3])

    near = _to_near_coord(near)

    return AmbiguityResolver(
        ignoring_strategy=ignoring_strategy,
        closest_coord=near,
        box=box
    )


def _to_near_coord(near: Optional[Union[Regions, ShapelyPointType]]) -> Optional[GeoPoint]:
    if near is None:
        return None

    if isinstance(near, Regions):
        near_id = near.as_list()[0].unique_ids()
        assert len(near_id) == 1

        request = RequestBuilder() \
            .set_request_kind(RequestKind.explicit) \
            .set_requested_payload([PayloadKind.centroids]) \
            .set_ids(near_id) \
            .build()

        response: Response = GeocodingService().do_request(request)
        if isinstance(response, SuccessResponse):
            assert len(response.features) == 1
            centroid = response.features[0].centroid
            return GeoPoint(lon=centroid.lon, lat=centroid.lat)
        else:
            raise ValueError("Unexpected geocoding response for id " + str(near_id[0]))

    if LazyShapely.is_point(near):
        return GeoPoint(lon=near.x, lat=near.y)

    raise ValueError('Not supported type: {}'.format(type(near)))


def _split(box: Optional[Union[str, List[str], Regions, List[Regions], ShapelyPolygonType]]) -> Tuple[
    scope_types, Optional[GeoRect]]:
    if not LazyShapely.is_polygon(box):
        return box, None
    else:
        return None, GeoRect(min_lon=box.bounds[0], min_lat=box.bounds[1], max_lon=box.bounds[2], max_lat=box.bounds[3])

def _create_new_queries(
        request: request_types,
        ambiguity_resovler: AmbiguityResolver,
        countries: parent_types = None,
        states: parent_types = None,
        counties: parent_types = None
) -> List[RegionQuery]:
    requests: Optional[List[str]] = _ensure_is_list(request)
    def ensure_is_parent_list(obj):
        if obj is None:
            return None

        if isinstance(obj, str):
            return [obj]
        if isinstance(obj, Regions):
            return obj.as_list()

        if isinstance(obj, list):
            return obj

        return [obj]

    countries = ensure_is_parent_list(countries)
    states = ensure_is_parent_list(states)
    counties = ensure_is_parent_list(counties)

    if countries is not None and len(countries) != len(requests):
        raise ValueError('Countries count({}) != names count({})'.format(len(countries), len(requests)))

    if states is not None and len(states) != len(requests):
        raise ValueError('States count({}) != names count({})'.format(len(states), len(requests)))

    if counties is not None and len(counties) != len(requests):
        raise ValueError('Counties count({}) != names count({})'.format(len(counties), len(requests)))

    queries = []
    for i in range(len(requests)):
        name = requests[i] if requests is not None else None
        country = _make_parent_region(countries[i]) if countries is not None else None
        state = _make_parent_region(states[i]) if states is not None else None
        county = _make_parent_region(counties[i]) if counties is not None else None

        query = RegionQuery(request=name, scope=None, ambiguity_resolver=ambiguity_resovler,
                            country=country, state=state, county=county)

        queries.append(query)

    return queries


def _create_queries(
        request: request_types,
        scope: scope_types,
        ambiguity_resovler: AmbiguityResolver
) -> List[RegionQuery]:

    requests: Optional[List[str]] = _ensure_is_list(request)

    scopes: Optional[Union[List[MapRegion], MapRegion]] = _to_scope(scope)
    positional_matching = isinstance(scopes, list)

    if positional_matching:
        if requests is not None and len(requests) != len(scopes):
            raise ValueError('Length of request and scope is not equal')

        return [
            RegionQuery(r, s, ambiguity_resovler) for r, s in zip(requests, scopes)
        ]
    else:
        # us-48 request - no requests, only scopes
        if requests is None and scopes is not None:
            return [RegionQuery(None, scopes, ambiguity_resovler)]

        # countries request - no requests and scopes
        if requests is None and scopes is None:
            return []

        return [RegionQuery(r, scopes, ambiguity_resovler) for r in requests]


class LazyShapely:
    @staticmethod
    def is_point(p) -> bool:
        if not LazyShapely._is_shapely_available():
            return False

        from shapely.geometry import Point
        return isinstance(p, Point)

    @staticmethod
    def is_polygon(p):
        if not LazyShapely._is_shapely_available():
            return False

        from shapely.geometry import Polygon
        return isinstance(p, Polygon)

    @staticmethod
    def _is_shapely_available():
        try:
            import shapely
            return True
        except:
            return False


class RegionsBuilder:
    def __init__(self,
                 level: Optional[Union[str, LevelKind]] = None,
                 request: request_types = None,
                 scope: scope_types = None,
                 highlights: bool = False,
                 allow_ambiguous=False
                 ):

        self._level: Optional[LevelKind] = _to_level_kind(level)
        self._default_ambiguity_resolver: AmbiguityResolver = AmbiguityResolver.empty()  # TODO rename to geohint
        self._highlights: bool = highlights
        self._allow_ambiguous = allow_ambiguous
        self._overridings: List[RegionQuery] = []

        self._scope: List[MapRegion] = []
        self._queries: List[RegionQuery] = _create_queries(request, scope, self._default_ambiguity_resolver)


    def drop_not_found(self) -> 'RegionsBuilder':
        self._default_ambiguity_resolver = AmbiguityResolver(IgnoringStrategyKind.skip_missing)
        return self

    def drop_not_matched(self) -> 'RegionsBuilder':
        self._default_ambiguity_resolver = AmbiguityResolver(IgnoringStrategyKind.skip_all)
        return self

    def allow_ambiguous(self) -> 'RegionsBuilder':
        self._default_ambiguity_resolver = AmbiguityResolver(IgnoringStrategyKind.take_namesakes)
        self._allow_ambiguous = True
        return self

    def where(self,
              request: request_types = None,
              within: Optional[Union[str, List[str], Regions, List[Regions], ShapelyPolygonType]] = None,
              near: Optional[Union[Regions, ShapelyPointType]] = None,
              ) -> 'RegionsBuilder':
        """
        If request is not exist - append it to a list with specified scope.
        If request is already exist in the list - specify scope exactly for that request.

        where(request, scope)

        Parameters
        ----------
        request : [array | string | None]
            Data can be filtered by full names at any level (only exact matching).
            For 'state' level:
            -'US-48' returns continental part of United States (48 states) in a compact form.
        within : [array | string | Regions | shapely.Polygon | None]
            Data can be filtered by scope name or polygon.
            'US-48' includes continental part of United States (48 states).
        near: [Regions | None]
            Resolve ambiguity by taking object closest to a 'near' object.


        Returns
        -------
            RegionsBuilder object

        Note
        -----
        If request is mixed with existing and new items scope will be specified for all of them.
        It is allowed to specify scope by chaihing calls of the where method as many times as needed
        while trying to resolve an ambiguity. Latest call defines the value, used by geocoding.

        Examples
        ---------
        >>> from lets_plot.geo_data import *
        >>> r = regions(level='country', request=['Germany', 'USA'])
        """

        scope, box = _split(within)
        ambiguity_resolver = AmbiguityResolver(None, _to_near_coord(near), box)

        new_overridings = _create_queries(request, scope, ambiguity_resolver)
        for overriding in self._overridings:
            if overriding.request in set([overriding.request for overriding in new_overridings]):
                self._overridings.remove(overriding)

        self._overridings.extend(new_overridings)

        return self

    def build(self) -> Regions:
        queries = self._get_queries()
        request = RequestBuilder() \
            .set_request_kind(RequestKind.geocoding) \
            .set_requested_payload([PayloadKind.highlights] if self._highlights else []) \
            .set_queries(queries) \
            .set_scope(self._scope) \
            .set_level(self._level) \
            .set_namesake_limit(NAMESAKE_MAX_COUNT) \
            .set_allow_ambiguous(self._allow_ambiguous) \
            .build()

        response: Response = GeocodingService().do_request(request)

        if not isinstance(response, SuccessResponse):
            _raise_exception(response)

        return Regions(response.level, response.answers, queries, self._highlights)

    def _get_queries(self) -> List[RegionQuery]:
        for overriding in self._overridings:
            overriding_did_not_match = True  # if overriding.name is not found in self._names just add it as new query
            for query in self._queries:
                if query.request == overriding.request:
                    query.scope = overriding.scope
                    query.ambiguity_resolver = overriding.ambiguity_resolver
                    overriding_did_not_match = False

            if overriding_did_not_match:
                self._queries.append(overriding)

        if len(self._queries) == 0:
            return [RegionQuery(request=None, scope=None, ambiguity_resolver=self._default_ambiguity_resolver)]

        return [
            RegionQuery(
                q.request,
                q.scope,
                q.ambiguity_resolver if q.ambiguity_resolver != AmbiguityResolver.empty() else self._default_ambiguity_resolver,
                country=q.country,
                state=q.state,
                county=q.county
            )
            for q in self._queries
        ]

    def __eq__(self, o):
        return isinstance(o, RegionsBuilder) \
               and self._overridings == o._overridings

    def __ne__(self, o):
        return not self == o
