package com.KillerDogeEmpire

import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.allProviders
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.MainActivity.Companion.afterPluginsLoadedEvent
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.PluginManager

@CloudstreamPlugin
class UltimaPlugin : Plugin() {
    var activity: AppCompatActivity? = null

    var currentSections: Array<PluginInfo>
        get() = getKey("ULTIMA_PROVIDER_LIST") ?: emptyArray<PluginInfo>()
        set(value) {
            setKey("ULTIMA_PROVIDER_LIST", value)
        }

    var extNameOnHome: Boolean
        get() = getKey("ULTIMA_EXT_NAME_ON_HOME") ?: true
        set(value) {
            setKey("ULTIMA_EXT_NAME_ON_HOME", value)
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
        registerMainAPI(Ultima(this))

        openSettings = {
            val frag = UltimaSettings(this)
            frag.show(activity!!.supportFragmentManager, "")
        }
    }

    fun reload(context: Context?) {
        val pluginData =
                PluginManager.getPluginsOnline().find { it.internalName.contains("Ultima") }
        if (pluginData == null) {
            PluginManager.hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager.loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
    }

    fun fetchSections(): Array<PluginInfo> {
        synchronized(allProviders) {
            var providers = allProviders
            var newProviderList = emptyArray<PluginInfo>()

            providers.forEach { provider ->
                if (!provider.name.equals("Ultima")) {
                    val doesProviderExist =
                            getKey<Array<PluginInfo>>("ULTIMA_PROVIDER_LIST")?.find {
                                it.name == provider.name
                            }
                    if (doesProviderExist == null) {
                        var mainPageList = emptyArray<SectionInfo>()
                        provider.mainPage.forEach { section ->
                            var sectionData =
                                    SectionInfo(section.name, section.data, provider.name, false)
                            mainPageList += sectionData
                        }
                        var providerData = PluginInfo(provider.name, mainPageList)
                        newProviderList += providerData
                    } else {
                        newProviderList += doesProviderExist
                    }
                }
            }

            if (newProviderList.size == providers.size) {
                return newProviderList
            } else {
                return newProviderList
                        .filter { new -> providers.find { new.name == it.name } != null }
                        .toTypedArray()
            }
        }
    }

    data class SectionInfo(
            @JsonProperty("name") var name: String? = null,
            @JsonProperty("url") var url: String? = null,
            @JsonProperty("pluginName") var pluginName: String? = null,
            @JsonProperty("enabled") var enabled: Boolean = false,
            @JsonProperty("priority") var priority: Int = 1
    )

    data class PluginInfo(
            @JsonProperty("name") var name: String? = null,
            @JsonProperty("sections") var sections: Array<SectionInfo>? = null
    )
}
