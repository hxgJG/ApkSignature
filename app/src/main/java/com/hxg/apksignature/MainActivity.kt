package com.hxg.apksignature

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hxg.apksignature.SystemServiceUtils.copyToClipboard
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var mPkgNameEdt: MaterialAutoCompleteTextView? = null
    var mSignDigestTxt: TextView? = null
    var mVersionInfoLayout: GridLayout? = null
    private var mUpperCase = true
    private var mPkgInfoList: MutableList<PackageInfo>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        val pkgNameEdt = findViewById<MaterialAutoCompleteTextView>(R.id.pkg_name_editText)
        mPkgNameEdt = pkgNameEdt
        val signDigestTxt = findViewById<TextView>(R.id.signature_textView)
        mSignDigestTxt = signDigestTxt
        val versionInfoLayout = findViewById<GridLayout>(R.id.version_info_layout)
        mVersionInfoLayout = versionInfoLayout
        versionInfoLayout.visibility = View.INVISIBLE
        val arrayList = ArrayList(PackageUtils.INSTANCE.getInstalledPackages(this))
        mPkgInfoList = arrayList
        pkgNameEdt.setAdapter(object : ArrayAdapter<PackageInfo>(this, android.R.layout.simple_list_item_1, arrayList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                getItem(position)?.let { info ->
                    view.findViewById<TextView>(android.R.id.text1).text = info.packageName
                }
                return view
            }
        })
        pkgNameEdt.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val packageInfo = parent.getItemAtPosition(position) as PackageInfo
            updatePkgInfo(packageInfo)
        }
        pkgNameEdt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (TextUtils.isEmpty(s.toString())) {
                    signDigestTxt.text = ""
                    versionInfoLayout.visibility = View.INVISIBLE
                }
            }
        })
        findViewById<View>(R.id.retrieve_signature_btn).setOnClickListener { onBtnClick() }
        signDigestTxt.run {
            setOnClickListener { v -> (v as? TextView)?.let { convertDigestCase(it) } }
            setOnLongClickListener { v -> (v as? TextView)?.let { copyDigest(it) } ?: true }
        }
    }

    fun copyDigest(textView: TextView): Boolean {
        copyToClipboard(this, textView.text)
        val tip = "${textView.text}\n已经复制成功！"
        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
        return true
    }

    fun convertDigestCase(textView: TextView) {
        mUpperCase = !mUpperCase
        var digest = textView.text.toString()
        digest = switchCase(digest)
        textView.text = digest
    }

    private fun onBtnClick() {
        val editView = mPkgNameEdt ?: return
        val infoList = mPkgInfoList ?: return
        val pkgName = editView.text.toString()
        if (TextUtils.isEmpty(pkgName)) {
            Toast.makeText(this, "Package name cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        var pkgInfo: PackageInfo? = null
        for (v in infoList) {
            if (v.packageName == pkgName) {
                pkgInfo = v
                break
            }
        }
        if (pkgInfo != null) {
            updatePkgInfo(pkgInfo)
        } else {
            Toast.makeText(this, "Invalid package name!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // update package info list
        mPkgInfoList?.run {
            clear()
            addAll(PackageUtils.INSTANCE.getInstalledPackages(this@MainActivity))
        }
    }

    private fun updatePkgInfo(pkgInfo: PackageInfo) {
        var digest = PackageUtils.INSTANCE.getSignatureDigest(pkgInfo)
        digest = switchCase(digest)
        mSignDigestTxt?.text = digest
        mPkgNameEdt?.run {
            setText(pkgInfo.packageName)
            clearFocus()
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }

        mVersionInfoLayout?.run {
            (getChildAt(1) as? TextView)?.text = pkgInfo.versionCode.toString()
            (getChildAt(3) as? TextView)?.text = pkgInfo.versionName
            visibility = View.VISIBLE
        }
    }

    private fun switchCase(digest: String) =
            if (mUpperCase) digest.toUpperCase(Locale.US) else digest.toLowerCase(Locale.US)
}