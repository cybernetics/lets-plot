package jetbrains.datalore.visualization.plot.gog.core.scale

import jetbrains.datalore.base.assertion.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object ScaleTestUtil {
    fun assertExpandValuesPreservedInCopy(scale: Scale2<*>) {
        var scale = scale
        scale = scale.with()
                .multiplicativeExpand(0.777)
                .additiveExpand(777.0)
                .build()

        val copy = scale.with().build()
        assertEquals(scale.multiplicativeExpand, copy.multiplicativeExpand, 0.0)
        assertEquals(scale.additiveExpand, copy.additiveExpand, 0.0)
    }

    fun assertValuesInLimits(scale: Scale2<*>, vararg domainValues: Any) {
        for (v in domainValues) {
            assertTrue(scale.isInDomainLimits(v), "Not in limits: $v")
        }
    }

    fun assertValuesNotInLimits(scale: Scale2<*>, vararg values: Any) {
        for (v in values) {
            assertFalse(scale.isInDomainLimits(v), "In limits: $v")
        }
    }
}
