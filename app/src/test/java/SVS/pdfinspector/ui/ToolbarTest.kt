package SVS.pdfinspector.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolbarTest {
    @Test
    fun pageLabelFormatsCurrentPosition() {
        assertEquals("1 / 12", pageLabel(0, 12))
        assertEquals("7 / 12", pageLabel(6, 12))
        assertEquals("0 / 0", pageLabel(0, 0))
    }

    @Test
    fun pageJumpTargetAcceptsOnlyValidPageNumbers() {
        assertEquals(0, pageJumpTarget("1", 12))
        assertEquals(6, pageJumpTarget("7", 12))
        assertEquals(11, pageJumpTarget("12", 12))
        assertNull(pageJumpTarget("0", 12))
        assertNull(pageJumpTarget("13", 12))
        assertNull(pageJumpTarget("-1", 12))
        assertNull(pageJumpTarget("abc", 12))
        assertNull(pageJumpTarget("1", 0))
    }

    @Test
    fun pageJumpErrorMessagesAreDescriptive() {
        assertEquals("Page number is required", getPageJumpError("", 12))
        assertEquals("Page number must be greater than 0", getPageJumpError("0", 12))
        assertEquals("Page number must be between 1 and 12", getPageJumpError("13", 12))
        assertEquals("Page number must be between 1 and 12", getPageJumpError("100", 12))
        assertEquals("No pages available", getPageJumpError("1", 0))
    }
}
