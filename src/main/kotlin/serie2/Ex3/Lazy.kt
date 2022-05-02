package serie2.Ex3

import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class Lazy<T>(initializer: ()->T) {
    val init = initializer
    private var currentValue: AtomicReference<T?> = AtomicReference<T?>(null)

    val value: T
        get() {
            while (true){
                val obsValue = currentValue.get()
                if (obsValue != null) return obsValue
                val newValue = init()
                if(currentValue.compareAndSet(obsValue, newValue))
                    return newValue
            }
        }
}

fun main(){
    val x : Lazy<Int> = Lazy { 2 }
    val y  by lazy { 3 }
    thread {
        println(x.value)
    }

    thread {
        println(x.value)
    }

}