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
import android.content.Intent
import android.net.Uri

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var merkja: Merkja

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val assetInputStream = assets.open("demo.md")
        val assetAsString = assetInputStream.bufferedReader().use { it.readText() }

        markdown_text_view.text = assetAsString

        merkja = Merkja(markdown_text_view) { matchEvent ->

            when (matchEvent.schemeType){
                Merkja.SCHEME_IMAGE -> loadImage(matchEvent)
                Merkja.SCHEME_LINK -> handleLink(matchEvent)
            }

        }
        merkja.render()
    }

    private fun handleLink(matchEvent: Merkja.MatchEvent){
        val clickedLink = matchEvent.value

        if(clickedLink.startsWith("https://")){
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(clickedLink)
            startActivity(intent)
        }else{
            val assetInputStream = assets.open(clickedLink)
            val assetAsString = assetInputStream.bufferedReader().use { it.readText() }
            markdown_text_view.text = assetAsString
            merkja.render()
        }
    }

    private fun loadImage(matchEvent: Merkja.MatchEvent){
        val request = Request.Builder()
            .url(matchEvent.value)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                val inputStream = response.body()?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                runOnUiThread {
                    merkja.insertImage(bitmap, matchEvent)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d(this@MainActivity::class.java.simpleName, e.toString())
            }
        })
    }
}
