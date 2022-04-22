package serie1.Ex4

import utils.await
import utils.dueTime
import utils.isPast
import utils.isZero
import java.util.logging.Logger
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Blocking queue with try dequeue method returning a future using kernel style
 */
class BlockingMessageQueueFuture<T>(private val capacity: Int) {
    override fun toString(): String {
        return messageQueue.toString()
    }

    private val monitor = ReentrantLock()
    private val waitingMessages = monitor.newCondition()
    private val messageQueue: Queue<T> = LinkedList()
    private val pendingFutures: Queue<MyFuture<T>> = LinkedList()


    @Throws(InterruptedException::class)


    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        /**
         * If there are more messages than the available capacity then throws an exception
         */
        if (messages.size > capacity) throw IllegalStateException("Number of messages to enqueue can't be bigger than the capacity")
        monitor.withLock {

            /**
             *  Checks if there are already pending consumers, then if there are consumers waiting it gives them the message directly without putting it in the queue.
             */
            var idx = 0
            if (messageQueue.isEmpty()) {
                while (pendingFutures.isNotEmpty() && idx < messages.size) {
                    val future = pendingFutures.poll()
                    println(future)
                    future.trySetState(messages[idx], null)
                    idx++
                }
            }

            /**
             * Drops the messages that have already been given to the consumers
             */
            val currentMessages = messages.drop(idx)


            /**
             * Fast path.
             * Enqueues the messages and notifies a consumer that there are messages available to consume
             */
            if (currentMessages.enqueue()) return true

            /**
             * If the timeout is zero, there is no wait. Therefore, it returns right away.
             */
            if (timeout.isZero) return false


            val dueTime = timeout.dueTime()
            /**
             * Awaits till it is signaled or the due time is past.
             */
            do {
                waitingMessages.await(dueTime)
                if (currentMessages.enqueue()) return true
                if (dueTime.isPast) {
                    return false
                }
            } while (true)

        }
    }

    /**
     * If it is possible to enqueue the messages, it puts them in the queue and inserts the future with the value in the pending futures queue for each message.
     */
    private fun List<T>.enqueue() = if (canEnqueue()) {
        forEach { message ->
            messageQueue.add(message)
        }
        if (pendingFutures.isNotEmpty()) pendingFutures.poll().trySetState(messageQueue.poll(), null)
        true
    } else false

    /**
     * Dequeues a message returning a representation of the result right away
     */
    @Throws(InterruptedException::class)
    fun tryDequeue(): MyFuture<T> {
        val future: MyFuture<T> = MyFuture()
        monitor.withLock {
            /**
             * if the message queue is not empty returns a representation with the value of the message.
             */
            if (messageQueue.isNotEmpty()) {
                future.trySetState(messageQueue.poll(), null)
                waitingMessages.signal()
                return future
            }
            /**
             * else it adds the future to the pending futures queue to later get a message from it.
             * It returns the future representation.
             */
            pendingFutures.add(future)
            return future
        }

    }

    /**
     * Checks if there is available space to enqueue all the messages
     */
    private fun List<T>.canEnqueue() = this.size + messageQueue.size <= capacity

    /**
     * All the possible Future states
     */
    enum class FutureState { NEW, DONE, CANCELLED }

    /**
     * A Future implementation
     */
    inner class MyFuture<T>() : Future<T> {
        private val monitor = ReentrantLock()
        private val hasValue = monitor.newCondition()
        private var value: T? = null
        private var state = FutureState.NEW
        private var exception: Exception? = null

        /**
         * Signals all pending futures and clears the queue, sets the state to CANCELLED
         */
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            monitor.withLock {
                val cancelStateSuccess = trySetState(null, CancellationException())
                pendingFutures.forEach { future ->
                    future.hasValue.signal()
                }
                pendingFutures.clear()
                return cancelStateSuccess
            }
        }

        fun trySetState(result: T?, error: Exception?): Boolean {
            monitor.withLock {
                if (result != null) {
                    state = FutureState.DONE
                    value = result
                    hasValue.signal()
                } else {
                    state = FutureState.CANCELLED
                    exception = error
                }
                return true
            }

        }

        override fun isCancelled(): Boolean {
            monitor.withLock {
                return state == FutureState.CANCELLED
            }
        }

        override fun isDone(): Boolean {
            monitor.withLock {
                return state == FutureState.DONE
            }
        }

        override fun get(): T {
            return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        }

        @Throws(InterruptedException::class)
        override fun get(timeout: Long, unit: TimeUnit): T {
            monitor.withLock {
                if (state == FutureState.DONE) {
                    val returnValue = value
                    requireNotNull(returnValue) { "Value can't be null when the state is done" }
                    return returnValue
                }
                if (state == FutureState.CANCELLED) {
                    val exceptionToBeThrown = exception
                    requireNotNull(exceptionToBeThrown) { "Exception can't be null when the state is cancelled" }
                    throw exceptionToBeThrown
                }
                if (timeout == 0L) throw TimeoutException()
                val dueTime = unit.convert(timeout, unit).toDuration(DurationUnit.MILLISECONDS).dueTime()

                do {
                    try {
                        hasValue.await(dueTime)
                        if (state == FutureState.DONE) {
                            val returnValue = value
                            requireNotNull(returnValue) { "Value can't be null when the state is done" }
                            return returnValue
                        }
                        if (state == FutureState.CANCELLED) {
                            /**
                             * Only throws the exception after the queue is empty
                             */
                            while (true) {
                                if (pendingFutures.isEmpty()) throw CancellationException()
                            }
                        }
                        if (dueTime.isPast) throw TimeoutException()
                    } catch (e: InterruptedException) {
                        if (state == FutureState.DONE) {
                            val returnValue = value
                            requireNotNull(returnValue) { "Value can't be null when the state is done" }
                            return returnValue
                        } else {
                            throw e
                        }
                    }
                } while (true)
            }
        }
    }
}