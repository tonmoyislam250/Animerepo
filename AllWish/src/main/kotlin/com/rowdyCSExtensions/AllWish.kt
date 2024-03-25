package com.RowdyAvocado

// import android.util.Log

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AllWish(val plugin: AllWishPlugin) : MainAPI() {
    override var mainUrl = AllWish.mainUrl
    override var name = "AllWish"
    override var supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false

    val mapper = jacksonObjectMapper()
    var sectionNamesList: List<String> = emptyList()

    companion object {
        val mainUrl = "https://all-wish.me"
        val header = mapOf("X-Requested-With" to "XMLHttpRequest")
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/filter?&type=Latest+Updated&language=sub&sort=default&page=" to
                            "Latest Updated (Sub)",
                    "$mainUrl/filter?&type=Latest+Updated&language=dub&sort=default&page=" to
                            "Latest Updated (Dub)"
            )

    private fun searchResponseBuilder(res: Document): List<AnimeSearchResponse> {
        var results = emptyList<AnimeSearchResponse>()
        res.select("div.item").forEach { item ->
            val name = item.selectFirst("div.name > a")?.text() ?: ""
            val url = item.selectFirst("div.name > a")?.attr("href") ?: ""
            results +=
                    newAnimeSearchResponse(name, url) {
                        this.posterUrl = item.selectFirst("a.poster img")?.attr("data-src")
                        // Log.d("Rushi", this.posterUrl ?: "")
                    }
        }
        return results
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res = app.get(request.data + page.toString()).document
        val searchRes = searchResponseBuilder(res)
        return newHomePageResponse(request.name, searchRes, true)
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url).document
        val id = res.select("main > div.container").attr("data-id")
        val data = res.selectFirst("div#media-info")
        val name = data?.selectFirst("h1.title")?.text()?.trim()?.replace(" (Dub)", "") ?: ""
        val posterRegex = Regex("/'(.*)'/gm")

        var episodes = emptyList<Episode>()

        val epRes =
                app.get("$mainUrl/ajax/episode/list/$id", AllWish.header).parsedSafe<APIResponse>()
        if (epRes?.status == 200) {
            epRes.html.select("div.range > div > a").forEach { ep ->
                val epId = ep.attr("data-ids")
                episodes +=
                        newEpisode(epId) { this.episode = ep.attr("data-num").toFloat().toInt() }
            }
        }

        return newAnimeLoadResponse(name, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, episodes)
            this.plot = data?.selectFirst("div.description > div.full > div")?.text()?.trim()
            this.backgroundPosterUrl =
                    posterRegex
                            .find(res.selectFirst("div.media-bg")?.attr("style")!!)
                            ?.destructured
                            ?.toList()
                            ?.get(0)
                            ?: data?.selectFirst("div.poster img")?.attr("src") ?: ""
            this.year =
                    data?.select("div.meta > div > span")
                            ?.find { it.attr("itemprop").equals("dateCreated") }
                            ?.text()
                            ?.toInt()
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val id = data.replace(mainUrl + "/", "")
        val res =
                app.get("$mainUrl/ajax/server/list?servers=$id", AllWish.header)
                        .parsedSafe<APIResponse>()
        if (res?.status == 200) {
            res.html.select("div.server-list > div.server").forEach { server ->
                val serverName = server.selectFirst("div > span")?.text() ?: ""
                val dataId = server.attr("data-link-id")
                val links = AllWishExtractor().getStreamUrl(serverName, dataId)
                if (links.isNotEmpty()) {
                    links.forEach { link ->
                        callback.invoke(
                                ExtractorLink(
                                        serverName,
                                        serverName,
                                        link,
                                        "",
                                        0,
                                        link.contains(".m3u8")
                                )
                        )
                    }
                }
            }
        }
        return true
    }

    data class APIResponse(
            @JsonProperty("status") val status: Int? = null,
            @JsonProperty("result") val result: String? = null,
            val html: Document = Jsoup.parse(result ?: "")
    )
}
