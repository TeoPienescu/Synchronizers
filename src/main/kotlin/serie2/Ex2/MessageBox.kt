package serie2.Ex2

import java.util.concurrent.atomic.AtomicReference

/**
 * Class that represent a message box.
 * All the consumers try to consume the published message.
 * The published message can be changed by the producer.
 */
class MessageBox<M> {
    /**
     * Class that represents the holder that contains the published message and the lives
     */
    private class Holder<M>(val msg: M, initialLives: Int) {
        var lives: Int = initialLives
    }

    /**
     * Holder that contains the published message and the number of lives.
     */
    private val holder: AtomicReference<Holder<M>?> = AtomicReference(null)

    /**
     * Publish a message.
     */
    fun publish(msg: M, lives: Int) {
        holder.set(Holder(msg, lives))
    }

    /**
     * Consume the published message.
     */
    fun tryConsume(): M? {
        do {
            val obsValue = holder.get()
            if (obsValue != null && obsValue.lives > 0) {
                //If the holder is the same as the observed one, then the message is consumed and a life is removed
                if (holder.compareAndSet(obsValue, Holder(obsValue.msg, obsValue.lives - 1)))
                    return obsValue.msg
            } else {
                return null
            }
        } while (true)
    }
}


