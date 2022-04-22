package serie1.Ex2

import utils.await
import utils.dueTime
import utils.isPast
import utils.isZero
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * A blocking message queue implementation using kernel style.
 */
class BlockingMessageQueue<T>(private val capacity: Int) {

    private val monitor = ReentrantLock()
    private val waitingMessages = monitor.newCondition()
    private val messageQueue: Queue<T> = LinkedList()
    private val pendingConsumers: Queue<Consumer<T>> = LinkedList()

    /**
     * Class that represents a consumer
     */
    class Consumer<T>(var message: T? = null, val condition: Condition){

        /**
         * Means that a message was already handed to it to be consumed
         */
        val canConsume: Boolean
            get() = message != null
    }

    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        /**
         * If there are more messages than the available capacity then throws an exception
         */
        if(messages.size > capacity) throw IllegalStateException("Number of messages to enqueue can't be bigger than the capacity")

        /**
         *  Checks if there are already pending consumers, then if there are consumers waiting it gives them the message directly without putting it in the queue.
         */
        monitor.withLock {
            var idx = 0
            if(messageQueue.isEmpty()){
                while(pendingConsumers.isNotEmpty()){
                    val consumer = pendingConsumers.poll()
                    consumer.message = messages[idx]
                    consumer.condition.signal()
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
            if(currentMessages.enqueue()) return true

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
                if(currentMessages.enqueue()) return true
                if(dueTime.isPast){
                    return false
                }
            }while (true)

        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        monitor.withLock {
            /**
             * Fast path.
             * The message queue is not empty. Therefore, there is no need to wait.
             * It takes a message from the queue and consumes it signaling the waiting messages to check for available space.
             */
            if(messageQueue.isNotEmpty()){
                waitingMessages.signalAll()
                return messageQueue.poll()
            }
            /**
             * If the timeout is zero, there is no wait. Therefore, it returns right away.
             */
            if (timeout.isZero) return null

            val dueTime = timeout.dueTime()

            /**
             * Creates a representation of the consumer and adds it to the list
             */
            val consumer: Consumer<T> = Consumer(condition = monitor.newCondition())
            pendingConsumers.add(consumer)
            do {
                consumer.condition.await(dueTime)
                val message = consumer.dequeue()
                if(message != null) return message
                if(dueTime.isPast){
                    return null
                }
            }while (true)
        }
    }

    /**
     * If it is possible to enqueue the messages, it puts them in the queue and notifies a possible consumer
     */
    private fun List<T>.enqueue() = if(canEnqueue()){
        forEach { message ->
            messageQueue.add(message)
        }
        notifyConsumer()
        true
    }else false

    /**
     * If it is possible to the consumer to consume the message, it means that a message has already been handed to him,
     * it consumes it and signals the waiting messages to enqueue to check if there is already available space for them to
     * enqueue. Else returns null
     */
    private fun Consumer<T>.dequeue() = if(canConsume){
        val message = message
        waitingMessages.signal()
        message
    }else null

    /**
     * Checks if there is available space to enqueue all the messages
     */
    private fun List<T>.canEnqueue() = this.size + messageQueue.size <= capacity

    /**
     * Notifies the consumer that there is already a message available for him to consume
     */
    private fun notifyConsumer(){
        while(pendingConsumers.isNotEmpty() && messageQueue.isNotEmpty()){
            val message = messageQueue.poll()
            val consumer = pendingConsumers.poll()
            consumer.message = message
            consumer.condition.signal()
        }
    }
}