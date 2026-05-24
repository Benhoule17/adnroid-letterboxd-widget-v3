package com.letterboxd.widget

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.util.regex.Pattern

object LetterboxdScraper {

    private const val TAG = "LetterboxdScraper"
    private const val BASE_URL = "https://letterboxd.com"

    // ── Recents ─────────────────────────────────────────────────────────────

    /**
     * Fetches up to 4 recent films from the user's RSS feed.
     * Returns a list of [Film] objects with slug, posterUrl, and rating.
     */
    suspend fun fetchRecents(username: String): List<Film> = withContext(Dispatchers.IO) {
        try {
            val rssUrl = "$BASE_URL/$username/rss"
            val xml = URL(rssUrl).readText(Charsets.UTF_8)
            parseRecentsRss(xml)
        } catch (e: Exception) {
            Log.e(TAG, "fetchRecents error", e)
            emptyList()
        }
    }

    private fun parseRecentsRss(xml: String): List<Film> {
        val films = mutableListOf<Film>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inItem = false
            var link = ""
            var rating = -1f
            var posterUrl = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> {
                                inItem = true
                                link = ""; rating = -1f; posterUrl = ""
                            }
                            "link" -> if (inItem) {
                                // <link> is CDATA in Letterboxd RSS; next TEXT has the URL
                                try { link = parser.nextText().trim() } catch (_: Exception) {}
                            }
                            "memberRating" -> if (inItem) {
                                try { rating = parser.nextText().trim().toFloat() } catch (_: Exception) {}
                            }
                            "description" -> if (inItem) {
                                try {
                                    val desc = parser.nextText()
                                    posterUrl = extractImgSrc(desc)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" && inItem) {
                            inItem = false
                            val slug = extractSlug(link)
                            if (slug.isNotEmpty() && posterUrl.isNotEmpty()) {
                                films.add(Film(slug, posterUrl, rating))
                                if (films.size == 4) return films
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseRecentsRss error", e)
        }
        return films
    }

    // ── Favourites ───────────────────────────────────────────────────────────

    /**
     * Fetches up to 4 favourite films by scraping the user's Letterboxd profile page.
     */
    suspend fun fetchFavourites(username: String): List<Film> = withContext(Dispatchers.IO) {
        try {
            val html = URL("$BASE_URL/$username").readText(Charsets.UTF_8)
            parseFavouritesHtml(html)
        } catch (e: Exception) {
            Log.e(TAG, "fetchFavourites error", e)
            emptyList()
        }
    }

    private fun parseFavouritesHtml(html: String): List<Film> {
        val films = mutableListOf<Film>()

        // Find the #favourites section
        val favIdx = html.indexOf("id=\"favourites\"")
        if (favIdx < 0) return films

        val section = html.substring(favIdx, minOf(favIdx + 8000, html.length))

        // Match each film item: data-item-slug="..." with an img srcset="..."
        val slugPattern = Pattern.compile("""data-item-slug="([^"]+)"""")
        val srcsetPattern = Pattern.compile("""srcset="([^"]+)"""")

        val slugMatcher = slugPattern.matcher(section)
        while (slugMatcher.find() && films.size < 4) {
            val slug = slugMatcher.group(1) ?: continue
            // Find the nearest srcset after this slug occurrence
            val afterSlug = section.substring(slugMatcher.end())
            val srcsetMatcher = srcsetPattern.matcher(afterSlug)
            if (srcsetMatcher.find()) {
                val srcset = srcsetMatcher.group(1) ?: continue
                // Take first URL from srcset (format: "url 1x, url 2x")
                val posterUrl = srcset.split(",").first().trim().split(" ").first()
                if (posterUrl.isNotBlank() && !posterUrl.contains("empty-poster")) {
                    films.add(Film(slug, posterUrl))
                }
            }
        }
        return films
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun extractSlug(url: String): String {
        val match = Regex("""/film/([^/]+)""").find(url)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractImgSrc(html: String): String {
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""").find(html)
        return match?.groupValues?.get(1)?.replace(" ", "%20") ?: ""
    }
}
