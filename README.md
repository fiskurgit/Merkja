# Merkja for Android

## Work in progress - do not use

A very simple single class Markdown renderer for when you only need the basic syntax and don't want to add a 3rd-party dependency. Warning: Contains hacky regex and span mangling, I have no idea how unoptimised it is.

It's not meant to be a replacement for a full-featured library, if you need full Markdown support use [Markwon](https://github.com/noties/Markwon)

This isn't a library, just copy the [SimpleMDRenderer](https://github.com/fiskurgit/Merkja/blob/master/app/src/main/java/fiskurgit/android/markdownrenderer/SimpleMDRenderer.kt) class to your project, usage:

## Simple

```kotlin
text_view.text = markdownString
SimpleMDRenderer(text_view).render()
```

## Handling Links and Images

```kotlin
text_view.text = markdownString

val simpleMDRenderer = SimpleMDRenderer(text_view) { matchEvent ->

    when (matchEvent.schemeType){
        SimpleMDRenderer.SCHEME_IMAGE -> loadImage(matchEvent)
        SimpleMDRenderer.SCHEME_LINK -> handleLink(matchEvent)
    }
}
simpleMDRenderer.render()

...

fun loadImage(matchEvent: MatchEvent){
    val imageUrl = matchEvent.value
    //fetch image async then:
    runOnUiThread {
        simpleMDRenderer.insertImage(bitmap, matchEvent)
    }
}

```

---

## Supported

* Big headers down to small headers
* **Bold**
* _italics_
* `inline code`
* Local images
* Async remote images
* Local and remote links (websites and local markdown files)
* Ordered and unordered lists

## Coming Soon or Work in Progress

* ```code blocks ```
* Custom links
* Horizontal rule
* Quotes

## Not Supported

* Variants for the supported tags above
* Probably something you want to use (use [Markwon](https://github.com/noties/Markwon) instead)


### Dev Notes

* Ordered and Unordered lists both need to check for line-start. Currently will trigger in text body...


