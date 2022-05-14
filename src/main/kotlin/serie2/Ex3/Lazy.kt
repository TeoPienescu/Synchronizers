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
            while (true) {
                val obsState = lazyState.get()
                if (obsState == State.INITIALIZED) {
                    while (currentValue == null) Thread.yield() // Waits for value to be produced giving the resources to other threads
                    return currentValue!!
                } else {
                    if (lazyState.compareAndSet(obsState, State.INITIALIZED)) {
                        currentValue = init()
                        return currentValue!!// Initializes the value
                    }
                }
            }
        }
}


//The while loop is needed to prevent that after 2 or more threads enter the else block, the next threads after the one that started initializing return null.
/**
return if (obsState == State.INITIALIZED) {
    while (currentValue == null) Thread.yield()
    currentValue!!
} else { -> can return null if 2 threads see the state Uninitialized at the beginning.
    if (lazyState.compareAndSet(obsState, State.INITIALIZED)) {
        currentValue = init()                   -> only one enters to initialize the value
    }
    currentValue!!   -> the first returns the value and the second returns null

    giving the error: Exception in thread "pool-x-thread-x" java.lang.NullPointerException

}*/