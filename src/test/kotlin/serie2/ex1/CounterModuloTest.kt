package serie2.ex1

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import serie2.Ex1.CounterModulo
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class CounterModuloTest(private val modulo: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Modulo: {0}")
        fun setup(): Collection<Array<Any>> {

            val maxModulo = 1000
            val params = mutableListOf<Array<Any>>()

            for (modulo in 1..maxModulo)
                params.add(arrayOf(modulo))

            return params
        }

    }

    @Test
    fun `single threaded increment and decrement counter`() {
        val counterModulo = CounterModulo(modulo)

        repeat(10000) {
            assertEquals((it + 1) % modulo, counterModulo.increment())
        }

        val decrementCounterModulo = CounterModulo(modulo)

        repeat(10000) {
            assertEquals((modulo - 1) - it % modulo, decrementCounterModulo.decrement())
        }

    }

    @Test
    fun `single threaded increment at module - 1 gives 0`() {
        val counterModulo = CounterModulo(modulo)

        repeat(modulo) {
            val value = counterModulo.increment()
            if (it == modulo - 1) {
                assertEquals(0, value)
            }

        }
    }

    @Test
    fun `single threaded decrement at 0 gives module - 1`() {
        val counterModulo = CounterModulo(modulo)
        val value = counterModulo.decrement()
        assertEquals(modulo - 1, value)
    }


    @Test
    fun `multi threaded increment counter`() {
        val pool = newCachedThreadPool()
        val counterModulo = CounterModulo(modulo)
        val count = AtomicInteger(0)
        var res = 0
        var adder = 1

        repeat(10000) {
            val r = Runnable {
                val value = counterModulo.increment()
                while (true) {
                    val obsCount = count.get()
                    if (count.compareAndSet(obsCount, obsCount + value)) break
                }
            }
            pool.execute(r)
        }

        pool.shutdown()
        pool.awaitTermination(1000000L, TimeUnit.SECONDS)

        repeat(10000) {
            if (adder == modulo) adder = 0
            res += adder
            adder++
        }

        assertEquals(res, count.get())
    }

    @Test
    fun `multi threaded decrement counter`() {
        val pool = newCachedThreadPool()
        val counterModulo = CounterModulo(modulo)
        val count = AtomicInteger(0)
        var res = 0
        var adder = modulo - 1

        repeat(10000) {
            val r = Runnable {
                val value = counterModulo.decrement()
                while (true) {
                    val obsCount = count.get()
                    if (count.compareAndSet(obsCount, obsCount + value)) break
                }
            }
            pool.execute(r)
        }

        pool.shutdown()
        pool.awaitTermination(1000000L, TimeUnit.SECONDS)

        repeat(10000) {
            if (adder == -1) adder = modulo - 1
            res += adder
            adder--
        }

        assertEquals(res, count.get())
    }


}