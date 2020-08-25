package com.hxg.apksignature.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class PackageUtils private constructor() {
    /**
     * 获取签名的MD5摘要
     */
    fun getSignatureDigest(pkgInfo: PackageInfo, type: String): String {
        val length = pkgInfo.signatures.size
        if (length <= 0) {
            return ""
        }
        val signature = pkgInfo.signatures[0]
        var md5: MessageDigest? = null
        try {
            md5 = MessageDigest.getInstance(type)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        md5?.let {
            val digest = it.digest(signature.toByteArray())
            return toHexString(digest)
        } ?: return ""
    }

    /**
     * 将字节数组转化为对应的十六进制字符串
     */
    private fun toHexString(rawByteArray: ByteArray): String {
        val chars = CharArray(rawByteArray.size * 2)
        for (i in rawByteArray.indices) {
            val b = rawByteArray[i].toInt()
            chars[i * 2] = HEX_CHAR[b ushr 4 and 0x0F]
            chars[i * 2 + 1] = HEX_CHAR[b and 0x0F]
        }
        return String(chars)
    }

    fun getInstalledPackages(context: Context): List<PackageInfo> {
        return context.packageManager.getInstalledPackages(PackageManager.GET_SIGNATURES)
    }

    fun getInstalledApplications(context: Context): List<ApplicationInfo> {
        return context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    }

    companion object {
        val MD5 = "MD5"
        val SHA1 = "SHA1"

        @JvmStatic
        val INSTANCE = PackageUtils()
        private val HEX_CHAR = charArrayOf(
                '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        )
    }
}