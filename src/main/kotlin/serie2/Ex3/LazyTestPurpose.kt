package serie2.Ex3

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


/**
 * Class that implements a lazy value
 */
class LazyTestPurpose<T>(initializer: () -> T) {
    val init = initializer

    /**
     * Counts the number of times the value is initialized for test purposes
     */
    val countInits = AtomicInteger(0)

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
                    while (currentValue == null) Thread.yield()
                    return currentValue!!
                } else {
                    if (lazyState.compareAndSet(obsState, State.INITIALIZED)) {
                        currentValue = init()
                        //Added for test purposes
                        while (true) {
                            val obsCounter = countInits.get()
                            if (obsCounter == countInits.get()) {
                                if (countInits.compareAndSet(obsCounter, obsCounter + 1)) break
                            }
                        }
                        return currentValue!!
                    }
                }
            }
        }
}

