package com.KillerDogeEmpire

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.Session
import kotlinx.coroutines.delay
import org.jsoup.nodes.Element
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
//.\gradlew.bat PelisPlus4KProvider:deployWithAdb
//adb logcat | find "TAG"
//com.lagradost.cloudstream3.prerelease
class WatchWrestlingProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://watchwrestling.ai"
    override var name = "Watch Wrestling"
    //override val hasMainPage = true
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
            TvType.Movie,
            TvType.TvSeries
    )


    override val mainPage = mainPageOf(
            "/sports/wwe-29-17/" to "Latest WWE Shows",
            "/sports/raw-39/" to "Latest RAW WWE Shows",
            "/sports/smackdown-30/" to "Latest Smackdown WWE Shows",
            "/sports/nxt-20/" to "Latest NXT WWE Shows",
            "/sports/aew-14/" to "Latest AEW Shows",
            "/sports/ufc-17/" to "Latest UFC Shows",
            "/sports/boxing-18/" to "Latest Boxing Shows",
            "/sports/ppv-21/" to "Latest PPV Wrestling Shows",
            "/sports/tna-impact-20/" to "Latest Impact Wrestling Shows",
            "/sports/njpw-15/" to "Latest NJPW Shows",
            "/sports/mma-11/" to "Latest MMA Shows",
            "/sports/gcw-7/" to "Latest GCW Shows",
            "/sports/roh-13/" to "Latest ROH Shows",
            "/sports/total-divas-15/" to "Latest Total Divas Shows",
            "/sports/main-event-14/" to "Latest Main Event Shows",
            "/sports/other-sport-13/" to "Other Sports",
    )


    //taken from hexated, I have no idea wtf I am doing

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("div.nag.cf div.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }


    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("div.thumb a")?.attr("href") ?: return null)
        //val href = "https://www.google.com"
        val title = this.selectFirst("div.data h2")?.text() ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.thumb a span.clip img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        //return document.select("div.post-filter-image").mapNotNull {
        return document.select("div.nag.cf div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {

        val document = app.get(url).document

        //Log.d("TAG", "HELLOOOO")

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null

        val poster = document.selectFirst("img[decoding]")?.attr("src") ?: "https://watchwrestling.bz/wp-content/uploads/2022/11/wwbzlogo2022.png"

        val tvType = TvType.TvSeries

        return if (tvType == TvType.TvSeries) {
            var episodes = mutableListOf<Episode>()

            document.select("span:contains(DAILYMOTION)").mapNotNull {

                val next = it?.parent()?.nextElementSibling()?.children()?.mapNotNull{
                    val name = it?.text()?.trim() ?: "null"
                    val href = fixUrl(it?.attr("href") ?: "null")

                    episodes.add(Episode(href,name))
                }
            }

            document.select("a:contains(DAILYMOTION)").mapNotNull {

                val name = it?.text()?.trim() ?: "null"
                val href = fixUrl(it?.attr("href") ?: "null")

                episodes.add(Episode(href,name))

            }


            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster.toString()
                //this.recommendations = recommendations
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster.toString()
                //this.recommendations = recommendations
            }
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {

        val num = Regex("[^mainid=]*$").find(data)?.value // -> 780700

        val vidcode = Regex("(?<=mirror=)(.*?)(?=\\%)").find(data)?.value

        // https://files.m2list.com/ajax/movie/get_sources/315345/dlp1
        val relink = "https://files.m2list.com/ajax/movie/get_sources/$num/$vidcode"
        val jason = app.get(relink)?.parsed<Epi>()

        var code = "${jason?.movie?.link}"
        //https://www.dailymotion.com/video/k3JAHfletwk94ayCVIu

        var output = "https://www.dailymotion.com/embed/video/$code"
        Log.d("TAG","$output")

        loadExtractor(output, subtitleCallback, callback)

        return true
    }

    data class Epi (
            @JsonProperty("movie") val movie:  Movie? = null,
            @JsonProperty("sources") val sources:  String? = null,
            @JsonProperty("tracks") val tracks:  String? = null,
            @JsonProperty("settings") val settings:  Settings? = null
    )

    data class Movie (
            @JsonProperty("link") val link: String
    )

    data class Settings (
            @JsonProperty("id") val id: Long,
            @JsonProperty("embed_title") val embedTitle: String,
            @JsonProperty("embed_title_status") val embedTitleStatus: Long,
            @JsonProperty("jw_version") val jwVersion: String,
            @JsonProperty("jw_key") val jwKey: String,
            @JsonProperty("jw_link") val jwLink: String,
            @JsonProperty("logo_link") val logoLink: Any? = null,
            @JsonProperty("logo_position") val logoPosition: String,
            @JsonProperty("logo_homepage") val logoHomepage: Any? = null,
            @JsonProperty("logo_status") val logoStatus: Long,
            @JsonProperty("poster_link") val posterLink: Any? = null,
            @JsonProperty("poster_status") val posterStatus: Long,
            @JsonProperty("popunder_ads_link") val popunderAdsLink: Any? = null,
            @JsonProperty("popunder_ads_status") val popunderAdsStatus: Long,
            @JsonProperty("vast_ads_link") val vastAdsLink: Any? = null,
            @JsonProperty("vast_ads_status") val vastAdsStatus: Long,
            @JsonProperty("vast_ads_skippable") val vastAdsSkippable: Long,
            @JsonProperty("banner_ads_code") val bannerAdsCode: Any? = null,
            @JsonProperty("banner_ads_status") val bannerAdsStatus: Long,
            @JsonProperty("text_color") val textColor: String,
            @JsonProperty("font_size") val fontSize: String,
            @JsonProperty("font_family") val fontFamily: String,
            @JsonProperty("background_color") val backgroundColor: String,
            @JsonProperty("background_opacity") val backgroundOpacity: String,
            @JsonProperty("edge_style") val edgeStyle: String,
            @JsonProperty("window_color") val windowColor: String,
            @JsonProperty("windows_opacity") val windowsOpacity: String,
            @JsonProperty("video_autostart") val videoAutostart: String,
            @JsonProperty("share_button") val shareButton: String,
            @JsonProperty("download_button") val downloadButton: String,
            @JsonProperty("auto_resume") val autoResume: String,
            @JsonProperty("created_at") val createdAt: Any? = null,
            @JsonProperty("updated_at") val updatedAt: String
    )

}