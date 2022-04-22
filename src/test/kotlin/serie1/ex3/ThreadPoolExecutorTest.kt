package serie1.ex3

import org.junit.Test
import serie1.Ex3.ThreadPoolExecutor
import java.lang.Thread.sleep
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

class PoolTest {
   @Test
    fun `simple test with success with max pool size 1`() {
       val pool = ThreadPoolExecutor(1, INFINITE)
       val threads = mutableSetOf<Thread>()
       var res1 : Int?= null
       var res2 : Int? = null

       pool.execute{
           threads.add(Thread.currentThread())
           res1 = 1
       }

       pool.execute{
           threads.add(Thread.currentThread())
           res2 = 2
       }

       pool.shutdown()
       pool.awaitTermination(INFINITE)
       assertEquals(1, threads.size)
       assertEquals(1, res1)
       assertEquals(2, res2)
    }

    @Test
    fun `simple test with success with max pool size 4`() {
        val monitor = ReentrantLock()
        val pool = ThreadPoolExecutor(4, INFINITE)
        val threads = mutableSetOf<Thread>()
        var res1 : Int?= null
        var res2 : Int? = null
        var res3: Int? = null
        var res4: Int? = null
        var res5 : Int? = null
        var res6 : Int? = null

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res1 = 1
        }

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res2 = 2
        }

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res3 = 3
        }

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res4 = 4
        }

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res5 = 5
        }

        pool.execute{
            monitor.withLock {
                threads.add(Thread.currentThread())
            }
            res6 = 6
        }

        pool.shutdown()
        pool.awaitTermination(INFINITE)
        println(threads.size)
        assertTrue(threads.size <= 4)
        assertEquals(1, res1)
        assertEquals(2, res2)
        assertEquals(3, res3)
        assertEquals(4, res4)
        assertEquals(5, res5)
        assertEquals(6, res6)
    }

    @Test
    fun `Executor uses a pending worker`() {
        val pool = ThreadPoolExecutor(2, 8.seconds)

        var res1 : Int? = null
        var res2 : Int? = null
        var res3 : Int? = null
        pool.execute {
            res1 = 1
        }
        pool.execute {
            res2 = 2
        }

        sleep(50)
        pool.execute {
            Thread.sleep(1000)
            res3 = 3
        }

        pool.shutdown()
        val res = pool.awaitTermination(3.seconds)

        assertEquals(true,res)
        assertEquals(1,res1)
        assertEquals(2,res2)
        assertEquals(3,res3)
    }

    @Test
    fun `Executor throws RejectedExecutionException when trying to execute`() {
        assertFailsWith<RejectedExecutionException> {
            val pool = ThreadPoolExecutor(1, 3.seconds)
            val threads = mutableSetOf<Thread>()
            var res1 : Int?= null
            var res2 : Int? = null

            pool.execute{
                threads.add(Thread.currentThread())
                res1 = 1
            }
            pool.shutdown()
            pool.awaitTermination(INFINITE)


            pool.execute{
                threads.add(Thread.currentThread())
                res2 = 2
            }
        }
    }

    @Test
    fun `Executor throws IllegalState when trying to shutdown when is already shutting down`() {
        assertFailsWith<IllegalStateException> {
            val pool = ThreadPoolExecutor(1, 3.seconds)
            val threads = mutableSetOf<Thread>()
            var res1 : Int?= null
            var res2 : Int? = null

            pool.execute{
                threads.add(Thread.currentThread())
                res1 = 1
            }
            pool.shutdown()
            pool.shutdown()


            pool.execute{
                threads.add(Thread.currentThread())
                res2 = 2
            }
        }
    }

    @Test
    fun `Executor throws IllegalState when trying terminate without shutting down first`() {
        assertFailsWith<IllegalStateException> {
            val pool = ThreadPoolExecutor(1, 3.seconds)
            val threads = mutableSetOf<Thread>()
            var res1 : Int?= null
            var res2 : Int? = null

            pool.execute{
                threads.add(Thread.currentThread())
                res1 = 1
            }
            pool.awaitTermination(INFINITE)

            pool.execute{
                threads.add(Thread.currentThread())
                res2 = 2
            }
        }
    }

}