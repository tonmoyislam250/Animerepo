package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*

class MangaDex(val plugin: MangaDexPlugin) : MainAPI() {
    override var name = "MangaDex"
    override var mainUrl = "https://mangadex.org"
    override var supportedTypes = setOf(TvType.Others)
    override var lang = "en"
    override val hasMainPage = true

    var apiUrl = "https://api.mangadex.org"
    var limit = 15
    val mapper = jacksonObjectMapper()

    override suspend fun search(query: String): List<SearchResponse> {
        val url =
                "$apiUrl/manga?title=$query&limit=20&hasAvailableChapters=true&includes[]=cover_art&order[relevance]=desc"
        val res = app.get(url).parsedSafe<MultiMangaResponse>()!!
        if (res.result.equals("ok")) return searchResponseBuilder(res.data)
        else return listOf<SearchResponse>()
    }

    override val mainPage =
            mainPageOf(
                    "$apiUrl/manga?limit=$limit&offset=#&order[createdAt]=desc&includes[]=cover_art&hasAvailableChapters=true" to
                            "Latest Updates"
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val res =
                app.get(request.data.replace("#", (page * limit).toString()))
                        .parsedSafe<MultiMangaResponse>()
        if (res?.result.equals("ok")) {
            return newHomePageResponse(request.name, searchResponseBuilder(res!!.data), true)
        }
        throw ErrorLoadingException("Nothing to show here.")
    }

    override suspend fun load(url: String): LoadResponse {
        val manga =
                app.get("${url.replace(mainUrl, apiUrl)}?includes[]=cover_art")
                        .parsedSafe<SingleMangaResponse>()
                        ?.data!!
        val mangaId = manga.id
        val poster = manga.rel.find { it.type.equals("cover_art") }?.attrs!!.fileName
        val posterUrl = "$mainUrl/covers/$mangaId/$poster"
        var chaptersList = emptyList<ChapterData?>()
        var counter = 0
        val limit = 500

        while (counter >= 0) {
            val res =
                    app.get(
                                    "$apiUrl/manga/$mangaId/feed?includes[]=scanlation_group&order[volume]=asc&order[chapter]=asc&limit=$limit&offset=${counter*limit}&translatedLanguage[]=en"
                            )
                            .parsedSafe<ChaptersListResponse>()!!
            chaptersList += res.data
            if ((res.limit + res.offset) < res.total) counter += 1 else counter = -1
        }

        val chapters =
                chaptersList.mapNotNull { chapter ->
                    newEpisode(chapter!!.id) {
                        this.name = chapter.attrs.title
                        this.episode = chapter.attrs.chapter?.toFloat()?.toInt()
                        this.season = chapter.attrs.volume?.toFloat()?.toInt()
                        this.data = chapter.id
                    }
                }

        return newAnimeLoadResponse(manga.attrs.title.en!!, url, TvType.Anime) {
            addEpisodes(DubStatus.Dubbed, chapters)
            this.backgroundPosterUrl = posterUrl
            this.posterUrl = posterUrl
            this.plot = manga.attrs.desc.en
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val chapterPages =
                app.get("$apiUrl/at-home/server/$data?forcePort443=false")
                        .parsedSafe<ChapterPagesResponse>()!!
        val imageUrlList =
                if (plugin.dataSaver)
                        chapterPages.chapter.dataSaver.sorted().mapNotNull {
                            chapterPages.baseUrl +
                                    "/data-saver/" +
                                    chapterPages.chapter.hash +
                                    "/" +
                                    it
                        }
                else
                        chapterPages.chapter.data.sorted().mapNotNull {
                            chapterPages.baseUrl + "/data/" + chapterPages.chapter.hash + "/" + it
                        }
        plugin.openFragment(imageUrlList)
        return false
    }

    private fun searchResponseBuilder(dataList: List<MangaData?>): List<SearchResponse> {
        val searchCollection =
                dataList.mapNotNull { manga ->
                    val mangaId = manga?.id
                    val poster = manga!!.rel.find { it.type.equals("cover_art") }?.attrs!!.fileName
                    val posterUrl = "$mainUrl/covers/$mangaId/$poster"
                    newAnimeSearchResponse(manga.attrs.title.en!!, "manga/" + manga.id) {
                        this.posterUrl = posterUrl
                    }
                }
        return searchCollection
    }

    // ======================== Manga API Response ==================================

    data class MultiMangaResponse(
            @JsonProperty("result") var result: String,
            @JsonProperty("data") var data: List<MangaData?>
    )

    data class SingleMangaResponse(
            @JsonProperty("result") var result: String,
            @JsonProperty("data") var data: MangaData?
    )

    data class MangaData(
            @JsonProperty("id") var id: String,
            @JsonProperty("attributes") var attrs: MangaAttributes,
            @JsonProperty("relationships") var rel: List<MangaRelationships>,
    )

    data class MangaAttributes(
            @JsonProperty("title") var title: MangaTitle,
            @JsonProperty("description") var desc: MangaDesc
    )

    data class MangaTitle(@JsonProperty("en") var en: String? = null)

    data class MangaDesc(@JsonProperty("en") var en: String? = null)

    data class MangaRelationships(
            @JsonProperty("id") var id: String,
            @JsonProperty("type") var type: String,
            @JsonProperty("attributes") var attrs: MangaRelationshipsAttributes? = null,
    )

    data class MangaRelationshipsAttributes(
            @JsonProperty("fileName") var fileName: String? = null,
    )

    // ====================== Chapter API Response ==============================

    data class ChaptersListResponse(
            @JsonProperty("result") var result: String,
            @JsonProperty("data") var data: List<ChapterData?>,
            @JsonProperty("limit") var limit: Int,
            @JsonProperty("offset") var offset: Int,
            @JsonProperty("total") var total: Int,
    )

    data class ChapterData(
            @JsonProperty("id") var id: String,
            @JsonProperty("attributes") var attrs: ChapterAttributes,
    )

    data class ChapterAttributes(
            @JsonProperty("title") var title: String? = null,
            @JsonProperty("volume") var volume: String? = null,
            @JsonProperty("chapter") var chapter: String? = null,
    )

    data class ChapterPagesResponse(
            @JsonProperty("result") var result: String,
            @JsonProperty("baseUrl") var baseUrl: String,
            @JsonProperty("chapter") var chapter: ChapterImages,
    )

    data class ChapterImages(
            @JsonProperty("hash") var hash: String,
            @JsonProperty("data") var data: List<String>,
            @JsonProperty("dataSaver") var dataSaver: List<String>,
    )
}
