package serie2.ex4

import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import serie2.Ex4.Queue
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.test.assertTrue

class QueueSimpleTest {
    @Test
    fun `queue single take on a previous inserted value`() {
        // build the needed artifacts
        val queue = Queue<String>()
        val expectedResult = "Ok"
        var result: String? = null

        queue.put(expectedResult)

        result = queue.take()

        TestCase.assertEquals(expectedResult, result)
    }

    @Test
    fun `single take on a empty queue`() {
        // build the needed artifacts
        val queue = Queue<String>()
        var result: String? = "a"

        result = queue.take()

        TestCase.assertEquals(null, result)
    }
}

@RunWith(Parameterized::class)
class QueueTest(private val iterations: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Iterations {0}")
        fun setup(): Collection<Array<Any>> {

            val params = mutableListOf<Array<Any>>()
            val maxIterations = 10000
            // Varying the number of readers, writers and capacity
            for (iteration in 1..maxIterations) {
                params.add(arrayOf(iteration))
            }

            return params
        }

    }

    @Test
    fun `Multithreaded queue operations`() {
        val putPool = newCachedThreadPool()
        val takePool = newCachedThreadPool()
        val queue = Queue<Int>()
        repeat(iterations){
            val r = Runnable {
                queue.put(it)
            }
            putPool.execute(r)
        }

        putPool.shutdown()
        putPool.awaitTermination(1000L, TimeUnit.SECONDS)

        val values = AtomicReferenceArray<Int?>(iterations)

        repeat(iterations){
            val r = Runnable {
                val value = queue.take()
                while(true) {
                    val obsListValue = values.get(it)
                    if (obsListValue == values[it])
                        if(values.compareAndSet(it, obsListValue, value)) break
                }
            }
            takePool.execute(r)
        }

        takePool.shutdown()
        takePool.awaitTermination(1000L, TimeUnit.SECONDS)

        var okResult = true
        for(i in 0 until iterations){
            if(values.get(i) !in 0..iterations)
                okResult = false
        }

        assertTrue(okResult)
    }


}