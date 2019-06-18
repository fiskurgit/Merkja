# Simple Markdown Renderer

* Headers # to ######
* **Bold**
* _Emphasis_
* `inline code`
* Local and remote images
* [Web Links](https://fiskurgit.github.io)
* [Local links](linked_page.md)

A _sentence with_ **mixed** `styling` and [links](https://fiskurgit.github.io) ...

1. an
2. ordered
3. list


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


