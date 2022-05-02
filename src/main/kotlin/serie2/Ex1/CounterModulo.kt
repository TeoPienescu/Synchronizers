package serie2.Ex1

import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

val logger = Logger.getLogger("logger")
class CounterModulo(moduloValue: Int) {
    private val value: Int = 0
    val counter = AtomicInteger(value)
    val modulo = moduloValue


    fun increment(): Int{
        do {
            val obsValue = counter.get()
            logger.info("Observed Value $obsValue")
            if(obsValue + 1 == modulo){
                if(counter.compareAndSet(obsValue, 0)) return 0
                logger.info("Counter incremented to 0")
            }else{
                if(counter.compareAndSet(obsValue, obsValue + 1)){
                    logger.info("Counter incremented to $counter")
                    return obsValue + 1
                }
            }
        }while (true)
    }

    fun decrement(): Int{
        do {
            val obsValue = counter.get()
            logger.info("Observed Value $obsValue")
            if(obsValue - 1 == 0){
                if(counter.compareAndSet(obsValue, modulo)) return modulo
                logger.info("Counter decremented to ${modulo}")
            }else{
                if(counter.compareAndSet(obsValue, obsValue - 1)){
                    logger.info("Counter decremented to $counter")
                    return obsValue - 1
                }
            }
        }while (true)
    }
}

fun main(){
    val counter = CounterModulo(5)
    println("Modulo: ${counter.modulo}")
    println("Start ${counter.counter.get()}")
    /*println(counter.increment())
    println(counter.increment())
    println(counter.increment())
    println(counter.increment())//0
    println(counter.increment())*/
    println(counter.increment())
    println(counter.increment())
    println(counter.decrement())
    println(counter.decrement())
    println(counter.decrement())// 5
    println(counter.decrement())
    println(counter.decrement())// 5
    println(counter.decrement())
    println(counter.decrement())// 5
    println(counter.decrement())
}