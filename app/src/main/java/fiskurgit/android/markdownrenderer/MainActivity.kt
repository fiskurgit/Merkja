package fiskurgit.android.markdownrenderer

import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException

import android.util.Log
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var simpleMDRenderer: SimpleMDRenderer

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val assetInputStream = assets.open("demo.md")
        val assetAsString = assetInputStream.bufferedReader().use { it.readText() }

        markdown_text_view.text = assetAsString

        simpleMDRenderer = SimpleMDRenderer(markdown_text_view) { matchEvent ->

            when (matchEvent.schemeType){
                SimpleMDRenderer.SCHEME_IMAGE -> loadImage(matchEvent)
                SimpleMDRenderer.SCHEME_LINK -> handleLink(matchEvent)
            }

        }
        simpleMDRenderer.render()
    }

    private fun handleLink(matchEvent: SimpleMDRenderer.MatchEvent){
        Log.d("MDL:::", "handleLink: ${matchEvent.value}")
    }

    private fun loadImage(matchEvent: SimpleMDRenderer.MatchEvent){
        Log.d("MDL:::", "loadImage: ${matchEvent.value}")
        val request = Request.Builder()
            .url(matchEvent.value)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val inputStream = response.body()?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                runOnUiThread {
                    simpleMDRenderer.insertImage(bitmap, matchEvent)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(this@MainActivity::class.java.simpleName, e.toString())
            }
        })
    }
}
