package serie2.ex2

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import serie2.Ex2.MessageBox
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageBoxSimpleTest {
    @Test
    fun `A new publish overrides the older one`() {
        val messageBox = MessageBox<Int>()
        val oldMessageValue = 10
        val newMessageValue = 20
        var value1: Int? = null
        var value2: Int? = null
        val lives = 2
        messageBox.publish(oldMessageValue, lives)
        messageBox.publish(newMessageValue, lives)
        value1 = messageBox.tryConsume()
        value2 = messageBox.tryConsume()

        assertEquals(newMessageValue, value1)
        assertEquals(newMessageValue, value2)
    }

    @Test
    fun `A new publish in the middle of the process of consuming`() {
        val messageBox = MessageBox<Int>()
        val oldMessageValue = 10
        val newMessageValue = 20
        var value1: Int? = null
        var value2: Int? = null
        var value3: Int? = null
        var value4: Int? = null
        val lives = 3
        val lives2 = 1
        messageBox.publish(oldMessageValue, lives)
        value1 = messageBox.tryConsume()
        value2 = messageBox.tryConsume()
        messageBox.publish(newMessageValue, lives2)
        value3 = messageBox.tryConsume()
        value4 = messageBox.tryConsume()
        assertEquals(oldMessageValue, value1)
        assertEquals(oldMessageValue, value2)
        assertEquals(newMessageValue, value3)
        assertEquals(null, value4)
    }
}


@RunWith(Parameterized::class)
class MessageBoxTest(private val lives: Int) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "Lives: {0}")
        fun setup(): Collection<Array<Any>> {

            val maxLives = 1000
            val params = mutableListOf<Array<Any>>()

            for (modulo in 1..maxLives)
                params.add(arrayOf(modulo))

            return params
        }
    }


    @Test
    fun `MultiThreaded consumers trying to consume 1 message with N lives`() {
        val messageBox = MessageBox<Int>()
        val iterations = 10000
        val messageValue = 10
        val pool = Executors.newCachedThreadPool()
        val messages = AtomicReferenceArray<Int?>(10000)
        messageBox.publish(messageValue, lives)
        repeat(iterations) {
            val r = Runnable {
                while(true) {
                    val obsListValue = messages.get(it)
                    if (obsListValue == messages[it])
                        if(messages.compareAndSet(it, obsListValue, messageBox.tryConsume())) break
                }
            }
            pool.execute(r)
        }
        pool.shutdown()
        pool.awaitTermination(1000L, TimeUnit.SECONDS)

        var notNullMessagesCount = 0
        val notNullMessagesList = mutableListOf<Int>()
        for (i in 0 until iterations) {
            val message = messages.get(i)
            if (message != null) {
                notNullMessagesCount++
                notNullMessagesList.add(message)
            }
        }

        val messagesAreValid = notNullMessagesList.all { it == messageValue }

        assertEquals(lives, notNullMessagesCount)
        assertTrue(messagesAreValid)
    }

}