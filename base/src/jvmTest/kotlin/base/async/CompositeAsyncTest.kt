package jetbrains.datalore.base.async

import jetbrains.datalore.base.async.asyncAssert.AsyncMatchers
import jetbrains.datalore.base.async.asyncAssert.AsyncMatchers.failure
import jetbrains.datalore.base.async.asyncAssert.AsyncMatchers.unfinished
import org.hamcrest.CustomTypeSafeMatcher
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.sameInstance
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeAsyncTest {

    private var first: SimpleAsync<Int> = SimpleAsync()
    private var second: SimpleAsync<Int> = SimpleAsync()
    private var composite: Async<List<Int>> = PlatformAsyncs.composite(listOf(first, second))

    @Test
    fun successOneByOne() {
        first.success(0)
        assertThat(composite, unfinished<Int>())

        second.success(1)
        assertThat(composite, AsyncMatchers.succeeded())
    }

    @Test
    fun alreadySucceeded() {
        assertThat(PlatformAsyncs.composite(listOf(Asyncs.constant(0))),
                AsyncMatchers.succeeded())
    }

    @Test
    fun emptyRequest() {
        assertThat(PlatformAsyncs.composite(emptyList<Async<Int>>()),
                AsyncMatchers.result(Matchers.hasSize(0)))
    }

    @Test
    fun failWithSingleException() {
        first.success(0)
        assertThat(composite, unfinished<Int>())

        val failure = IllegalStateException("test")
        second.failure(failure)
        assertThat(composite, failure(sameInstance(failure)))
    }

    @Test
    fun failWithSeveralExceptions() {
        first.failure(RuntimeException("0"))
        second.failure(RuntimeException("1"))
        assertThat(composite, failure(
                object : CustomTypeSafeMatcher<ThrowableCollectionException>("collection of throwables") {
                    override fun matchesSafely(failure: ThrowableCollectionException): Boolean {
                        val throwables = failure.throwables
                        assertEquals(2, throwables.size)
                        val expected = listOf("0", "1")
                        val actual = listOf(throwables[0].message, throwables[1].message)
                        return expected == actual
                    }
                }
        ))
    }

    @Test
    fun resultListOrder() {
        second.success(1)
        first.success(0)
        val valueMatcher = Matchers.contains(0, 1) as Matcher<List<Int>>
        assertThat(composite, AsyncMatchers.result(valueMatcher))
    }
}
