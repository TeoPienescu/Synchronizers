package serie2.Ex2

import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class UnsafeMessageBox<M> {
    private class Holder<M>(val msg: M, initialLives: Int) {
        var lives: Int = initialLives
    }

    private var holder: AtomicReference<Holder<M>?> = AtomicReference(null)

    fun publish(msg: M, lives: Int) {
        holder = AtomicReference(Holder(msg, lives))
    }

    fun tryConsume(): M? {
        do {
            val obsValue = holder.get()
            if (obsValue != null && obsValue.lives > 0) {
                if (holder.compareAndSet(obsValue, Holder(obsValue.msg, obsValue.lives - 1)))
                    return obsValue.msg
            } else {
                return null
            }
        } while (true)
    }

}


class SafeMessageBox<M> {
    private class Holder<M>(val msg: M, initialLives: Int) {
        var lives: Int = initialLives
    }
    private var holder: Holder<M>? = null
    fun publish(msg: M, lives: Int) {
        holder = Holder(msg, lives)
    }
    fun tryConsume(): M? =
        if (holder != null && holder!!.lives > 0) {
            holder!!.lives -= 1
            holder!!.msg
        } else {
            null
        }
}

fun main() {
    val x = UnsafeMessageBox<Int>()

    println(x.publish(3, 4))

    val t1 = Thread{
        println(x.tryConsume())
    }
    val t2 = Thread{
        println(x.tryConsume())
    }
    val t3 = Thread{
        println(x.tryConsume())
    }
    val t4 = Thread{
        println(x.tryConsume())
    }
    val t5 = Thread{
        println(x.tryConsume())
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