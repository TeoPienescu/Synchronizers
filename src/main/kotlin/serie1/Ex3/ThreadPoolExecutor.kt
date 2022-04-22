package serie1.Ex3


import utils.await
import utils.dueTime
import utils.isPast
import utils.isZero
import java.util.*
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration


/**
 * A thread pool executor implementation using kernel style.
 */
class ThreadPoolExecutor(
    private val maxThreadPoolSize: Int,
    private val keepAliveTime: Duration,
) {
    /**
     * All the possible states of an Executor
     */
    private enum class ExecutorState {
        WORKING, SHUTDOWN_STARTED, TERMINATED
    }

    /**
     * Represents a worker thread
     */
    private inner class Worker(val condition: Condition, var task: Runnable? = null) {
        var thread: Thread? = null

        /**
         * Starts a new thread to execute the worker code
         */
        fun start() {
            thread = thread {
                run(this)
            }
        }

        /**
         * Is used to run the task ignoring eventual exceptions thrown by the external code
         */
        fun safeTaskExecution() {
            try {
                task?.run()
                task = null
            } catch (e: Exception) {
                // here we can safely ignore the exception
            }
        }

        /**
         * Terminate worker thread due to shut down.
         * Is supposed to be called by the lock owner
         */
        fun terminate() {
            if (--size == 0 && state == ExecutorState.SHUTDOWN_STARTED) {
                state = ExecutorState.TERMINATED
                poolTerminated.signalAll()
            }
        }
    }

    private val monitor = ReentrantLock()
    private val poolTerminated = monitor.newCondition()

    /**
     * Shared state
     */
    private var size = 0
    private var state = ExecutorState.WORKING

    /**
     * Queue of pending workers
     * Queue of tasks to be executed
     */
    private val pendingWorkers = LinkedList<Worker>()
    private val pendingTasks = LinkedList<Runnable>()

    /**
     * Executes the given task in the thread pool
     */
    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable): Unit {
        monitor.withLock {
            /**
             * If the state is not WORKING, the task is rejected and is thrown RejectedExecutionException
             */
            if (state != ExecutorState.WORKING) throw RejectedExecutionException()

            /**
             * If there are pending available workers, it is removed and the runnable is given to one of them to execute.
             */
            if (pendingWorkers.isNotEmpty()) {
                val worker = pendingWorkers.removeFirst()
                worker.task = runnable
                worker.condition.signal()
            }
            /**
             * If the size of the thread pool is less than the maximum size, a new worker is created and given the task.
             * Else the task is added to the queue of pending tasks to be executed later.
             */
            else if (size < maxThreadPoolSize) {
                size++
                Worker(monitor.newCondition(), runnable).start()
            } else {
                pendingTasks.add(runnable)
            }
        }
    }

    /**
     * Runs the task in the given worker
     */
    private fun run(worker: Worker) {
        while (true) {
            /**
             * Executes the task
             */
            worker.safeTaskExecution()
            val dueTime = keepAliveTime.dueTime()
            monitor.withLock {
                /**
                 * Fast path.
                 * If there are pending tasks in the queue, the worker is given the first one.
                 */
                if (pendingTasks.isNotEmpty()) {
                    val item = pendingTasks.removeFirst()
                    worker.task = item
                } else {
                    /**
                     * The worker is added to the queue of pending workers, if the state of the executor is WORKING,
                     * because doesn't have another task to execute.
                     */
                    if (state == ExecutorState.WORKING) {
                        pendingWorkers.add(worker)
                    }

                    do {
                        /**
                         * If the state of the executor isn't WORKING, tries to terminate if it is possible and returns to exit the run loop.
                         */
                        if (state != ExecutorState.WORKING) {
                            worker.terminate()
                            return
                        }
                        /**
                         * It waits till is signalled or the due time is past.
                         * If the state is WORKING checks if the due time is past then if it is,
                         * the worker is removed from the queue of pending workers and exits the run loop.
                         * If it is not past it will execute the next task.
                         */
                        worker.condition.await(dueTime)

                        if (dueTime.isPast) {
                            size--
                            pendingWorkers.remove(worker)
                            return
                        }

                    } while (worker.task == null)
                }

            }
        }
    }

    /**
     * Notifies all the pending workers and clears the queue of pending workers.
     */
    private fun notifyWorkers() {
        for (worker in pendingWorkers) {
            worker.condition.signal()
        }
        pendingWorkers.clear()
    }

    /**
     * Puts the executor in the shutdown state, and notifies the workers.
     * If it is already in the shutdown state, it throws an exception.
     */
    fun shutdown() {
        monitor.withLock {
            if (state != ExecutorState.WORKING) throw IllegalStateException("The executor can't be already in shutdown mode")
            state = ExecutorState.SHUTDOWN_STARTED
            notifyWorkers()
        }
    }

    /**
     * Awaits the termination of the executor.
     */
    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        monitor.withLock {
            /**
             * Fast path
             * If the state is WORKING, it clears the queue of pending workers and throws an exception.
             * The state is set to TERMINATED to stop the run loops of the pending workers.
             */
            if (state == ExecutorState.WORKING) {
                pendingWorkers.clear()
                state = ExecutorState.TERMINATED
                throw IllegalStateException("To terminate, shutdown the executor first")
            }
            if (state == ExecutorState.TERMINATED) return true
            if (timeout.isZero) return false
            val dueTime = timeout.dueTime()
            do {
                poolTerminated.await(dueTime)
                if (state == ExecutorState.TERMINATED) return true
                if (dueTime.isPast) return false
            } while (true)
        }
    }
}



