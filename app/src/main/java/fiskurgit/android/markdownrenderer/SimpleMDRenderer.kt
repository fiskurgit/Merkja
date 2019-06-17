package fiskurgit.android.markdownrenderer

import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Spanned
import java.util.regex.Pattern
import android.text.SpannableStringBuilder
import android.text.style.*
import android.content.res.Resources
import android.graphics.*
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView

/*

    This is a single file Markdown renderer.
    It's very basic, but is a simple drop-in class to use in a project so you don't need an external library dependency.
    If you need more features and a full API use Markwon instead: https://github.com/noties/Markwon

    Supported syntax:

    # Big headers down to
    ###### small headers
    **Bold**
    _italics_
    `inline code`
    ![An image](imageUrl or resource name)

    Image resources from the .apk will load automatically, for remote images, or Uris you'll need to fetch them yourself.
    examples:

    ![An image](ic_app_icon)
    ![An image](https://website.com/image.png)

    For remote images supply an external handler, fetch asynchronously then call insertImage() with the bitmap.

    Links also need to be handled by an external handler

 */
class SimpleMDRenderer(private val textView: TextView, var externalHandler: (matchEvent: MatchEvent) -> Unit = { _ -> }) {

    private var start = 0
    private var end = 0

    companion object {
        private const val RANGE_DEFAULT = 0
        private const val RANGE_INNER = 1

        private const val DEFAULT_MODE = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        private const val SCHEME_H1 = 1
        private const val SCHEME_H2 = 2
        private const val SCHEME_H3 = 3
        private const val SCHEME_H4 = 4
        private const val SCHEME_H5 = 5
        private const val SCHEME_H6 = 6
        private const val SCHEME_CODE_INLINE = 7
        private const val SCHEME_EMPHASES = 8
        private const val SCHEME_BOLD = 9
        const val SCHEME_IMAGE = 10
        const val SCHEME_LINK = 11

        fun resizeImage(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val scale = Resources.getSystem().displayMetrics.widthPixels.toFloat() / width
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
            bitmap.recycle()
            return resizedBitmap
        }
    }

    private var black = Color.parseColor("#000000")
    private var codeBackground = Color.parseColor("#DEDEDE")
    private var link = Color.parseColor("#ff00cc")

    data class MatchEvent(
        val schemeType: Int,
        val matchText: String,
        val value: String,
        val start: Int = -1,
        val end: Int = -1
    )

    data class MDScheme(
        val id: Int,
        val pattern: Pattern,
        val foreground: Int? = null,
        val background: Int? = null,
        val textStyle: Int? = Typeface.NORMAL,
        val scale: Float? = null,
        val range: Int = RANGE_DEFAULT
    )

    private val h1Pattern = Pattern.compile("^#\\s.*\\n")
    private val h1Scheme = MDScheme(SCHEME_H1, h1Pattern, black,null, Typeface.BOLD, 2.0f)

    private val h2Pattern = Pattern.compile("^##\\s.*\\n")
    private val h2Scheme = MDScheme(SCHEME_H2, h2Pattern, black, null, Typeface.BOLD, 1.8f)

    private val h3Pattern = Pattern.compile("^###\\s.*\\n")
    private val h3Scheme = MDScheme(SCHEME_H3, h3Pattern, black, null, Typeface.BOLD, 1.6f)

    private val h4Pattern = Pattern.compile("^####\\s.*\\n")
    private val h4Scheme = MDScheme(SCHEME_H4, h4Pattern, black, null, Typeface.BOLD, 1.4f)

    private val h5Pattern = Pattern.compile("^#####\\s.*\\n")
    private val h5Scheme = MDScheme(SCHEME_H5, h5Pattern, black, null, Typeface.BOLD, 1.2f)

    private val h6Pattern = Pattern.compile("^######\\s.*\\n")
    private val h6Scheme = MDScheme(SCHEME_H6, h6Pattern, black, null, Typeface.BOLD, 1.0f)

    private val boldPattern = Pattern.compile("\\*\\*.*\\*\\*")
    private val boldScheme = MDScheme(SCHEME_BOLD, boldPattern, black, null, Typeface.BOLD)

    private val emphasisPattern = Pattern.compile("_.*_")
    private val emphasesScheme = MDScheme(SCHEME_EMPHASES, emphasisPattern, black, null, Typeface.ITALIC)

    private val inlineCodePattern = Pattern.compile("`.*`")
    private val inlineCodeScheme = MDScheme(SCHEME_CODE_INLINE, inlineCodePattern, black, codeBackground)

    private val imagePattern = Pattern.compile("(?:!\\[(?:.*?)]\\((.*?)\\))")
    private val imageScheme = MDScheme(SCHEME_IMAGE, imagePattern)

    private val linkPattern = Pattern.compile("(?:[^!]\\[(.*?)]\\((.*?)\\))")
    private val linkScheme = MDScheme(SCHEME_LINK, linkPattern, link)

    private val schemes = mutableListOf<MDScheme>()

