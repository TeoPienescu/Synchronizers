package serie1.Ex1
import utils.await
import utils.dueTime
import utils.isPast
import java.lang.Thread.currentThread
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration as Duration

/**
 * Thread Exchanger using kernel style
 */
class Exchanger<T> {
    /**
     * Only one condition is needed since there will only be one thread at a time waiting
     */
    private val monitor = ReentrantLock()
    private val condition = monitor.newCondition()

    /**
     * Representation of the holder of the values to be exchanged
     */
    class Holder<T>(var valueT1: T, var valueT2: T? = null)
    private var holder: Holder<T>? =  null

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T? {
        monitor.withLock {
            /**
             * Fast path.
             * If the holder is not null it means that there is already a thread waiting for exchange.
             * It puts its value into the holder and gets the value of the other thread from the holder as well.
             * After signalling, it has to reset the holder to null.
             */
            if(holder != null){
                val valueT1 = holder!!.valueT1
                holder!!.valueT2 = value
                condition.signal()
                holder = null
                return valueT1
            }
            /**
             * Wait path.
             * It is the first thread that arrives. It has to wait for a second one to trade.
             * It updates the global holder with a Holder that contains its value.
             * Then creates a local copy of the global holder
             */
            holder = Holder(value)
            val localHolder = holder

            checkNotNull(localHolder) {"Holder was just initialized, local holder can't be null here"}

            val dueTime = timeout.dueTime()
            do {
                try {
                    /**
                     * The thread awaits to be signalled by the next one to exchange.
                     * If it is signalled, means that the two values are already available in the holder, it gets the value and returns it.
                     * Else if the due time is past just resets the holder to null and returns null
                     */
                    condition.await(dueTime)
                    if (localHolder.valueT2 != null) {
                        return localHolder.valueT2
                    }
                    if (dueTime.isPast) {
                        holder = null
                        return null
                    }
                }catch(e: InterruptedException){
                    /**
                     * In case of InterruptedException and there is already a value available it exchanges first then interrupts.
                     * Else it interrupts and throws the exception
                     */
                    if (localHolder.valueT2 != null) {
                        val valueT2 = localHolder.valueT2
                        currentThread().interrupt()
                        return valueT2
                    }
                    currentThread().interrupt()
                    throw e
                }
            }while (true)
        }

    }
}



