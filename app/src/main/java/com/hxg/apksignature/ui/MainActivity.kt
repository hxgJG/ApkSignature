package com.hxg.apksignature.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hxg.apksignature.R
import com.hxg.apksignature.util.PackageUtils
import com.hxg.apksignature.util.SystemServiceUtils.copyToClipboard
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var mPkgNameEdt: MaterialAutoCompleteTextView? = null
    var appIcon: ImageView? = null
    var appName: TextView? = null
    var packageName: TextView? = null
    var appMd5: TextView? = null
    var appSha1: TextView? = null
    var appSha1WithoutMarks: TextView? = null
    var appVersion: TextView? = null
    private var mUpperCase = true
    private var mPkgInfoList: MutableList<PackageInfo>? = null
    private var appInfoList: MutableList<ApplicationInfo>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        val pkgNameEdt = findViewById<MaterialAutoCompleteTextView>(R.id.pkg_name_editText)
        mPkgNameEdt = pkgNameEdt

        appIcon = findViewById(R.id.app_icon)
        appName = findViewById(R.id.app_name)
        packageName = findViewById(R.id.package_name)
        appMd5 = findViewById(R.id.app_md5)
        appSha1 = findViewById(R.id.app_sha1)
        appSha1WithoutMarks = findViewById(R.id.app_sha1_without_marks)
        appVersion = findViewById(R.id.app_version)

        val arrList = ArrayList(PackageUtils.INSTANCE.getInstalledApplications(this))
        appInfoList = arrList

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
        pkgNameEdt.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
            (parent.getItemAtPosition(position) as? PackageInfo)?.let { info ->
                updatePkgInfo(info, getAppName(info.packageName))
            }
        }
        findViewById<View>(R.id.retrieve_signature_btn).setOnClickListener { onBtnClick() }
        appMd5?.setOnClickListener { v -> (v as? TextView)?.let { convertDigestCase(it) } }
        appMd5?.setOnLongClickListener { return@setOnLongClickListener copyDigest(it as TextView) }
        appSha1?.setOnClickListener { v -> (v as? TextView)?.let { convertDigestCase(it) } }
        appSha1?.setOnLongClickListener { return@setOnLongClickListener copyDigest(it as TextView) }
        appSha1WithoutMarks?.setOnClickListener { v -> (v as? TextView)?.let { convertDigestCase(it) } }
        appSha1WithoutMarks?.setOnLongClickListener { return@setOnLongClickListener copyDigest(it as TextView) }
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
        infoList.forEachIndexed { _, info ->
            if (info.packageName == pkgName) {
                pkgInfo = info
                return@forEachIndexed
            }
        }
        if (pkgInfo != null) {
            updatePkgInfo(pkgInfo!!, getAppName(pkgInfo!!.packageName))
        } else {
            Toast.makeText(this, "Invalid package name!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppName(pkgName: String): String {
        appInfoList?.let { list ->
            list.forEach { appInfo ->
                if (appInfo.packageName.equals(pkgName, true)) {
                    return packageManager.getApplicationLabel(appInfo).toString()
                }
            }
        }
        return ""
    }

    override fun onResume() {
        super.onResume()
        // update package info list
        mPkgInfoList?.run {
            clear()
            addAll(PackageUtils.INSTANCE.getInstalledPackages(this@MainActivity))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePkgInfo(pkgInfo: PackageInfo, name: String?) {
        appIcon?.setImageDrawable(pkgInfo.applicationInfo.loadIcon(packageManager))
        appName?.text = name
        appVersion?.text = "版本名：${pkgInfo.versionName}\n版本号：${pkgInfo.versionCode}"
        packageName?.text = pkgInfo.packageName
        appMd5?.text = PackageUtils.INSTANCE.getSignatureDigest(pkgInfo, PackageUtils.MD5)
        appSha1?.text = getSha1(pkgInfo)
        appSha1WithoutMarks?.text = getSha1WithoutMarks(pkgInfo)

        mPkgNameEdt?.run {
            setText(pkgInfo.packageName)
            clearFocus()
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun getSha1WithoutMarks(pkgInfo: PackageInfo) = PackageUtils.INSTANCE.getSignatureDigest(pkgInfo, PackageUtils.SHA1)

    private fun getSha1(pkgInfo: PackageInfo): String {
        val sha1 = getSha1WithoutMarks(pkgInfo)
        if (sha1.isEmpty() || sha1.length % 2 != 0) return sha1
        if (!sha1.contentEquals(":")) {
            var i = 0
            val sb = StringBuilder()
            do {
                sb.append(sha1.subSequence(i, i + 2))
                i += 2
                if (i < sha1.length) {
                    sb.append(":")
                }
            } while (i < sha1.length)
            return sb.toString()
        }
        return sha1
    }

    private fun switchCase(digest: String) =
            if (mUpperCase) digest.toUpperCase(Locale.US) else digest.toLowerCase(Locale.US)
}