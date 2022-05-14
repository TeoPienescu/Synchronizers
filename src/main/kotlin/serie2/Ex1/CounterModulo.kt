package serie2.Ex1

import java.util.concurrent.atomic.AtomicInteger

/**
 *  Class that represents a counter that counts between 0 and the given module - 1.
 *  If the counter's value is module - 1 then the next increment is 0.
 *  If the counter's value is 0 then the next decrement is module-1
 */
class CounterModulo(moduloValue: Int) {
    private val value: Int = 0
    private val counter = AtomicInteger(value)
    private val modulo = moduloValue

    /**
     * Increments the counter's value
     */
    fun increment(): Int{
        do {
            val obsValue = counter.get()
            if(obsValue + 1 == modulo){                                       //If the counter´s value is module - 1, use CAS to set the value to 0
                if(counter.compareAndSet(obsValue, 0))
                    return 0
            }else{                                                            //Else use CAS to increment just 1 to the counter's value
                if(counter.compareAndSet(obsValue, obsValue + 1))
                    return obsValue + 1
            }
        }while (true)
    }

    /**
     * Decrements the counter's value
     */
    fun decrement(): Int{
        do {
            val obsValue = counter.get()
            if(obsValue == 0){                                                 //If the counter´s value is 0, use CAS to set the value to module - 1
                if(counter.compareAndSet(obsValue, modulo - 1))
                    return modulo - 1
            }else{
                if(counter.compareAndSet(obsValue, obsValue - 1))     //Else use CAS to decrement just 1 to the counter's value
                     return obsValue - 1
            }
        }while (true)
    }
}
