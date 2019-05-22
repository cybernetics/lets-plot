package jetbrains.datalore.visualization.plot.builder.data

import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.visualization.plot.base.DataFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun generateData(rowCount: Int, varNames: Collection<String>): DataFrame {
    val variables = varNames.map { DataFrame.Variable(it) }

    val builder = DataFrame.Builder()
    for (variable in variables) {
        builder.put(variable, toSerie(variable.name, indices(rowCount)))
    }

    return builder.build()
}

fun indices(count: Int): List<Int> {
    return (0 until count).toList()
}

fun toSerie(prefix: String, ints: Collection<Int>): List<*> {
    return ints.map { v -> prefix + v }
}

fun createCircle(pointsCount: Int, r: Double): List<DoubleVector> {
    @Suppress("NAME_SHADOWING")
    var pointsCount = pointsCount
    val circle = ArrayList<DoubleVector>()
    val step = 2 * PI / pointsCount++
    var angle = 0.0
    while (pointsCount-- > 0) {
        circle.add(DoubleVector(r * cos(angle), r * sin(angle)))
        angle += step
    }

    circle[circle.size - 1] = circle[0]

    return circle
}


