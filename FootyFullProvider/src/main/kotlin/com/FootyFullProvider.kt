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
import okhttp3.RequestBody

//.\gradlew.bat PelisPlus4KProvider:deployWithAdb
//adb logcat | find "TAG"
//com.lagradost.cloudstream3.prerelease

class FootyFullProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://footyfull.com/"
    override var name = "Footy Full"

    //override val hasMainPage = true
    override val hasMainPage = true

    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    //val reqlink = "https://librivox.org/api/feed/audiobooks/title/%5EAr?format=json"
    //val jason = app.get(reqlink)?.parsed<BookList>()
    //val log = jason?.books



    //override val mainPage = mainPageOf(
    //    "/search?primary_key=0&search_category=title&search_page=" to "Latest Audiobooks",
    //    )

    override val mainPage = mainPageOf(
        "/world-cup/" to "FIFA World Cup 2022",
        "/premier-league/" to "Premier League",
        "/la-liga/" to "La Liga",
        "/uefa-champions-league/" to "Champions League",
        "/shows/" to "Shows"
        )
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }


    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("div.entry-image a")?.attr("href") ?: return null)
        //val href = "https://www.google.com"
        val title = this.selectFirst("div.entry-header h2 a")?.text() ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.entry-image a img")?.attr("src"))

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        //return document.select("div.post-filter-image").mapNotNull {
        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        
        val poster = document.selectFirst("div.book-page-book-cover img")?.attr("src") ?: "null"

        
        
        val payloadcode = document.selectFirst("a.vlog-cover")?.attr("data-id") ?: "null"
        //Log.d("TAG","payloadcode $payloadcode")
        val payload = "action=vlog_format_content&format=video&display_playlist=true&id=$payloadcode"
        val body = RequestBody.create(null, payload)

        val req = app.post("https://footyfull.com/wp-admin/admin-ajax.php",
        requestBody=body,
        headers = mapOf(
            """authority""" to """footyfull.com""",
            """accept""" to """*/*""",
            //"""accept-language""" to """en-US,en;q=0.9""",
            """cache-control""" to """no-cache""",
            """content-type""" to """application/x-www-form-urlencoded; charset=UTF-8""",
            """origin""" to """https://footyfull.com""",
            """pragma""" to """no-cache""",
            """referer""" to url,
            //"""sec-ch-ua""" to """ "Not_A Brand";v="99", "Microsoft Edge";v="109", "Chromium";v="109" """,
            //"""sec-ch-ua-mobile""" to """?0""",
            //"""sec-ch-ua-platform""" to """ "Windows" """,
            //"""sec-fetch-dest""" to """empty""",
            //"""sec-fetch-mode""" to """cors""",
            //"""sec-fetch-site""" to """same-origin""",
            //"""user-agent""" to """Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36 Edg/109.0.1518.55""",
            """x-requested-with""" to """XMLHttpRequest"""
        )
        )

        val text = req?.document?.select("script")?.html()?.toString() ?: "Null"

        //var sbtext = Regex("""https:\/\/sbchill.com\/e\/.*?html""").findAll(text)// ?: "Null"
        //sbtext = sbtext.replace("sbchill","watchsb")

        //var okrutext = Regex("""https:\/\/ok.ru\/videoembed\/.*?\"""").findAll(text)// ?: "Null"

        //var streamtapetext = Regex("""https:\/\/streamtape.com\/e\/.*?\"""").findAll(text)// ?: "Null"
        
        //okrutext = okrutext.replace("\""," ")

        val tvType = TvType.TvSeries
        
        return if (tvType == TvType.TvSeries) {

            var episodes = mutableListOf<Episode>()
            var streamlist = mutableListOf<String>()

            val streams = Regex("""var linksourcesF = \[(.*)\]""").find(text)?.
                    value?.replace("var linksourcesF = [","")?.replace("]","")
            
            Regex("""\"([^\"]+)\"""")?.findAll(streams!!)?.forEach{
                streamlist.add(it?.value!!)
            }

            var c = 0

            val eptext = req?.document?.select("div.video-text")?.mapNotNull{
                val nameep = it?.text() ?: "null"
                val linkep = streamlist.get(c)
                episodes.add(Episode(linkep, nameep))

                c = c+1
            }
            //First Half by Okru, Second Half by Okru

            // sbtext?.forEach(){ 
            //     href = it.value.replace("sbchill","watchsb")
            //     //val name = it?.text()?.trim() ?: return null

            //     episodes.add(Episode(
            //        href,
            //        //name,
            //    ))
            // }

            // okrutext?.forEach(){
            //     href = it.value.replace("\""," ")
            //     //val name = it?.text()?.trim() ?: return null

            //     episodes.add(Episode(
            //        href,
            //        //name,
            //    ))
            // }
            

             
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster.toString()
                //this.recommendations = recommendations
            }
        } 
        
        
        
        else {
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



        Log.d("TAG", "data $data")

        val output = data?.replace("\"","")?.replace("sblongvu","watchsb")
        ?.replace("sbbrisk","watchsb")

        Log.d("TAG", "output $output")

        loadExtractor(output!!, subtitleCallback, callback)

        return true
    }


}

