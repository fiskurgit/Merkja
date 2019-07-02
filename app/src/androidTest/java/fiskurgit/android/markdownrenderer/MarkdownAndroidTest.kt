package fiskurgit.android.markdownrenderer

import android.content.Context
import android.graphics.Bitmap
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Ignore

@RunWith(AndroidJUnit4::class)
class MarkdownAndroidTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var renderer: Merkja

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("fiskurgit.android.markdownrenderer", appContext.packageName)
    }

    //todo - figure this out
    @Test @Ignore
    fun imageParseAndRemove(){
        val md = "a: ![Image title](http://website.com/image.png)\n"

        val dummyView = TextView(context)
        dummyView.text = md
        renderer = Merkja(dummyView){ matchEvent ->

            val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
            renderer.insertImage(bitmap, matchEvent)
            Assert.assertEquals("a: \n", dummyView.text.toString())
        }
        renderer.render()
    }

    @Test
    fun inlineCodeParseAndRemove(){
        val md = "a: `Markdown Renderer`\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("a: Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun emphasisParseAndRemove(){
        val md = "a: _Markdown Renderer_\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("a: Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun boldParseAndRemove(){
        val md = "a: **Markdown Renderer**\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("a: Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h6ParseAndRemove(){
        val md = "###### Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h5ParseAndRemove(){
        val md = "##### Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h4ParseAndRemove(){
        val md = "#### Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h3ParseAndRemove(){
        val md = "### Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h2ParseAndRemove(){
        val md = "## Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }

    @Test
    fun h1ParseAndRemove(){
        val md = "# Markdown Renderer\n"

        val dummyView = TextView(context)
        dummyView.text = md
        val renderer = Merkja(dummyView)
        renderer.render()

        Assert.assertEquals("Markdown Renderer\n", dummyView.text.toString())
    }
}
