package jetbrains.datalore.visualization.plot.base.render.geom

import jetbrains.datalore.base.geometry.DoubleRectangle
import jetbrains.datalore.visualization.plot.base.event.MappedDataAccess
import jetbrains.datalore.visualization.plot.base.event3.MouseEventSource
import jetbrains.datalore.visualization.plot.base.render.*
import jetbrains.datalore.visualization.plot.base.render.geom.LivemapProvider.LivemapData
import jetbrains.datalore.visualization.plot.base.render.geom.util.GenericLegendKeyElementFactory


class LivemapGeom(private val myDisplayMode: DisplayMode) : Geom {
    private var myMapProvider: LivemapProvider? = null

    override val legendKeyElementFactory: LegendKeyElementFactory
        get() {
            when (myDisplayMode) {
                DisplayMode.POINT -> return PointLegendKeyElementFactory()
                DisplayMode.PIE -> return FilledCircleLegendKeyElementFactory()
                else -> return GenericLegendKeyElementFactory()
            }
        }

    override fun build(root: SvgRoot, aesthetics: Aesthetics, pos: PositionAdjustment, coord: CoordinateSystem,
                       ctx: GeomContext) {
        throw IllegalStateException("Not applicable to live map")
    }

    fun setLivemapProvider(livemapProvider: LivemapProvider) {
        myMapProvider = livemapProvider
    }

    fun createCanvasFigure(
            aesthetics: Aesthetics,
            dataAccess: MappedDataAccess,
            bounds: DoubleRectangle,
            eventSource: MouseEventSource,
            layers: List<LivemapLayerData>
    ): LivemapData {
        return myMapProvider!!.createLivemap(aesthetics, dataAccess, bounds, eventSource, layers)
    }

    enum class DisplayMode {
        POLYGON,
        POINT,
        PIE,
        HEATMAP,
        BAR
    }

    enum class Theme {
        COLOR,
        LIGHT,
        DARK
    }

    enum class Projection {
        EPSG3857,
        EPSG4326,
        AZIMUTHAL,
        CONIC
    }

    companion object {

        // ToDo: not static, depends on 'display mode'
        val RENDERS = listOf(
                Aes.MAP_ID,
                Aes.ALPHA,
                Aes.COLOR,
                Aes.FILL,
                Aes.SIZE,
                Aes.SHAPE,
                Aes.FRAME,
                Aes.X,
                Aes.Y
        )
        val HANDLES_GROUPS = false
    }
}