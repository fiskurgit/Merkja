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
import android.view.View
import android.widget.TextView
import android.graphics.RectF
import androidx.annotation.ColorInt

/*

    This is a single file Markdown renderer.
    It's very basic, but is a simple drop-in class to use in a project so you don't need an external library dependency.
    If you need more features and a full API use Markwon instead: https://github.com/noties/Markwon

    I should probably have read this before starting work: https://developer.android.com/reference/java/util/regex/Pattern

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
        private const val SCHEME_ORDERED_LIST = 12
        private const val SCHEME_UNORDERED_LIST = 13
        private const val SCHEME_QUOTE = 14
        private const val SCHEME_CODE_BLOCK = 15

        const val SCHEME_IMAGE = 10
        const val SCHEME_LINK = 11

        private const val LINE_START = "(?:\\A|\\R)"

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

    private var codeBackground = Color.parseColor("#DEDEDE")
    private var linkColor = Color.parseColor("#cc0000")

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
        val scale: Float? = null
    )

    private var placeholderCounter = 0
    private lateinit var span: SpannableStringBuilder

    private val schemes = mutableListOf<MDScheme>()

    init {
        schemes.add(MDScheme(SCHEME_H6, Pattern.compile("$LINE_START######\\s(.*\\R)"), 1.0f))
        schemes.add(MDScheme(SCHEME_H5, Pattern.compile("$LINE_START#####\\s(.*\\R)"), 1.2f))
        schemes.add(MDScheme(SCHEME_H4, Pattern.compile("$LINE_START####\\s(.*\\R)"), 1.4f))
        schemes.add(MDScheme(SCHEME_H3, Pattern.compile("$LINE_START###\\s(.*\\R)"), 1.6f))
        schemes.add(MDScheme(SCHEME_H2, Pattern.compile("$LINE_START##\\s(.*\\R)"), 1.8f))
        schemes.add(MDScheme(SCHEME_H1, Pattern.compile("$LINE_START#\\s(.*\\R)"), 2.0f))
        schemes.add(MDScheme(SCHEME_LINK, Pattern.compile("(?:[^!]\\[(.*?)]\\((.*?)\\))")))
        schemes.add(MDScheme(SCHEME_BOLD, Pattern.compile("\\*\\*.*\\*\\*")))
        schemes.add(MDScheme(SCHEME_EMPHASES, Pattern.compile("_.*_")))
        schemes.add(MDScheme(SCHEME_ORDERED_LIST, Pattern.compile("([0-9]+.)(.*)\\n")))
        schemes.add(MDScheme(SCHEME_UNORDERED_LIST, Pattern.compile("\\*.*\\n")))
        schemes.add(MDScheme(SCHEME_CODE_BLOCK, Pattern.compile("(?:```)\\n*\\X+(?:```)")))
        schemes.add(MDScheme(SCHEME_CODE_INLINE, Pattern.compile("`.*`")))
        schemes.add(MDScheme(SCHEME_QUOTE, Pattern.compile("$LINE_START>.*\\n")))
        schemes.add(MDScheme(SCHEME_IMAGE, Pattern.compile("(?:!\\[(?:.*?)]\\((.*?)\\))")))
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    fun render(){

        span = SpannableStringBuilder(textView.text)

        for(scheme in schemes) {
            val matcher = scheme.pattern.matcher(span)
            var removed = 0
            while (matcher.find()) {
                start = matcher.start() - removed
                end = matcher.end() - removed

                when (scheme.id){
                    SCHEME_CODE_BLOCK -> {
                        span.setSpan(FullWidthBackgroundSpan(Color.parseColor("#ededed")), start, end, DEFAULT_MODE)

                        if (isAndroidPPlus()) span.setSpan(TypefaceSpan(Typeface.MONOSPACE), start, end, DEFAULT_MODE)

                        span.delete(start, start + 3)
                        span.delete(end - 4, end - 1)

                        removed += 6
                    }
                    SCHEME_QUOTE -> {
                        span.replace(start+1, start + 2, " ")//replace > with space

                        //todo - line height needs to be increased too if possible:
                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.LTGRAY, dpToPx(4), 0), start, end, DEFAULT_MODE)
                    }
                    SCHEME_ORDERED_LIST -> {
                        val number = matcher.group(1)
                        span.setSpan(StyleSpan(Typeface.BOLD), start, start + number.length, DEFAULT_MODE)
                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                    }
                    SCHEME_UNORDERED_LIST -> {
                        //There is BulletSpan but this is less problematic, and the more useful BulletSpan is AndroidP onwards anyway
                        span.replace(start, start + 1, "•")

                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                    }
                    SCHEME_LINK -> {
                        span.setSpan(ForegroundColorSpan(linkColor), start, end, DEFAULT_MODE)

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
                            val placeholder = "${System.currentTimeMillis()}_$placeholderCounter"

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
                        span.setSpan(StyleSpan(Typeface.BOLD), start, end, DEFAULT_MODE)
                        span.delete(end-2, end)
                        span.delete(start, start+2)
                        removed += 4
                    }
                    SCHEME_EMPHASES -> {
                        span.setSpan(StyleSpan(Typeface.ITALIC), start, end, DEFAULT_MODE)
                        span.delete(end - 1, end)
                        span.delete(start, start + 1)
                        removed += 2
                    }
                    SCHEME_CODE_INLINE -> {
                        if (isAndroidPPlus()) span.setSpan(TypefaceSpan(Typeface.MONOSPACE), start, end, DEFAULT_MODE)

                        span.setSpan(BackgroundColorSpan(codeBackground), start, end, DEFAULT_MODE)

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

        val start = span.indexOf(matchEvent.matchText, 0, false)

        if(start != -1) {
            span.setSpan(ImageSpan(textView.context, resizeImage(bitmap)), start, start + 1, DEFAULT_MODE)
            span.delete(start + 1, start + matchEvent.matchText.length)
            textView.text = span
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

    private fun dpToPx(dp: Int): Int = (dp * Resources.getSystem().displayMetrics.density).toInt()
    private fun isAndroidPPlus(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}
