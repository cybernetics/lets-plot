package jetbrains.datalore.visualization.plot.base.data

import jetbrains.datalore.visualization.plot.base.DataFrame
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataFrameAssert internal constructor(private val myData: DataFrame) {

    internal fun hasRowCount(expected: Int): DataFrameAssert {
        assertEquals(expected, myData.rowCount(), "Row count")
        return this
    }

    internal fun hasSerie(varName: String, serie: List<*>): DataFrameAssert {
        assertTrue(DataFrameUtil.hasVariable(myData, varName), "Var '$varName'")
        val variable = DataFrameUtil.findVariableOrFail(myData, varName)
        val list = myData[variable]
        assertEquals(list, serie)
        return this
    }

    companion object {
        fun assertHasVars(df: DataFrame, vars: Iterable<DataFrame.Variable>, dataSize: Int = -1) {
            for (variable in vars) {
                assertTrue(df.has(variable), "Has var '${variable.name}'")
                if (dataSize >= 0) {
                    assertEquals(dataSize, df[variable].size, "Data size '${variable.name}'")
                }
            }
        }
    }
}
