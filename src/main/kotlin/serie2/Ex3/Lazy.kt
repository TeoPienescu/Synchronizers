package serie2.Ex3

import java.util.concurrent.atomic.AtomicReference

/**
 * Class that implements a lazy value
 */
class Lazy<T>(initializer: () -> T) {
    /**
     * Value initializer
     */
    val init = initializer

    /**
     * Possible states of the lazy value
     */
    enum class State {
        UNINITIALIZED,
        INITIALIZED
    }

    var lazyState: AtomicReference<State> = AtomicReference(State.UNINITIALIZED)

    @Volatile
    private var currentValue: T? = null

    val value: T
        get() {
            val obsState = lazyState.get()
            return if (obsState == State.UNINITIALIZED && lazyState.compareAndSet(obsState, State.INITIALIZED)) {
                currentValue = init() // Initializes the value
                currentValue!!
            } else {
                while (currentValue == null) Thread.yield() // Waits for value to be produced giving the resources to other threads
                currentValue!!
            }
        }
}

