package jetbrains.datalore.visualization.plot.builder.data.sampling

import jetbrains.datalore.base.gcommon.base.Preconditions.checkState
import jetbrains.datalore.visualization.plot.base.DataFrame
import jetbrains.datalore.visualization.plot.base.data.Sampling

internal abstract class SamplingBase(val sampleSize: Int) : Sampling {

    init {
        checkState(this.sampleSize > 0, "Sample size must be greater than zero, but was: " + this.sampleSize)
    }

    open fun isApplicable(population: DataFrame): Boolean {
        return population.rowCount() > sampleSize
    }
}
