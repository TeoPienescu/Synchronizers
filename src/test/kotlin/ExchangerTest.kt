import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ExchangerTest {
    @Test
    fun `Exchange between two threads without timeout`() {
        val e = Exchanger<String>()
        var value1: String? = null
        var value2: String? = null
        val t1 = Thread {
            value1 = e.exchange("a", Duration.INFINITE)
        }
        val t2 = Thread {
            value2 = e.exchange("b", Duration.INFINITE)
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
        assertEquals("b", value1)
        assertEquals("a", value2)
    }

    @Test
    fun `Exchange between two threads with timeout`() {
        val e = Exchanger<Int>()
        var value1: Int? = null
        var value2: Int? = null
        val t1 = Thread {
            value1 = e.exchange(1, Duration.ZERO)
        }
        val t2 = Thread {
            value2 = e.exchange(2, 500.toDuration(DurationUnit.MILLISECONDS))
        }
        t1.start()
        t2.start()
        t1.join()
        t2.join()
        assertEquals(null , value1)
        assertEquals(null, value2)
    }

    @Test
    fun `Thread 1 times out but other thread gets in to trade with Thread 2 before Thread's 2 timeout`() {
        val e = Exchanger<String>()
        val t1 = Thread {
            assertEquals(null, e.exchange("Doesn't trade", 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        val t2 = Thread {
            Thread.sleep(2000)
            assertEquals("b", e.exchange("a", 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        val t3 = Thread {
            Thread.sleep(2000)
            assertEquals("a", e.exchange("b", 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        t1.start()
        t2.start()
        t3.start()
        t1.join()
        t2.join()
    }

    @Test
    fun `Thread 3 times out after Thread 1 and Thread 2 exchange values`() {
        val e = Exchanger<Int>()
        val t1 = Thread {
            assertEquals(2, e.exchange(1, 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        val t2 = Thread {
            assertEquals(1, e.exchange(2, 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        val t3 = Thread {
            Thread.sleep(3000)
            assertEquals(null, e.exchange(3, 1000.toDuration(DurationUnit.MILLISECONDS)))
        }
        t1.start()
        t2.start()
        t3.start()
        t1.join()
        t2.join()
    }

    @Test fun `Exchange with 4 threads at the same time`(){
        val exchanger = Exchanger<String>()
        val values = ConcurrentHashMap<String, String?>()

        val threads = listOf(
            Thread {
                values["t1"] = exchanger.exchange("t1", Duration.INFINITE)
            },
            Thread {
                values["t2"] = exchanger.exchange("t2", Duration.INFINITE)
            },
            Thread {
                values["t3"] = exchanger.exchange("t3", Duration.INFINITE)
            },
            Thread {
                values["t4"] = exchanger.exchange("t4", Duration.INFINITE)
            }
        )

        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        assertEquals(4, values.size)
        assertEquals(values[values["t1"]], "t1")
        assertEquals(values[values["t2"]], "t2")
        assertEquals(values[values["t3"]], "t3")
        assertEquals(values[values["t4"]], "t4")
    }

    @Test
    fun `Interrupting Thread1 while it's waiting gives interrupted exception`() {
        val exchanger = Exchanger<Int>()
        var e: Exception? = null
        val t1 = Thread {
            try {
                exchanger.exchange(1, Duration.INFINITE)
            } catch (ex: InterruptedException) {
                e = ex
            }
        }
        t1.start()
        t1.interrupt()
        t1.join()
        assertTrue(e is InterruptedException)
    }

    @Test
    fun `Interrupting Thread 1 while it is waiting for Thread 2 gives interrupted exception and gives null to Thread 2 after timeout`() {
        val exchanger = Exchanger<Int>()
        var e: Exception? = null
        var value2: Int? = 20 // just to make sure that value2 value at the end isn't null by mistake
        val t1 = Thread {
            try {
                exchanger.exchange(1, Duration.INFINITE)
            } catch (ex: InterruptedException) {
                e = ex
            }
        }
        val t2 = Thread {
            value2 = exchanger.exchange(2, 1000.toDuration(DurationUnit.MILLISECONDS))
        }
        t1.start()
        t1.interrupt()
        t2.start()
        t1.join()
        t2.join()
        assertTrue(e is InterruptedException)
        assertNull(value2)
    }
}