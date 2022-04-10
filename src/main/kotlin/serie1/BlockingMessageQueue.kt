package isel.leic.pc.utils.serie1

import isel.leic.pc.utils.await
import isel.leic.pc.utils.dueTime
import isel.leic.pc.utils.isPast
import isel.leic.pc.utils.isZero
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BlockingMessageQueue<T>(private val capacity: Int) {
    private val monitor = ReentrantLock()
    val messageQueue: Queue<T> = LinkedList<T>()
    private val pendingListMessages: Queue<PendingListMessages> = LinkedList<PendingListMessages>()
    private val pendingConsumers: Queue<Condition> = LinkedList<Condition>()

    inner class MessageListWrapper<T>(val messages: List<T>) {
        val canEnqueue
            get() = messages.size + messageQueue.size <= capacity
    }

    class PendingListMessages(var units: Int, val condition: Condition) {
        val spaceIsAvailable : Boolean
            get() = units == 0
    }

    @Throws(InterruptedException::class)
    fun tryEnqueue(messages: List<T>, timeout: Duration): Boolean {
        monitor.withLock {

            val messagesToEnqueue = MessageListWrapper(messages)

            //fast path
            if (messagesToEnqueue.canEnqueue) {
                enqueue(messagesToEnqueue)
                println(messageQueue)
                return true
            }

            if (timeout.isZero) {
                println(messageQueue)
                return false
            }

            val dueTime = timeout.dueTime()
            val waiter = PendingListMessages(messages.size, monitor.newCondition())
            pendingListMessages.add(waiter)

            do {
                waiter.condition.await(dueTime)
                if (messagesToEnqueue.canEnqueue) {
                    enqueue(messagesToEnqueue)
                    println(messageQueue)
                    return true
                }
                if (dueTime.isPast){println(messageQueue) ;return false}
            } while (true)
        }

    }



    @Throws(InterruptedException::class)
    fun tryDequeue(timeout: Duration): T? {
        monitor.withLock {
            if(messageQueue.isNotEmpty()){
                val messages = pendingListMessages.peek()
                dequeue(messages)
                println(messageQueue)
                return messageQueue.poll()
            }

            if (timeout.isZero) {
                return null
            }

            val dueTime = timeout.dueTime()
            do {
                val pendingConsumer = monitor.newCondition()
                pendingConsumers.add(pendingConsumer)
                pendingConsumer.await(dueTime)
                if (messageQueue.isNotEmpty()) {
                    val messages = pendingListMessages.peek()
                    dequeue(messages)
                    println(messageQueue)
                    return messageQueue.poll()
                }
                if (dueTime.isPast) return null
            } while (true)
        }
    }

    private fun enqueue(messagesWrapper: MessageListWrapper<T>) {
        messagesWrapper.messages.forEach { msg -> messageQueue.offer(msg) }
        if(pendingConsumers.isNotEmpty()) pendingConsumers.poll().signal()
    }

    private fun dequeue(messages: PendingListMessages?){
        if(messages != null) {
            messages.units--
            if (messages.spaceIsAvailable) messages.condition.signal()
            pendingListMessages.remove(messages)
        }
    }
}


fun main() {
    val block = BlockingMessageQueue<String>(4)
    val list = listOf("a", "b", "c", "d")
    val list2 = listOf("e")

    val t1 = Thread {
        val x = block.tryEnqueue(list, 2.toDuration(DurationUnit.SECONDS))
        println("1: $x")
    }

    val t2 = Thread {
        val y = block.tryEnqueue(list2, 2.toDuration(DurationUnit.SECONDS))
        println("2: $y")
    }

    val t3 = Thread {
        val z = block.tryEnqueue(list2, 2.toDuration(DurationUnit.SECONDS))
        println("3: $z")
    }

    val t4 = Thread {
        val z = block.tryDequeue(2.toDuration(DurationUnit.SECONDS))
        println("4: $z")
    }

    val t5 = Thread {
        val z = block.tryDequeue(2.toDuration(DurationUnit.SECONDS))
        println("5: $z")
    }

    t1.start()
    t2.start()
    t3.start()
    t4.start()
    t5.start()
    t1.join()
    t2.join()
    t3.join()
    t4.join()
    t5.join()
}