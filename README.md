# Merkja

A very simple single class Markdown renderer for when you only need the basic syntax and don't want to add a 3rd-party dependency. 

This isn't meant to be a replacement for a full-featured library, if you need full Markdown support use [Markwon](https://github.com/noties/Markwon)

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
```

---

## Supported

* Big headers down to small headers
* **Bold**
* _italics_
* `inline code`
* Local and remote Images
* Local and remote links (websites and local markdown files)

## Coming Soon

* ```code blocks ```
* Lists
* Custom links
* Horizontal rule

## Not Supported

* Variants for the supported tags above
* Probably something you want to use (use [Markwon](https://github.com/noties/Markwon) instead)
