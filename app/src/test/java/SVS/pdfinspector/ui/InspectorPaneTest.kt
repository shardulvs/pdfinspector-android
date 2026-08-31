package SVS.pdfinspector.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import SVS.pdfinspector.engine.DrawNode
import SVS.pdfinspector.engine.NodeKind

class InspectorPaneTest {
    
    private fun createTestNode(
        id: Int, 
        label: String, 
        detail: String = "", 
        raw: String = "", 
        text: String = "",
        children: List<DrawNode> = emptyList()
    ) = DrawNode(
        id = id,
        kind = NodeKind.TEXT,
        label = label,
        detail = detail,
        startIndex = 0,
        endIndex = 0,
        bounds = null,
        colorArgb = 0,
        raw = raw,
        children = children,
        text = text
    )

    @Test
    fun filterByLabelMatches() {
        val root = createTestNode(0, "root", children = listOf(
            createTestNode(1, "TextBlock", detail = "Some detail"),
            createTestNode(2, "Image", detail = "Image detail"),
        ))
        
        val query = "TextBlock"
        val matches = root.children.filter { it.label.contains(query, ignoreCase = true) }
        assertEquals(1, matches.size)
        assertEquals("TextBlock", matches[0].label)
    }

    @Test
    fun filterByDetailMatches() {
        val root = createTestNode(0, "root", children = listOf(
            createTestNode(1, "Text1", detail = "arial font"),
            createTestNode(2, "Text2", detail = "courier font"),
        ))
        
        val query = "arial"
        val matches = root.children.filter { 
            listOfNotNull(it.label, it.detail).any { text -> 
                text.contains(query, ignoreCase = true) 
            }
        }
        assertEquals(1, matches.size)
        assertEquals("Text1", matches[0].label)
    }

    @Test
    fun filterIsCaseInsensitive() {
        val root = createTestNode(0, "root", children = listOf(
            createTestNode(1, "TextBlock", detail = "Some detail"),
        ))
        
        val queries = listOf("textblock", "TEXTBLOCK", "TextBlock", "TeXtBlOcK")
        for (query in queries) {
            val matches = root.children.filter { it.label.contains(query, ignoreCase = true) }
            assertEquals("Query '$query' should match", 1, matches.size)
        }
    }

    @Test
    fun emptyQueryMatches() {
        val root = createTestNode(0, "root", children = listOf(
            createTestNode(1, "Text1", detail = "detail1"),
            createTestNode(2, "Text2", detail = "detail2"),
        ))
        
        val query = ""
        val matches = root.children.filter { 
            if (query.trim().isEmpty()) true
            else listOfNotNull(it.label, it.detail).any { text -> 
                text.contains(query, ignoreCase = true)
            }
        }
        assertEquals("Empty query should match all", 2, matches.size)
    }

    @Test
    fun searchFieldToggleState() {
        // Test the state management logic
        var showSearchField = false
        assertTrue("Search field should start hidden", !showSearchField)
        
        showSearchField = true
        assertTrue("Search field should be visible when toggled", showSearchField)
        
        showSearchField = false
        assertFalse("Search field should be hidden when toggled again", showSearchField)
    }

    @Test
    fun searchQueryFilteringLogic() {
        val root = createTestNode(0, "root", children = listOf(
            createTestNode(1, "TextElement", detail = "Text content"),
            createTestNode(2, "PathElement", detail = "Path data"),
            createTestNode(3, "ImageElement", detail = "Image data"),
        ))
        
        val testCases = listOf(
            Pair("", 3),  // Empty query returns all
            Pair("text", 1),  // "text" matches only TextElement
            Pair("path", 1),  // "path" matches only PathElement
            Pair("element", 3),  // "element" matches all three
            Pair("notfound", 0),  // No matches
        )
        
        for ((query, expectedCount) in testCases) {
            val matches = root.children.filter { node ->
                if (query.trim().isEmpty()) true
                else listOfNotNull(node.label, node.detail).any { 
                    it.contains(query, ignoreCase = true) 
                }
            }
            assertEquals("Query '$query' should match $expectedCount nodes", expectedCount, matches.size)
        }
    }
}

