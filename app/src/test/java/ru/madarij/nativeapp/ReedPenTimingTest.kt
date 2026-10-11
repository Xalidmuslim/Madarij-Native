package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    @Test fun handwritingHonorsSpacesAndPunctuation() {
        val line = ReedPenCharacterTimeline("А Б.")
        assertEquals(0f, line.positionAt(0f), 0.00001f)
        assertEquals(4f, line.positionAt(1f), 0.00001f)
        assertTrue(line.positionAt(.32f) > 1f)
        assertTrue(line.positionAt(.32f) < 2.5f)
        assertEquals(0f, ReedPenCharacterTimeline("").positionAt(.5f), 0f)
    }
    @Test fun penProgressNeverGoesBackwards() {
        val line = ReedPenCharacterTimeline("Понимание — это свет, который в сердце.")
        var previous = 0f
        for (n in 0..1000) {
            val value = line.positionAt(n / 1000f)
            assertTrue(value >= previous)
            previous = value
        }
    }
}
