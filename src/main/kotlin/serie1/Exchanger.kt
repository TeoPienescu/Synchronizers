package serie1

import isel.leic.pc.utils.*
import java.lang.Thread.currentThread
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.Queue
import java.util.concurrent.locks.Condition
import kotlin.concurrent.withLock
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.Duration as Duration


/**
 * A simple implementation of a thread-safe exchange between two values.
 * In the process of the exchange there are two threads: the waiter and the exchanging.
 * The waiter is the thread that arrives and doesn't have any other thread to exchange with, so it waits.
 * The exchanging is the thread that arrives, exchanges with the waiter and then signals the waiter to exchange with it.
 * For this purpose, two queues are used to store the values to wait and exchange in the order they arrive.
 */
class Exchanger<T> {
    private val monitor = ReentrantLock()
    private var waiterThread: WaiterThread<T>? = null
    private val exchangingThreadQueue: Queue<ExchangingThread<T>> = LinkedList<ExchangingThread<T>>()
    /**
     * Value wrappers for the waiting and exchanging thread.
     * The [WaiterThread] value can be null to signal that it already gave its value to the other thread.
     */
    class WaiterThread<T>(var value: T?, val condition: Condition){
        val valueIsNull: Boolean
            get() = value == null
    }
    class ExchangingThread<T>(val value: T)

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T? {
        monitor.withLock {

            /**
             * Fast Path. If the waiting threads values queue is not empty, then the exchange will occur right away.
             * It removes the first waiter thread value from the queue and calls a function to exchange the values
             * and notify the waiting thread to proceed with the exchange as well.
             */
            if(waiterThread != null){
                val waiter = waiterThread
                return waiter?.let { exchangeAndNotifyWaiter(it,value) }
            }

            /**
             * The waiting thread values queue is empty. Therefore, it is a thread that needs to wait, so if the timeout
             * is zero, returns null.
             */
            if(timeout.isZero) return null

            /**
             * Creates a [WaiterThread] and adds it to the waiter thread values queue.
             */
            val waiter = WaiterThread<T>(value, monitor.newCondition())
            waiterThread = waiter

            val dueTime = timeout.dueTime()

            /**
             * Awaits for the condition to be signaled. If it isn't signaled within the timeout, returns null
             * If a [InterruptedException] is thrown, it is caught, the waiter is interrupted and removed from the queue and then rethrows the error.
             * The remove operation is safe because the [WaiterThread] wraps its own value. Therefore, since it timed out,
             * and it will not exchange anymore the value can be discarded.
             */

            do{
                try {
                    waiter.condition.await(dueTime)
                    if (waiter.valueIsNull) {
                        return exchangingThreadQueue.poll().value
                    }
                    if (dueTime.isPast) {
                        waiterThread = null
                        return null
                    }
                }catch (e: InterruptedException){
                    currentThread().interrupt()
                    waiterThread = null
                    throw e
                }
            }while(true)
        }
    }

    /**
     * The exchanging thread calls this function to exchange the values and notify the waiting thread to proceed with the exchange.
     * The value from this thread it is stored in the [ExchangingThread] queue so the waiter can get its value to exchange.
     * In the meantime, the waiting thread value is set to null to signal that it already gave its value to the other thread.
     */
    private fun exchangeAndNotifyWaiter(waiter: WaiterThread<T>, exchangingValue:T): T? {
            val exchanger = ExchangingThread(exchangingValue)
            exchangingThreadQueue.add(exchanger)

            val waiterValue = waiter.value
            waiter.value = null
            waiter.condition.signal()
            waiterThread = null
            return waiterValue
    }
}
