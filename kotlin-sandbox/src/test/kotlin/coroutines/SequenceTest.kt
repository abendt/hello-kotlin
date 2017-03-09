package coroutines

import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Test
import kotlin.coroutines.experimental.buildSequence

class SequenceTest {

    @Test
    fun firstSequence() {
        val sequence = buildSequence {
            println("1")

            yield("hello")

            println("2")

            yield("world")

            println("3")
        }

        assertThat(sequence).containsExactly("hello", "world")
    }
}