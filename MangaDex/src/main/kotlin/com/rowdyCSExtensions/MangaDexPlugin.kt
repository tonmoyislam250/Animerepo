package com.RowdyAvocado

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class MangaDexPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    var dataSaver: Boolean
        get() = getKey("MANGADEX_DATA_SAVER") ?: false
        set(value) {
            setKey("MANGADEX_DATA_SAVER", value)
        }

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

        openSettings = {
            val frag = MangaDexSettings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
    }

    fun reload(context: Context?) {
        val pluginData =
                PluginManager.getPluginsOnline().find { it.internalName.contains("MangaDex") }!!
        PluginManager.unloadPlugin(pluginData.filePath)
        PluginManager.loadAllOnlinePlugins(context!!)
        afterPluginsLoadedEvent.invoke(true)
    }

    fun openFragment(imageUrlList: List<String>) {
        val frag = MangaDexFragment(this, imageUrlList)
        frag.show(activity!!.supportFragmentManager, "")
    }
}
