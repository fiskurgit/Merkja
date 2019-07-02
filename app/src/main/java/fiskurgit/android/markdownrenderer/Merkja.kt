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

// See https://github.com/fiskurgit/Merkja for more info
class Merkja(private val textView: TextView, var externalHandler: (matchEvent: MatchEvent) -> Unit = { _ -> }) {

    companion object {

        private const val DEFAULT_MODE = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

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

    private var placeholderCounter = 0
    private lateinit var span: SpannableStringBuilder

    data class MatchEvent(val schemeType: Int, val matchText: String, val value: String)

    enum class Scheme(val pattern: Pattern, val scale: Float? = null) {
        H6(Pattern.compile("$LINE_START######\\s(.*\\R)"), 1.0f),
        H5(Pattern.compile("$LINE_START#####\\s(.*\\R)"), 1.2f),
        H4(Pattern.compile("$LINE_START####\\s(.*\\R)"), 1.4f),
        H3(Pattern.compile("$LINE_START###\\s(.*\\R)"), 1.6f),
        H2(Pattern.compile("$LINE_START##\\s(.*\\R)"), 1.8f),
        H1(Pattern.compile("$LINE_START#\\s(.*\\R)"), 2.0f),
        LINK(Pattern.compile("(?:[^!]\\[(.*?)]\\((.*?)\\))")),
        BOLD(Pattern.compile("\\*\\*.*\\*\\*")),
        EMPHASIS(Pattern.compile("_.*_")),
        ORDERED_LIST(Pattern.compile("([0-9]+.)(.*)\\n")),
        UNORDERED_LIST(Pattern.compile("\\*.*\\n")),
        CODE_BLOCK(Pattern.compile("(?:```)\\n*\\X+(?:```)")),
        CODE_INLINE(Pattern.compile("`.*`")),
        QUOTE(Pattern.compile("$LINE_START>.*\\n")),
        IMAGE(Pattern.compile("(?:!\\[(?:.*?)]\\((.*?)\\))"))
    }

    init {
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    fun render(){

        span = SpannableStringBuilder(textView.text)

        for(scheme in Scheme.values()){
            val matcher = scheme.pattern.matcher(span)
            var removed = 0
            while (matcher.find()) {
                start = matcher.start() - removed
                end = matcher.end() - removed

                when (scheme.name){
                    Scheme.CODE_BLOCK.name -> {
                        span.setSpan(FullWidthBackgroundSpan(Color.parseColor("#ededed")), start, end, DEFAULT_MODE)

                        if (isAndroidPPlus()) span.setSpan(TypefaceSpan(Typeface.MONOSPACE), start, end, DEFAULT_MODE)

                        span.delete(start, start + 3)
                        span.delete(end - 4, end - 1)

                        removed += 6
                    }
                    Scheme.QUOTE.name -> {
                        span.replace(start+1, start + 2, " ")//replace > with space

                        //todo - line height needs to be increased too if possible:
                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.LTGRAY, dpToPx(4), 0), start, end, DEFAULT_MODE)
                    }
                    Scheme.ORDERED_LIST.name -> {
                        val number = matcher.group(1)
                        span.setSpan(StyleSpan(Typeface.BOLD), start, start + number.length, DEFAULT_MODE)
                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                    }
                    Scheme.UNORDERED_LIST.name -> {
                        //There is BulletSpan but this is less problematic, and the more useful BulletSpan is AndroidP onwards anyway
                        span.replace(start, start + 1, "•")

                        if (isAndroidPPlus()) span.setSpan(QuoteSpan(Color.TRANSPARENT, 0, (12 * Resources.getSystem().displayMetrics.density).toInt()), start, end, DEFAULT_MODE)
                    }
                    Scheme.LINK.name -> {
                        span.setSpan(ForegroundColorSpan(linkColor), start, end, DEFAULT_MODE)

                        val linkText = matcher.group(1)

                        span.delete(start+1, end)
                        span.insert(start+1, linkText)

                        val matchEvent = MatchEvent(SCHEME_LINK, matcher.group(), matcher.group(2))
                        span.setSpan(CustomClickableSpan(externalHandler, matchEvent), start+1, start+1 + linkText.length, DEFAULT_MODE)

                        removed += (end - (start+1)) - linkText.length

                    }
                    Scheme.IMAGE.name -> {
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
                    Scheme.H6.name, Scheme.H5.name, Scheme.H4.name, Scheme.H3.name, Scheme.H2.name, Scheme.H1.name -> {
                        val value =  matcher.group(1)
                        span.delete(start, end)
                        removed += (end - start) - value.length
                        span.insert(start, value)
                        span.setSpan(ForegroundColorSpan(Color.BLACK), start, start + value.length, DEFAULT_MODE)
                        span.setSpan(StyleSpan(Typeface.BOLD), start, start + value.length, DEFAULT_MODE)
                        span.setSpan(RelativeSizeSpan(scheme.scale ?: 1f), start, start + value.length, DEFAULT_MODE)
                    }
                    Scheme.BOLD.name -> {
                        span.setSpan(StyleSpan(Typeface.BOLD), start, end, DEFAULT_MODE)
                        span.delete(end-2, end)
                        span.delete(start, start+2)
                        removed += 4
                    }
                    Scheme.EMPHASIS.name -> {
                        span.setSpan(StyleSpan(Typeface.ITALIC), start, end, DEFAULT_MODE)
                        span.delete(end - 1, end)
                        span.delete(start, start + 1)
                        removed += 2
                    }
                    Scheme.CODE_INLINE.name -> {
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
