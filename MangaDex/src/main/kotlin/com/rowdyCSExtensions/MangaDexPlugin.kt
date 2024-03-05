package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MangaDexPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    companion object {
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post(
                    object : Runnable {
                        override fun run() {
                            function()
                        }
                    }
            )
        }
    }

    override fun load(context: Context) {
        activity = context as AppCompatActivity

        // All providers should be added in this manner
        registerMainAPI(MangaDex(this))

        // openSettings = {
        //     val frag = MangaDexSettings(this)
        //     frag.show(activity!!.supportFragmentManager, "")
        // }
    }

    fun openFragment(imageUrlList: List<String>) {
        val frag = MangaFragment(this, imageUrlList)
        frag.show(activity!!.supportFragmentManager, "")
    }
}
