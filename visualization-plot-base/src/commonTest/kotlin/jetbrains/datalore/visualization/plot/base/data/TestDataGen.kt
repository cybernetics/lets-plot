package jetbrains.datalore.visualization.plot.base.data

import jetbrains.datalore.visualization.plot.base.DataFrame

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
