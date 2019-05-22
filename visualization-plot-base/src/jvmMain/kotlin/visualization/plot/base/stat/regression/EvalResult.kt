package jetbrains.datalore.visualization.plot.gog.server.core.data.stat.regression

class EvalResult(val y: Double,    // predicted value
                 val ymin: Double, // lower pointwise confidence interval around the mean
                 val ymax: Double, // upper pointwise confidence interval around the mean
                 val se: Double    // standard error
)