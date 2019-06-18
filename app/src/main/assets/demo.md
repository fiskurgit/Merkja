# Simple Markdown Renderer

## Supported

* Headers # to ######
* **Bold**
* _Emphasis_
* `inline code`
* Local and remote images
* [Web Links](https://fiskurgit.github.io)
* [Local links](linked_page.md)


#### Remote image:
![Remote Image](https://fiskurgit.github.io/blog/series1/sample1.png)

#### Local image:
![Local image](hexagram_res)

```
SCHEME_BOLD -> {
    span.delete(end-2, end)
    span.delete(start, start+2)
    removed += 4
}

```



#### Another remote image:
![Remote Image](https://fiskurgit.github.io/blog/series1/sample2.png)


> If I never did another film after 'Paris, Texas,' I'd be happy.


