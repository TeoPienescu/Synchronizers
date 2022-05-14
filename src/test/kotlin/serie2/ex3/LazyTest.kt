package serie2.ex3

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import serie2.Ex3.Lazy
import serie2.Ex3.LazyTestPurpose
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLongArray
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LazyTest{
    @Test
    fun `Only initializes value once`() {
        val iterations = 100000
        val list = mutableListOf<Int>()
        val l: Lazy<Int> = Lazy {                    //Something that takes some time to execute -> around 70 ms
            for (i in 0..iterations) {
                list.add(i)
            }
            list.shuffle()
            list.sort()
            list.map { it + 1 }
            list[0]
        }

        val elapsedFirstTime = measureTimeMillis {
            l.value
        }

        val elapsedSecondTime = measureTimeMillis {
            l.value
        }

        val elapsedThirdTime = measureTimeMillis {
            l.value
        }

        println("First access to value: $elapsedFirstTime ms") //First thread initializes the value so it takes more time
        println("Second access to value: $elapsedSecondTime ms")
        println("Third access to value: $elapsedThirdTime ms")

        assertTrue(elapsedFirstTime > 50L)
        assertTrue(elapsedSecondTime < 2L)
        assertTrue(elapsedThirdTime < 2L)
    }


}

@RunWith(Parameterized::class)
class CounterModuloTest(private val iterations: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Iterations: {0}")
        fun setup(): Collection<Array<Any>> {

            val iterations = 10000
            val params = mutableListOf<Array<Any>>()

            for (modulo in 1..iterations)
                params.add(arrayOf(modulo))

            return params
        }

    }

    @Test
    fun `MultiThreaded only initializes value once`() {
        val pool = newCachedThreadPool()
        val l: LazyTestPurpose<Int> = LazyTestPurpose {
            2
        }
        val elapsedTimes = AtomicLongArray(iterations)

        repeat(iterations) {
            val r1 = Runnable {
                val elapsedTime = measureTimeMillis {
                    l.value
                }
                while (true) {
                    val obsListValue = elapsedTimes.get(it)
                    if (obsListValue == elapsedTimes[it])
                        if(elapsedTimes.compareAndSet(it, obsListValue, elapsedTime)){
                            break
                        }
                }
            }
            pool.execute(r1)
        }

        pool.shutdown()
        pool.awaitTermination(1000L, TimeUnit.SECONDS)
        assertEquals(1, l.countInits.get())

    }
}
