package com.hxg.apksignature

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils

/**
 * System service utils
 */
object SystemServiceUtils {
    /**
     * Copy text to system clipboard
     *
     * @param context [Context]
     * @param text    text will be copied.
     */
    @JvmStatic
    fun copyToClipboard(context: Context, text: CharSequence?) {
        if (TextUtils.isEmpty(text)) {
            return
        }
        val item = ClipData.Item(text)
        val clipData = ClipData("", arrayOf("text/plain"), item)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager ?: return
        clipboardManager.setPrimaryClip(clipData)
    }
}