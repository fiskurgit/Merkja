package fiskurgit.android.markdownrenderer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class MarkdownAndroidTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("fiskurgit.android.markdownrenderer", appContext.packageName)
    }

    @Test
    fun imageParseAndRemove(){
        val md = "a: ![Image title](http://website.com/image.png)\n"

        val parsed = SimpleMDRenderer(drawableCallback = {renderer, block, image, location ->
            Assert.assertEquals("http://website.com/image.png", image)
            Assert.assertEquals(3, location)
        }).process(md)

        Assert.assertEquals("a: \n", parsed.toString())
    }

    @Test
    fun inlineCodeParseAndRemove(){
        val md = "a: `Markdown Renderer`\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("a: Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun emphasisParseAndRemove(){
        val md = "a: _Markdown Renderer_\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("a: Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun boldParseAndRemove(){
        val md = "a: **Markdown Renderer**\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("a: Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h6ParseAndRemove(){
        val md = "###### Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h5ParseAndRemove(){
        val md = "##### Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h4ParseAndRemove(){
        val md = "#### Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h3ParseAndRemove(){
        val md = "### Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h2ParseAndRemove(){
        val md = "## Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }

    @Test
    fun h1ParseAndRemove(){
        val md = "# Markdown Renderer\n"

        val parsed = SimpleMDRenderer().process(md)

        Assert.assertEquals("Markdown Renderer\n", parsed.toString())
    }
}
