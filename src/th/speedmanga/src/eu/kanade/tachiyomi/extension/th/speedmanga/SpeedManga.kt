package eu.kanade.tachiyomi.extension.th.speedmanga

import eu.kanade.tachiyomi.multisrc.mangareader.MangaReader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SpeedManga :
    MangaReader(
        "Speed Manga",
        "https://speed-manga.net",
        "th",
    ) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        //  Keeping this in case instability occurs.
        // .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/manga/?order=popular&page=$page", headers)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/manga/?order=update&page=$page", headers)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/manga/".toHttpUrl().newBuilder()
        if (query.isNotBlank()) url.addQueryParameter("s", query)
        filters.forEach { filter ->
            when (filter) {
                is OrderFilter -> orderOptions[filter.state].second.takeIf { it.isNotEmpty() }
                    ?.let { url.addQueryParameter("order", it) }

                is StatusFilter -> statusOptions[filter.state].second.takeIf { it.isNotEmpty() }
                    ?.let { url.addQueryParameter("status", it) }

                is TypeFilter -> typeOptions[filter.state].second.takeIf { it.isNotEmpty() }
                    ?.let { url.addQueryParameter("type", it) }

                is GenreFilter -> filter.state.filter { it.state }
                    .forEach { url.addQueryParameter("genre[]", it.value) }

                else -> {}
            }
        }
        url.addQueryParameter("page", page.toString())
        return GET(url.build(), headers)
    }

    override fun searchMangaSelector() = "div.listupd div.bs div.bsx"

    override fun searchMangaFromElement(element: Element) = SManga.create().apply {
        val a = element.selectFirst("a")!!
        setUrlWithoutDomain(a.attr("href"))
        title = element.selectFirst("div.tt")?.text() ?: a.attr("title")
        thumbnail_url = element.selectFirst("img")?.imgAttr()
    }

    override fun searchMangaNextPageSelector() = "a.next.page-numbers"

    override fun mangaDetailsParse(response: Response): SManga {
        val doc = response.asJsoup()
        return SManga.create().apply {
            title = doc.selectFirst("h1.entry-title")?.text() ?: ""
            thumbnail_url = doc.selectFirst("div.thumb img")
                ?.attr("src")?.ifEmpty { null }
                ?: doc.selectFirst("div.thumb img")?.attr("data-src")
            genre = doc.select("span.mgen a").joinToString { it.text() }
            author = doc.selectFirst("span[itemprop=author]")?.text()?.trim()
            description = doc.selectFirst("div.entry-content, div[itemprop=description]")?.text()
            val statusText = doc.select("div.tsinfo .imptdt, div.infotable tr")
                .firstOrNull {
                    it.text().contains("สถานะ", ignoreCase = true) ||
                        it.text().contains("Status", ignoreCase = true)
                }
                ?.text()?.lowercase() ?: ""
            status = when {
                statusText.contains("ongoing") || statusText.contains("กำลังดำเนิน") -> SManga.ONGOING
                statusText.contains("completed") || statusText.contains("จบแล้ว") -> SManga.COMPLETED
                statusText.contains("hiatus") || statusText.contains("หยุด") -> SManga.ON_HIATUS
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
        return document.select("div.eplister li").map { li ->
            SChapter.create().apply {
                val a = li.selectFirst("a")!!
                setUrlWithoutDomain(a.attr("href"))
                name = li.selectFirst("span.chapternum")?.text()?.trim() ?: a.text().trim()
                date_upload = li.selectFirst("span.chapterdate")?.text()?.trim()
                    ?.let { runCatching { dateFormat.parse(it)?.time }.getOrNull() } ?: 0L
            }
        }
    }

    override fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body.string()

        val scriptContent = html.substringAfter("ts_reader.run(")
            .substringBeforeLast(");")

        if (scriptContent.length == html.length) {
            throw Exception("ts_reader.run not found")
        }

        val imagesContent = Regex(
            """"images"\s*:\s*\[(.*?)\]""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(scriptContent)?.groupValues?.get(1)
            ?: throw Exception("No images array found")

        val pages = Regex("""https?:\\?/\\?/[^"]+""")
            .findAll(imagesContent)
            .mapIndexed { i, m ->
                val withFixedSlashes = m.value.replace("\\/", "/")
                val decoded = decodeUnicodeEscapes(withFixedSlashes)
                Page(i, imageUrl = decoded)
            }
            .toList()

        if (pages.isEmpty()) throw Exception("No pages found")
        return pages
    }

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Referer", "$baseUrl/")
            .set("Sec-Fetch-Dest", "image")
            .build()
        return GET(page.imageUrl!!, newHeaders)
    }

    private fun decodeUnicodeEscapes(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            if (i + 5 < input.length &&
                input[i] == '\\' &&
                input[i + 1] == 'u' &&
                input.substring(i + 2, i + 6).all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
            ) {
                sb.append(input.substring(i + 2, i + 6).toInt(16).toChar())
                i += 6
            } else {
                sb.append(input[i])
                i++
            }
        }
        return sb.toString()
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList(
        OrderFilter(),
        StatusFilter(),
        TypeFilter(),
        GenreFilter(),
    )
}
