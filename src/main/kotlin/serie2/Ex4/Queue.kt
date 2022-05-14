package serie2.Ex4

import java.util.concurrent.atomic.AtomicReference

/**
 * Queue implementation using Michael-Scott algorithm
 */
class Queue<T> {

    private class Node<T>(val value: T? = null) {
        val next = AtomicReference<Node<T>?>()
    }

    private val head: AtomicReference<Node<T>>
    private val tail: AtomicReference<Node<T>>

    init {
        // initial queue state
        // a dummy(sentinel) node and head and tail refering it
        val dummy = Node<T>()
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    /**
     * Adds an element to the queue
     */
    fun put(elem: T) {
        val newNode = Node<T>(elem)
        do {
            val obsTail = tail.get()
            val obsTailNext = obsTail.next.get()
            if (obsTail == tail.get()) {    // just check that obsTail and obsTailNext are consistent
                if (obsTailNext == null) {  // the quiescent (stable) state
                    if (obsTail.next.compareAndSet(obsTailNext, newNode)) { // try do the insertion
                        tail.compareAndSet(obsTail, newNode) // and adjust tail
                        return
                    }
                } else {
                    tail.compareAndSet(obsTail, obsTailNext) // try adjust tail
                }
            }
        } while (true)
    }

    /**
     * Removes an element from the queue
     */
    fun take(): T? {
        do {
            val obsHead = head.get()
            val obsNext = obsHead.next.get()
            if(obsHead == head.get()){
                obsNext ?: return null
                if (head.compareAndSet(obsHead, obsNext)) { //Only removes if the head is the same as the observed head.
                    return obsNext.value
                }
            }
        } while (true)
    }

}