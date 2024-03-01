package com.KillerDogeEmpire

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass. Use the [BlankFragment.newInstance] factory method to create an
 * instance of this fragment.
 */
class UltimaFragment(val plugin: UltimaPlugin) : BottomSheetDialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private val providers = plugin.fetchSections()

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
        val settingsLayoutId =
                plugin.resources!!.getIdentifier("settings", "layout", "com.KillerDogeEmpire")
        val settingsLayout = plugin.resources!!.getLayout(settingsLayoutId)
        val settings = inflater.inflate(settingsLayout, container, false)
        val outlineId =
                plugin.resources!!.getIdentifier("outline", "drawable", "com.KillerDogeEmpire")

        // building save button and its click listener
        val saveIconId =
                plugin.resources!!.getIdentifier("save_icon", "drawable", "com.KillerDogeEmpire")
        val saveBtn = settings.findView<ImageView>("save")
        saveBtn.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        saveBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.reload(context)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
        )

        // building toggle for extension_name_on_home and its click listener
        val extNameOnHomeBtn = settings.findView<Switch>("ext_name_on_home_toggle")
        extNameOnHomeBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        extNameOnHomeBtn.isChecked = plugin.extNameOnHome
        extNameOnHomeBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.extNameOnHome = extNameOnHomeBtn.isChecked
                    }
                }
        )

        // building list of extensions and its sections with its click listener
        val parentLayout = settings.findView<LinearLayout>("parent_list")
        val parentLayoutId =
                plugin.resources!!.getIdentifier("parent_layout", "layout", "com.KillerDogeEmpire")
        providers.forEach { provider ->
            val parentLayoutView =
                    buildExtensionView(provider, parentLayoutId, outlineId, inflater, container)
            parentLayout.addView(parentLayoutView)
        }

        return settings
    }

    fun buildExtensionView(
            provider: UltimaPlugin.PluginInfo,
            parentLayoutId: Int,
            outlineId: Int,
            inflater: LayoutInflater,
            container: ViewGroup?
    ): View {

        // collecting required resources
        val parentElementLayout = plugin.resources!!.getLayout(parentLayoutId)
        val parentLayoutView = inflater.inflate(parentElementLayout, container, false)
        val parentTextViewBtn = parentLayoutView.findView<TextView>("parent_textview")
        val childList = parentLayoutView.findView<LinearLayout>("child_list")
        val childLayoutId =
                plugin.resources!!.getIdentifier("child_checkbox", "layout", "com.KillerDogeEmpire")

        // building extension textview and its click listener
        parentTextViewBtn.text = "▶ " + provider.name
        parentTextViewBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        parentTextViewBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        if (childList.visibility == View.VISIBLE) {
                            childList.visibility = View.GONE
                            parentTextViewBtn.text = "▶ " + provider.name
                        } else {
                            childList.visibility = View.VISIBLE
                            parentTextViewBtn.text = "▼ " + provider.name
                        }
                    }
                }
        )

        // building list of sections of current extnesion with its click listener
        provider.sections?.forEach { section ->
            val newSectionView =
                    buildSectionView(section, childLayoutId, outlineId, inflater, container)
            childList.addView(newSectionView)
        }
        return parentLayoutView
    }

    fun buildSectionView(
            section: UltimaPlugin.SectionInfo,
            childLayoutId: Int,
            outlineId: Int,
            inflater: LayoutInflater,
            container: ViewGroup?
    ): View {

        // collecting required resources
        val ChildElementLayout = plugin.resources!!.getLayout(childLayoutId)
        val sectionView = inflater.inflate(ChildElementLayout, container, false)
        val childCheckBoxBtn = sectionView.findView<CheckBox>("child_checkbox")
        val counterLayout = sectionView.findView<LinearLayout>("counter_layout")

        // building section checkbox and its click listener
        childCheckBoxBtn.text = section.name
        childCheckBoxBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        childCheckBoxBtn.isChecked = section.enabled
        childCheckBoxBtn.setOnCheckedChangeListener(
                object : OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                        section.enabled = isChecked
                        plugin.currentSections = providers
                        counterLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
        )

        // configure priority counter next to the section
        configureCounterView(section, counterLayout, outlineId)

        return sectionView
    }

    fun configureCounterView(
            section: UltimaPlugin.SectionInfo,
            counterLayout: LinearLayout,
            outlineId: Int
    ) {

        // collecting required resources
        val decreasePriorityBtn = counterLayout.findView<TextView>("decrease")
        val priorityTextview = counterLayout.findView<TextView>("priority_count")
        val increasePriorityBtn = counterLayout.findView<TextView>("increase")

        // counter visible only if section enabled
        counterLayout.visibility = if (section.enabled) View.VISIBLE else View.GONE
        priorityTextview.text = section.priority.toString()

        // configuring click listener for decrease button
        decreasePriorityBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        decreasePriorityBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        val count = priorityTextview.text.toString().toInt()
                        if (count > 1) {
                            section.priority -= 1
                            plugin.currentSections = providers
                            priorityTextview.text = (count - 1).toString()
                        }
                    }
                }
        )

        // configuring click listener for increase button
        increasePriorityBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        increasePriorityBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        val count = priorityTextview.text.toString().toInt()
                        if (count < 50) {
                            section.priority += 1
                            plugin.currentSections = providers
                            priorityTextview.text = (count + 1).toString()
                        }
                    }
                }
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}
}
