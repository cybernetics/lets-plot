#
# Copyright (c) 2019. JetBrains s.r.o.
# Use of this source code is governed by the MIT license that can be found in the LICENSE file.
#
from typing import Dict, Any

from ._frontend_ctx import FrontendContext
from ._html_contexts import _create_html_frontend_context, _use_isolated_frame
from ._json_contexts import _create_json_frontend_context, _is_Intellij_Python_Lets_Plot_Plugin
from ._mime_types import TEXT_HTML, LETS_PLOT_JSON
from .._global_settings import get_global_bool
from ..plot.core import PlotSpec
from ..plot.plot import GGBunch

__all__ = [
    # 'load_lets_plot_js'  # deprecated
]

_frontend_contexts: Dict[str, FrontendContext] = {}

_default_mimetype = TEXT_HTML
if _is_Intellij_Python_Lets_Plot_Plugin():
    _default_mimetype = LETS_PLOT_JSON
    _frontend_contexts[LETS_PLOT_JSON] = _create_json_frontend_context()


def _setup_html_context(isolated_frame: bool = None, offline: bool = None, show_status: bool = False) -> None:
    """
    Configures Lets-Plot HTML output.

    Parameters
    ----------
    isolated_frame : bool, optional, default None - auto-detect
        If `True`, generate HTLM which can be used in `iframe` or in a standalone HTML document
        If `False`, pre-load Lets-Plot JS library. Notebook cell output will only consist of HTML for the plot rendering.

    offline : bool, optional, default None - evaluated to 'connected' mode in production environment.
        If `True`, full Lets-Plot JS bundle will be added to the notebook. Use this option if you would like
        to work with notebook without the Internet connection.
        If `False`, load Lets-Plot JS library from CDN.

        show_status : bool
            Whether to show status of loading of the Lets-Plot JS library.
            Only applicable when the Lets-Plot JS library is preloaded.
    """
    embed = offline if offline is not None else get_global_bool('offline')
    ctx = _create_html_frontend_context(isolated_frame, embed)
    ctx.configure(verbose=show_status)
    _frontend_contexts[TEXT_HTML] = ctx


# def load_lets_plot_js(embed: bool = None):
#     """
#     Deprecated since v.1.3: instead use LetsPlot.setup_html()
#
#     Loads Lets-Plot javascript library into current frontend context.
#
#     Parameters
#     ----------
#     embed : bool, optional
#         True - embed JS which is bundled with Lets-Plot PyPI package. This is useful for off-line notebooks.
#         False - load JS from CDN.
#         default - load JS from CDN.
#     """
#     try:
#         from IPython.display import display_html
#         display_html("""\
#             <div style="color:darkred;">
#                 Method `load_lets_plot_js()` is deprecated since v.1.3 and will be removed soon.<br>
#                 Try to use `LetsPlot.setup_html()` instead.
#             </div>
#         """, raw=True)
#     except ImportError:
#         pass
#
#     _setup_html_context(None, embed, hide_status=False)


def _display_plot(plot_spec: Any):
    """
    Draw plot or `bunch` of plots in the current frontend context
    :param plot_spec: PlotSpec or GGBunch object
    """
    if not (isinstance(plot_spec, PlotSpec) or isinstance(plot_spec, GGBunch)):
        raise ValueError("PlotSpec or GGBunch expected but was: {}".format(type(plot_spec)))

    if _default_mimetype == TEXT_HTML:
        plot_html = _as_html(plot_spec.as_dict())
        try:
            from IPython.display import display_html
            display_html(plot_html, raw=True)
            return
        except ImportError:
            pass

        # ToDo: show HTML is brawser window
        return

    if _default_mimetype == LETS_PLOT_JSON:
        _frontend_contexts[LETS_PLOT_JSON].show(plot_spec.as_dict())
        return

        # fallback plain text
    print(plot_spec.as_dict())


def _as_html(plot_spec: Dict) -> str:
    """
    Creates plot HTML using 'html' frontend context.

    :param plot_spec: dict
    """
    if TEXT_HTML not in _frontend_contexts:
        if _use_isolated_frame():
            # 'Isolated' HTML context can be setup lazily.
            _setup_html_context(isolated_frame=True, offline=False, show_status=False)
        else:
            return """\
                <div style="color:darkred;">
                    Lets-plot `html` is not configured.<br> 
                    Try to use `LetsPlot.setup_html()` before first occurrence of plot.
                </div>    
                """

    return _frontend_contexts[TEXT_HTML].as_str(plot_spec)
