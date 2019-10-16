package jetbrains.datalore.plotDemo.plotContainer

import jetbrains.datalore.plot.builder.presentation.Style.JFX_PLOT_STYLESHEET
import jetbrains.datalore.plotDemo.model.plotContainer.BarPlotResizeDemo
import jetbrains.datalore.vis.demoUtils.jfx.SceneMapperDemoFactory

object BarPlotResizeContinuousXDemoSceneMapper {
    @JvmStatic
    fun main(args: Array<String>) {
        PlotResizeDemoUtil.show(BarPlotResizeDemo.continuousX(), SceneMapperDemoFactory(JFX_PLOT_STYLESHEET))
    }
}