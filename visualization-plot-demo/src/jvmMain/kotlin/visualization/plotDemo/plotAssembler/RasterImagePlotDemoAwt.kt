package jetbrains.datalore.visualization.plotDemo.plotAssembler

import jetbrains.datalore.visualization.plotDemo.SwingDemoFrame
import jetbrains.datalore.visualization.plotDemo.model.plotAssembler.RasterImagePlotDemo

class RasterImagePlotDemoAwt : RasterImagePlotDemo() {

    private fun show() {
        val plots = createPlots()
        val svgRoots = createSvgRootsFromPlots(plots)
        SwingDemoFrame.showSvg(svgRoots, demoComponentSize, "Raster image plot")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            RasterImagePlotDemoAwt().show()
        }
    }
}