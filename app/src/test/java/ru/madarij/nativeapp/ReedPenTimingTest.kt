package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ReedPenTimingTest {
    @Test fun unhurriedAndBoundedDuration() {
        assertEquals(5500, reedPenDurationMillis(40))
        assertEquals(7540, reedPenDurationMillis(130))
        assertEquals(12000, reedPenDurationMillis(250))
    }
    @Test fun longerTextCannotWriteFaster() {
        val sizes = (0..320 step 7).toList()
        sizes.zipWithNext().forEach { (a, b) ->
            assert(reedPenDurationMillis(b) >= reedPenDurationMillis(a))
        }
    }
}
