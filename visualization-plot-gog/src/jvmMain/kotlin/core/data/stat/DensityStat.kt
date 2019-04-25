package jetbrains.datalore.visualization.plot.gog.core.data.stat

import jetbrains.datalore.visualization.plot.gog.common.data.SeriesUtil
import jetbrains.datalore.visualization.plot.gog.core.data.DataFrame
import jetbrains.datalore.visualization.plot.gog.core.data.StatContext
import jetbrains.datalore.visualization.plot.gog.core.data.TransformVar
import jetbrains.datalore.visualization.plot.gog.core.data.stat.DensityStat.BandWidthMethod.NRD0
import jetbrains.datalore.visualization.plot.gog.core.render.Aes
import java.util.*
import java.util.function.Function

/**
 * Calculates the density function.
 * (or if the weight aesthetic is supplied, the sum of the weights, **not yet implemented**)
 */
class DensityStat : BaseStat(DEF_MAPPING) {
    private var myAdjust = DEF_ADJUST
    private var myN = DEF_N
    private var myBandWidthMethod = NRD0
    private var myBandWidth: Double? = null
    private var myKernel: Function<Double, Double> = DensityStatUtil.kernel(Kernel.GAUSSIAN)

    init {
    }

    fun setKernel(kernel: Kernel) {
        myKernel = DensityStatUtil.kernel(kernel)
    }

    fun setAdjust(adjust: Double) {
        myAdjust = adjust
    }

    fun setN(n: Int) {
        if (n > MAX_N) {
            throw IllegalArgumentException("The input n " + n + " > " + MAX_N + "is too large!")
        }
        myN = n
    }

    fun setBandWidthMethod(bw: BandWidthMethod) {
        myBandWidthMethod = bw
        myBandWidth = null
    }

    fun setBandWidth(bw: Double) {
        //myBW = BandWidth.DOUBLE;
        myBandWidth = bw
    }

    override fun apply(data: DataFrame, statCtx: StatContext): DataFrame {
        if (data.hasNoOrEmpty(TransformVar.X)) {
            return DataFrame.Builder.emptyFrame()
        }

        val valuesX = data.getNumeric(TransformVar.X)
        val statX = DensityStatUtil.createStepValues(statCtx.overallXRange(), myN)
        val statDensity = ArrayList<Double>()
        val statCount = ArrayList<Double>()
        val statScaled = ArrayList<Double>()

        // weight aesthetics
        val weight = StatUtil.weightVector(valuesX.size, data)

        val bandWidth: Double
        val densityFunction: Function<Double, Double>
        //bandWidth = myBandWidthMethod.equals(BandWidthMethod.DOUBLE) ? myBandWidth : DensityStatUtil.bandWidth(myBandWidthMethod, groupX);
        bandWidth = myBandWidth ?: DensityStatUtil.bandWidth(myBandWidthMethod, valuesX)
        densityFunction = DensityStatUtil.densityFunction(valuesX, myKernel, bandWidth, myAdjust, weight)

        for (x in statX) {
            val d = densityFunction.apply(x)
            statCount.add(d)
            statDensity.add(d / SeriesUtil.sum(weight)!!)
        }
        val maxm = Collections.max(statCount)
        for (d in statCount) {
            statScaled.add(d / maxm)
        }

        return DataFrame.Builder()
                .putNumeric(Stats.X, statX)
                .putNumeric(Stats.DENSITY, statDensity)
                .putNumeric(Stats.COUNT, statCount)
                .putNumeric(Stats.SCALED, statScaled)
                //.putNumericVar(Stats.GROUP, newGroups)
                .build()
    }

    override fun requires(): List<Aes<*>> {
        return listOf<Aes<*>>(Aes.X)
    }

    enum class Kernel {
        GAUSSIAN,
        RECTANGULAR,
        TRIANGULAR,
        BIWEIGHT,
        EPANECHNIKOV,
        OPTCOSINE,
        COSINE
    }

    enum class BandWidthMethod {
        NRD0,
        NRD
    }

    companion object {

        val DEF_KERNEL = "gaussian"
        val DEF_ADJUST = 1.0
        val DEF_N = 512
        val DEF_BW = "nrd0"
        private val DEF_MAPPING: Map<Aes<*>, DataFrame.Variable> = mapOf(
                Aes.X to Stats.X,
                Aes.Y to Stats.DENSITY
        )
        private val MAX_N = 9999
    }

    /*
  @Override
  public boolean handlesGroups() {
    return true;
  }
  */
}