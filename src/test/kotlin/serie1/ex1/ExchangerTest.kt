import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import serie1.Ex1.Exchanger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds




class ExchangerTest{
    @Test
    fun `Exchange between two threads and the third times out`() {
        val exchanger = Exchanger<Int>()
        var res1: Int? = null
        var res2: Int? = null
        var res3: Int? = null
        val thread1 = Thread {
            res1 = exchanger.exchange(1, INFINITE)
        }

        val thread2 = Thread {
            res2 = exchanger.exchange(2, INFINITE)
        }

        val thread3 = Thread {
            res3 = exchanger.exchange(2, 3.seconds)
        }

        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()
        thread3.start()
        thread3.join()

        assertEquals(2, res1)
        assertEquals(1, res2)
        assertEquals(null, res3)
    }

    @Test
    fun `First thread times out and then next two exchange`() {
        val exchanger = Exchanger<Int>()
        var res1: Int? = null
        var res2: Int? = null
        var res3: Int? = null
        val thread1 = Thread {
            res1 = exchanger.exchange(1, 2.seconds)
        }

        Thread.sleep(3000)
        val thread2 = Thread {
            res2 = exchanger.exchange(2, INFINITE)
        }

        val thread3 = Thread {
            res3 = exchanger.exchange(3, INFINITE)
        }

        thread1.start()
        thread1.join()
        thread2.start()
        thread3.start()
        thread2.join()
        thread3.join()


        assertEquals(null, res1)
        assertEquals(3, res2)
        assertEquals(2, res3)
    }

    @Test
    fun `First thread times out then all get null`() {
        val exchanger = Exchanger<Int>()
        var res1: Int? = null
        var res2: Int? = null
        val thread1 = Thread {
            res1 = exchanger.exchange(1, 1.seconds)
        }
        val thread2 = Thread {
            res2 = exchanger.exchange(2, 1.seconds)
        }
        thread1.start()
        thread1.join()
        thread2.start()
        Thread.sleep(2000)
        thread2.join()
        assertNull(res1)
        assertNull(res2)
    }

    @Test
    fun `Interruption throws InterruptedException`() {
        val exchanger = Exchanger<Int>()
        var exception: Exception? = null
        val thread1 = Thread {
            try {
                exchanger.exchange(1, INFINITE)
            } catch (ex: InterruptedException) {
                exception = ex
            }
        }
        thread1.start()
        thread1.interrupt()
        thread1.join()
        assertTrue(exception is InterruptedException)
    }
}


@RunWith(Parameterized::class)
class ExchangerParameterizedTest(private val nExchanges: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Exchanges: {0}")
        fun setup(): Collection<Array<Any>> {

            val params = mutableListOf<Array<Any>>()

            for (exchangers in 2..240)
                params.add(arrayOf(exchangers))

            return params
        }

    }

    /**
     * It is executed for each value of the parameter nExchanges.
     * Tries to make n number of exchanges between two threads
     */

    @Test
    fun `Exchanger parameterized test`() {

        val listOfValues = (1..nExchanges * 2).toList()
        val t1Values = listOfValues.subList(0, nExchanges)
        val t2Values = listOfValues.subList(nExchanges, nExchanges * 2)

        val exchanger = Exchanger<Int>()
        val monitor = ReentrantLock()
        val exchangers = mutableListOf<Thread>()
        val resultMap = ConcurrentHashMap<Int, Int?>()

        for (i in 0 until nExchanges) {
            val thread1 = thread {
                val input = listOfValues[i]
                val message = exchanger.exchange(input, 2.seconds)
                monitor.withLock {
                    resultMap[input] = message
                }
            }

            val thread2 = thread {
                val input = listOfValues[i + nExchanges]
                val message = exchanger.exchange(input, 2.seconds)

                monitor.withLock {
                    resultMap[input] = message
                }
            }

            exchangers.add(thread1)
            exchangers.add(thread2)

        }

        exchangers.forEach { it.join(3000) }
        println(resultMap)
        t1Values.forEach { sent ->
            assertEquals(sent, resultMap[resultMap[sent]])
            val received = resultMap[sent]
            assertNotNull(received)
        }


        t2Values.forEach { sent ->
            assertEquals(sent, resultMap[resultMap[sent]])
            val received = resultMap[sent]
            assertNotNull(received)
        }
    }

}

