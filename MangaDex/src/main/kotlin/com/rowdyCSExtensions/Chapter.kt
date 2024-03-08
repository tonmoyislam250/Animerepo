package com.RowdyAvocado

// import android.util.Log
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MangaDexChapterFragment(
        val plugin: MangaDexPlugin,
        val chapterName: String,
        val chapterPages: ChapterPagesResponse
) : BottomSheetDialogFragment() {

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun getDrawable(name: String): Drawable? {
        val id =
                plugin.resources!!.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    private fun getString(name: String): String? {
        val id = plugin.resources!!.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // collecting required resources
        val chapterLayoutId =
                plugin.resources!!.getIdentifier("chapter", "layout", "com.RowdyAvocado")
        val chapterLayout = plugin.resources!!.getLayout(chapterLayoutId)
        val chapterView = inflater.inflate(chapterLayout, container, false)
        val pageLayoutId = plugin.resources!!.getIdentifier("page", "layout", "com.RowdyAvocado")
        // val loadingImg =
        //         plugin.resources!!.getIdentifier("save_icon", "drawable", "com.RowdyAvocado")

        val chapterTitleTextView = chapterView.findView<TextView>("title")
        chapterTitleTextView.text = chapterName
        val pageListLayout = chapterView.findView<LinearLayout>("page_list")
        val prefix = "${chapterPages.baseUrl}/#/${chapterPages.chapter.hash}/"
        val images: Pair<String, List<String>> =
                if (plugin.dataSaver)
                        Pair(
                                "data-saver",
                                chapterPages.chapter.dataSaver,
                        )
                else
                        Pair(
                                "data",
                                chapterPages.chapter.data,
                        )

        images.second.forEach { image ->
            val url = prefix.replace("#", images.first) + image
            val pageLayout = plugin.resources!!.getLayout(pageLayoutId)
            val pageLayoutView = inflater.inflate(pageLayout, container, false)
            val pageImageView = pageLayoutView.findView<ImageView>("page")
            Glide.with(this)
                    .load(url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(Target.SIZE_ORIGINAL)
                    .into(pageImageView)
            pageListLayout.addView(pageLayoutView)
        }
        return chapterView
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}
}
