package coroutines

import org.junit.Test
import kotlinx.coroutines.experimental.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class CoRoutinesTest {

    @Test
    fun myFirstLaunch() {
        launch(CommonPool) {
            println(Thread.currentThread().name)
            delay(1000)
            println("World")
        }

        println("Hello")

        Thread.sleep(2000)
    }

    @Test
    fun canRunBlocking() {
        runBlocking {
            println(Thread.currentThread().name)
            launch(CommonPool) {
                delay(1000)
                println("World")
            }

            println("Hello")

            delay(2000)
        }
    }


    @Test
    fun canWaitForJob() {
        runBlocking {
            val job = launch(CommonPool) {
                delay(1000)
                println("World")
            }

            println("Hello")

            job.join()
        }
    }

    @Test
    fun refactoredVersion() {
        runBlocking {
            val job = launch(CommonPool) {
                doWorld()
            }

            println("Hello")

            job.join()
        }
    }


    private suspend fun doWorld() {
        delay(1000)
        println("World")
    }

    @Test
    fun coRoutinesAreLightWeight() {
        runBlocking {
            val jobs = List(100_000) {
                // create a lot of coroutines and list their jobs
                launch(CommonPool) {
                    delay(1000L)
                    print(".")
                }
            }
            jobs.forEach { it.join() } // wait for all jobs to complete
        }
    }

    @Test
    fun threadsAreHeavyWeight() {
        val jobs = List(100000) {
            thread {
                Thread.sleep(1000L)
                print(".")
            }
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun coRoutinesAreLikeDaemonThreads() {
        println(Thread.currentThread().name)
        runBlocking {
            println(Thread.currentThread().name)
            launch(CommonPool) {
                repeat(1000) {
                    println("about to sleep $it")
                    delay(500)
                }
            }

            delay(1000)
        }
    }

    @Test
    fun canCancelCoRoutine() {
        runBlocking {
            val job = launch(CommonPool) {
                repeat(1000) {
                    println("wait $it")
                    delay(500)
                }
            }

            delay(2000)

            println("cancel job")

            job.cancel()

            delay(2000)

            println("done")
        }
    }

    @Test
    fun cancelIsCooperative() {
        runBlocking {
            val job = launch(CommonPool) {
                var nextPrintTime = 0L

                while (true) {
                    val now = System.currentTimeMillis()

                    if (now > nextPrintTime) {
                        println("X")
                        nextPrintTime = now + 500
                    }
                }
            }

            delay(1000)

            println("cancel job")

            job.cancel()

            delay(2000)

            println("done")
        }
    }

    @Test
    fun canCancelCooperativeJob() {
        runBlocking {
            val job = launch(CommonPool) {
                var nextPrintTime = 0L

                try {
                    while (isActive) {
                        val now = System.currentTimeMillis()

                        if (now > nextPrintTime) {
                            println("X")
                            nextPrintTime = now + 500
                        }
                    }
                } finally {
                    println("finally")
                }
            }

            delay(1000)

            println("cancel job")

            job.cancel()

            delay(2000)

            println("done")
        }
    }

    @Test
    fun cannotRunSuspendingFunctionWhenCancelled() {
        runBlocking {
            val job = launch(CommonPool) {
                try {
                    while (isActive) {
                        println("X")
                        delay(500)
                    }
                } finally {
                    delay(100)
                    println("will never be run")
                }
            }

            delay(1000)

            job.cancel()

            delay(2000)
        }
    }

    @Test
    fun canRunSuspendingFunctionAfterCancellation() {
        runBlocking {
            val job = launch(CommonPool) {
                try {
                    while (isActive) {
                        println("X")
                        delay(500)
                    }
                } finally {
                    run(NonCancellable) {
                        delay(100)
                        println("will be run")
                    }
                }
            }

            delay(1000)

            job.cancel()

            delay(2000)
        }
    }


    @Test
    fun canUseTimeout() {
        runBlocking {
            withTimeout(1000) {
                repeat(1000) {
                    println("X")
                    delay(500)
                }
            }
        }
    }

    suspend fun doSomethingUseful(): Int {
        delay(1000)
        return 10
    }

    @Test
    fun runJobsSequentially() = runBlocking {
        val time = measureTimeMillis {
            val one = doSomethingUseful()
            val two = doSomethingUseful()

            println("answer is ${one + two}")
        }

        println("Took $time")
    }

    @Test
    fun runJobsAsync() = runBlocking {
        val time = measureTimeMillis {
            val one = async(CommonPool) { doSomethingUseful() }
            val two = async(CommonPool) { doSomethingUseful() }

            println("answer is ${one.await() + two.await()}")
        }

        println("Took $time")
    }

    @Test
    fun runJobsAsyncLazy() = runBlocking {
        val time = measureTimeMillis {
            val one = async(CommonPool, start = false) { doSomethingUseful() }
            val two = async(CommonPool, start = false) { doSomethingUseful() }

            println("answer is ${one.await() + two.await()}")
        }

        println("Took $time")
    }

    fun asyncDoSomethingUseful() =
            async(CommonPool) {
                doSomethingUseful()
            }

    @Test
    fun runAsyncStyleFunction() {
        val time = measureTimeMillis {

            val one = asyncDoSomethingUseful()
            val two = asyncDoSomethingUseful()

            runBlocking {
                println("answer is ${one.await() + two.await()}")
            }
        }

        println("Took $time")
    }

    @Test
    fun canRunInContext() = runBlocking {
        val jobs = arrayListOf<Job>()
        jobs += launch(Unconfined) {
            // not confined -- will work with main thread
            println(" 'Unconfined': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(context) {
            // context of the parent, runBlocking coroutine
            println("    'context': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(CommonPool) {
            // will get dispatched to ForkJoinPool.commonPool (or equivalent)
            println(" 'CommonPool': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("     'newSTC': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun confinedVsUnconfined() = runBlocking {
        val jobs = arrayListOf<Job>()
        jobs += launch(Unconfined) {
            // not confined -- will work with main thread
            println(" 'Unconfined': I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println(" 'Unconfined': After delay in thread ${Thread.currentThread().name}")
        }
        jobs += launch(context) {
            // context of the parent, runBlocking coroutine
            println("    'context': I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("    'context': After delay in thread ${Thread.currentThread().name}")
        }
        jobs.forEach { it.join() }
    }

    @Test
    fun canJumpBetweenThreads() {
        val ctx1 = newSingleThreadContext("Ctx1")
        val ctx2 = newSingleThreadContext("Ctx2")
        runBlocking(ctx1) {
            log("Started in ctx1")
            run(ctx2) {
                log("Working in ctx2")
            }
            log("Back to ctx1")
        }
    }

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

}
