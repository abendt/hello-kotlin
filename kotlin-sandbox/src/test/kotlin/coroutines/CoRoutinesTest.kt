package coroutines

import org.junit.Test
import kotlinx.coroutines.experimental.*
import kotlin.concurrent.thread

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
        val jobs = List(1000) {
            thread {
                Thread.sleep(1000L)
                print(".")
            }
        }
        jobs.forEach { it.join() }
    }
}