    init {
        schemes.add(h6Scheme)
        schemes.add(h5Scheme)
        schemes.add(h4Scheme)
        schemes.add(h3Scheme)
        schemes.add(h2Scheme)
        schemes.add(h1Scheme)
        schemes.add(linkScheme)
        schemes.add(boldScheme)
        schemes.add(emphasesScheme)
        schemes.add(inlineCodeScheme)
        schemes.add(imageScheme)//must be last as may be async and start/end indexes will change while downloading images
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    private lateinit var span: SpannableStringBuilder

    fun render(){

        span = SpannableStringBuilder(textView.text)

        for(scheme in schemes) {
            l("Evaluating scheme: ${scheme.id}")
            val matcher = scheme.pattern.matcher(span)
            var removed = 0
            while (matcher.find()) {
                when (RANGE_INNER) {
                    scheme.range -> {
                        start = matcher.start() + 1
                        end = matcher.end() - 1
                    }
                    else -> {
                        start = matcher.start() - removed
                        end = matcher.end() - removed
                    }
                }

                when {
                    scheme.foreground != null -> span.setSpan(ForegroundColorSpan(scheme.foreground), start, end, DEFAULT_MODE)
                }
                when {
                    scheme.background != null -> span.setSpan(BackgroundColorSpan(scheme.background), start, end, DEFAULT_MODE)
                }
                when {
                    scheme.textStyle != null -> span.setSpan(StyleSpan(scheme.textStyle), start, end, DEFAULT_MODE)
                }
                when {
                    scheme.scale != null -> span.setSpan(RelativeSizeSpan(scheme.scale), start, end, DEFAULT_MODE)
                }

                when (scheme.id){
                    SCHEME_LINK -> {
                        val linkText = matcher.group(1)

                        span.delete(start+1, end)
                        span.insert(start+1, linkText)

                        val matchEvent = MatchEvent(SCHEME_LINK, matcher.group(), matcher.group(2))
                        span.setSpan(CustomClickableSpan(externalHandler, matchEvent), start+1, start+1 + linkText.length, DEFAULT_MODE)

                        removed += (end - (start+1)) - linkText.length

                    }
                    SCHEME_IMAGE -> {
                        val imageRes = findResource(matcher.group(1))
                        if(imageRes != null){
                            val drawable = textView.context.getDrawable(imageRes)
                            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

                            if (drawable != null){
                                span.setSpan(CustomImageSpan(drawable), start, start+1, DEFAULT_MODE)
                                span.delete(start+1, end)
                            }
                        }else {
                            val matchEvent = MatchEvent(SCHEME_IMAGE, matcher.group(), matcher.group(1), start, end)
                            externalHandler(matchEvent)
                        }
                    }
                    SCHEME_H6 -> {
                        span.delete(start, start + 7)
                        removed += 7
                    }
                    SCHEME_H5 -> {
                        span.delete(start, start + 6)
                        removed += 6
                    }
                    SCHEME_H4 -> {
                        span.delete(start, start + 5)
                        removed += 5
                    }
                    SCHEME_H3 -> {
                        span.delete(start, start + 4)
                        removed += 4
                    }
                    SCHEME_H2 -> {
                        span.delete(start, start + 3)
                        removed += 3
                    }
                    SCHEME_H1 -> {
                        span.delete(start, start + 2)
                        removed += 2
                    }
                    SCHEME_BOLD -> {
                        span.delete(end-2, end)
                        span.delete(start, start+2)
                        removed += 4
                    }
                    SCHEME_EMPHASES -> {
                        span.delete(end - 1, end)
                        span.delete(start, start + 1)
                        removed += 2
                    }
                    SCHEME_CODE_INLINE -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            span.setSpan(TypefaceSpan(Typeface.MONOSPACE), start, end, DEFAULT_MODE)
                        }

                        span.delete(end-1, end)
                        span.delete(start, start+1)
                        removed += 2
                    }
                }
            }
        }

        textView.text =  span
    }

    fun insertImage(bitmap: Bitmap?, matchEvent: MatchEvent) {
        if(bitmap == null) return

        span.setSpan(ImageSpan(textView.context, resizeImage(bitmap)), matchEvent.start, matchEvent.start+1, DEFAULT_MODE)
        span.delete(matchEvent.start+1, matchEvent.end)
        textView.text = span
    }

    private fun findResource(imageRef: String): Int? {
        return try {
            val idField = R.drawable::class.java.getDeclaredField(imageRef)
            idField.getInt(idField)
        } catch (e: NoSuchFieldException) {
            null
        }
    }

    private class CustomImageSpan(val image: Drawable): DynamicDrawableSpan() {
        override fun getDrawable(): Drawable {
                return image
        }
    }

    private class CustomClickableSpan(var externalHandler: (matchEvent: MatchEvent) -> Unit, val matchEvent: MatchEvent): ClickableSpan(){
        override fun onClick(widget: View) {
            externalHandler(matchEvent)
        }
    }

    private fun l(message: String){
        Log.d("MDL:::", "renderer: $message")
    }
}