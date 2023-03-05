# Concurrent Programming

In this repository you will find some implementations of synchronizers and thread safe utility components made while studying "Concurrent Programming", namely:
  ### Lock based (serie1)
  - [**Exchanger**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie1/Ex1/Exchanger.kt): Exchanges values between two threads.

  - **Blocking Message Queue**: A blocking queue that can be used to exchange messages between threads.
    - [**Classic Aproach**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie1/Ex2/BlockingMessageQueue.kt): Both enqueue and dequeue methods are blocking.
    - [**Future Implementation**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie1/Ex4/BlockingMessageQueueFuture.kt): Dequeue method returns a [future](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie1/Ex4/BlockingMessageQueueFuture.kt#L135), thus is non blocking.

  - [**Thread Pool Executor**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie1/Ex3/ThreadPoolExecutor.kt): 
  A thread pool that can be used to execute tasks concurrently, without the need to manage thread creation and destruction.
    

### Lock free (serie2)
  - [**Counter Modulo**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie2/Ex1/CounterModulo.kt): 
  A counter that can be incremented and decremented, but only up to a given value.
  
  - [**Message Box**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie2/Ex2/MessageBox.kt): 
  Stores a message that can be read an determined number of times.
  
  - [**Lazy**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie2/Ex3/Lazy.kt): 
  A lazy value that is initialized only once, and then cached.

  - [**Queue**](https://github.com/TeoPienescu/Synchronizers/blob/main/src/main/kotlin/serie2/Ex4/Queue.kt): 
  A thread safe classic queue. Implemented using the [Lock free algorithm of Michael and Scott.](https://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf)


## Tests:
  Every synchronizer and component was properly tested using both unit and parameterized tests.
  The unit tests also serve as examples of how to use them properly.
  
  - [Lock based](https://github.com/TeoPienescu/Synchronizers/tree/main/src/test/kotlin/serie1)
  - [Lock free](https://github.com/TeoPienescu/Synchronizers/tree/main/src/test/kotlin/serie2)
  
