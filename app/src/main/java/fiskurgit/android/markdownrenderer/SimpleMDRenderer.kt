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
import android.graphics.RectF
import androidx.annotation.ColorInt


/*

    This is a single file Markdown renderer.
    It's very basic, but is a simple drop-in class to use in a project so you don't need an external library dependency.
    If you need more features and a full API use Markwon instead: https://github.com/noties/Markwon

    Supported syntax:


    ![An image](imageUrl or resource name)

    Image resources from the .apk will load automatically, for remote images, or Uris you'll need to fetch them yourself.
    examples:

    ![An image](ic_app_icon)
    ![An image](https://website.com/image.png)

    For remote images supply an external handler, fetch asynchronously then call insertImage() with the bitmap.

    Links also need to be handled by an external handler

 */
class SimpleMDRenderer(private val textView: TextView, var externalHandler: (matchEvent: MatchEvent) -> Unit = { _ -> }) {

    companion object {

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
        const val SCHEME_ORDERED_LIST = 12
        const val SCHEME_UNORDERED_LIST = 13
        const val SCHEME_QUOTE = 14

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

    private var start = 0
    private var end = 0

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
        val scale: Float? = null
    )


    private val LINE_START = "(?:\\A|\\R)"

    private val h1Pattern = Pattern.compile("$LINE_START#\\s(.*\\R)")
    private val h1Scheme = MDScheme(SCHEME_H1, h1Pattern, black,null, Typeface.BOLD, 2.0f)

    private val h2Pattern = Pattern.compile("$LINE_START##\\s(.*\\R)")
    private val h2Scheme = MDScheme(SCHEME_H2, h2Pattern, black, null, Typeface.BOLD, 1.8f)

    private val h3Pattern = Pattern.compile("$LINE_START###\\s(.*\\R)")
    private val h3Scheme = MDScheme(SCHEME_H3, h3Pattern, black, null, Typeface.BOLD, 1.6f)

    private val h4Pattern = Pattern.compile("$LINE_START####\\s(.*\\R)")
    private val h4Scheme = MDScheme(SCHEME_H4, h4Pattern, black, null, Typeface.BOLD, 1.4f)

    private val h5Pattern = Pattern.compile("$LINE_START#####\\s(.*\\R)")
    private val h5Scheme = MDScheme(SCHEME_H5, h5Pattern, black, null, Typeface.BOLD, 1.2f)

    private val h6Pattern = Pattern.compile("$LINE_START######\\s(.*\\R)")
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

    private val orderedListPattern = Pattern.compile("([0-9]+.)(.*)\\n")
    private val orderedListScheme = MDScheme(SCHEME_ORDERED_LIST, orderedListPattern, black)

    private val unorderedListPattern = Pattern.compile("\\*.*\\n")
    private val unorderedListScheme = MDScheme(SCHEME_UNORDERED_LIST, unorderedListPattern, black)

    private val quotePattern = Pattern.compile(">.*\\n")
    private val quoteScheme = MDScheme(SCHEME_QUOTE, quotePattern, black)



    private val schemes = mutableListOf<MDScheme>()

    private var placeholderCounter = 0

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
        schemes.add(orderedListScheme)
        schemes.add(unorderedListScheme)
        schemes.add(quoteScheme)
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
                start = matcher.start() - removed
                end = matcher.end() - removed

                when {
                    scheme.foreground != null -> span.setSpan(ForegroundColorSpan(scheme.foreground), start, end, DEFAULT_MODE)
                }
                when {
                    scheme.background != null -> span.setSpan(BackgroundColorSpan(scheme.background), start, end, DEFAULT_MODE)
                }
                when {
                    scheme.textStyle != null -> span.setSpan(StyleSpan(scheme.textStyle), start, end, DEFAULT_MODE)
                }

                when (scheme.id){
                    SCHEME_QUOTE -> {
                        span.replace(start, start + 1, " ")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            //todo - line height needs to be increased too if possible:
                            span.setSpan(QuoteSpan(Color.LTGRAY, dpToPx(4), 0), start, end, DEFAULT_MODE)
                        }
                    }
                    SCHEME_ORDERED_LIST -> {
                        val number = matcher.group(1)
                        span.setSpan(StyleSpan(Typeface.BOLD), start, start + number.length, DEFAULT_MODE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                        }
                    }
                    SCHEME_UNORDERED_LIST -> {
                        //There is BulletSpan but this is less problematic, and the more useful BulletSpan is AndroidP onwards anyway
                        span.replace(start, start + 1, "•")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                        }

                        span.setSpan(FullWidthBackgroundSpan(Color.parseColor("#ededed")), start, end, DEFAULT_MODE)


                    }
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

                                removed += (end - start) - 1
                            }
                        }else {
                            //Async images could arrive back in any order (or not at all), so inject placeholder text
                            placeholderCounter++
                            val placeholder = "XX${System.currentTimeMillis()}_$placeholderCounter"

                            val imageUri = matcher.group(1)
                            val matchEvent = MatchEvent(SCHEME_IMAGE, placeholder, imageUri)

                            span.delete(start, end)
                            span.insert(start, placeholder)

                            removed +=  (end - start) - placeholder.length

                            externalHandler(matchEvent)
                        }
                    }
                    SCHEME_H6, SCHEME_H5, SCHEME_H4, SCHEME_H3, SCHEME_H2, SCHEME_H1 -> {
                        val value =  matcher.group(1)
                        span.delete(start, end)
                        removed += (end - start) - value.length
                        span.insert(start, value)
                        span.setSpan(ForegroundColorSpan(Color.BLACK), start, start + value.length, DEFAULT_MODE)
                        span.setSpan(StyleSpan(Typeface.BOLD), start, start + value.length, DEFAULT_MODE)
                        span.setSpan(RelativeSizeSpan(scheme.scale ?: 1f), start, start + value.length, DEFAULT_MODE)
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

    private fun dpToPx(dp: Int): Int{
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun insertImage(bitmap: Bitmap?, matchEvent: MatchEvent) {
        if(bitmap == null) return

        l("inserting image: ${matchEvent.value}")

        val start = span.indexOf(matchEvent.matchText, 0, false)

        if(start != -1) {
            span.setSpan(ImageSpan(textView.context, resizeImage(bitmap)), start, start + 1, DEFAULT_MODE)
            span.delete(start + 1, start + matchEvent.matchText.length)
            textView.text = span
        }else {
            val placeholder = matchEvent.matchText
            l("Could not find placeholder $placeholder")
        }

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

    private class FullWidthBackgroundSpan(@ColorInt color: Int): LineBackgroundSpan{

        val paint = Paint()

        init {
            paint.color = color
        }
        override fun drawBackground(c: Canvas?, p: Paint?, left: Int, right: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence?, start: Int, end: Int, lnum: Int) {
            c?.drawRect(RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()), paint)
        }
    }

    private fun l(message: String){
        Log.d("MDL:::", "renderer: $message")
    }
}