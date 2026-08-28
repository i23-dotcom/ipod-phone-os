package com.ipod.phoneos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection
import java.util.Base64

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingFileChooser: WebChromeClient.FileChooserParams? = null
    private var pendingApkPicker = false
    private val FILE_PICKER = 4101
    private val PERMISSIONS = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        configureWebView()
        requestRuntimePermissions()
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = settings.userAgentString + " iPodPhoneOS/1.0"

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url) ?: super.shouldInterceptRequest(view, request)
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "http" || uri.scheme == "https") return false
                openExternal(uri.toString())
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val wanted = request.resources.filter {
                        it == PermissionRequest.RESOURCE_VIDEO_CAPTURE || it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    }.toTypedArray()
                    val cameraOk = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    val micOk = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val allowed = wanted.filter {
                        (it == PermissionRequest.RESOURCE_VIDEO_CAPTURE && cameraOk) ||
                        (it == PermissionRequest.RESOURCE_AUDIO_CAPTURE && micOk)
                    }.toTypedArray()
                    if (allowed.isEmpty()) request.deny() else request.grant(allowed)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS)
                    callback.invoke(origin, false, false)
                }
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                pendingFileChooser = fileChooserParams
                pendingApkPicker = fileChooserParams.acceptTypes.any { it.contains("apk", true) || it.contains("android.package-archive", true) }
                try {
                    startActivityForResult(fileChooserParams.createIntent(), FILE_PICKER)
                } catch (_: Exception) {
                    fileChooserCallback = null
                    pendingFileChooser = null
                    return false
                }
                return true
            }
        }

        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            saveDownload(url, userAgent, contentDisposition, mimeType)
        })
        webView.addJavascriptInterface(NativeBridge(this), "NativeBridge")
    }

    private fun requestRuntimePermissions() {
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSIONS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_PICKER) return
        val uris = if (resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(resultCode, data)
        } else null
        fileChooserCallback?.onReceiveValue(uris)
        val apk = pendingApkPicker || (uris?.firstOrNull()?.toString()?.lowercase()?.endsWith(".apk") == true)
        fileChooserCallback = null
        pendingFileChooser = null
        pendingApkPicker = false
        if (apk && !uris.isNullOrEmpty()) installApk(uris[0])
    }

    fun openExternal(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }

    fun openNativeBrowser(url: String) {
        startActivity(Intent(this, BrowserActivity::class.java).putExtra("url", url))
    }

    fun installApk(uri: Uri) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "Allow this app to install APKs, then select the APK again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivity(intent) } catch (_: Exception) { Toast.makeText(this, "Android could not open the APK", Toast.LENGTH_LONG).show() }
    }

    fun listLaunchableApps(): String {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val arr = JSONArray()
        infos.distinctBy { it.activityInfo.packageName }.sortedBy { it.loadLabel(pm).toString().lowercase() }.forEach {
            val ai = it.activityInfo
            val o = JSONObject()
            o.put("packageName", ai.packageName)
            o.put("name", ai.loadLabel(pm).toString())
            o.put("activity", ai.name)
            arr.put(o)
        }
        return arr.toString()
    }

    fun isPackageInstalled(packageName: String): Boolean = try {
        packageManager.getApplicationInfo(packageName, 0)
        true
    } catch (_: Exception) { false }

    fun launchPackage(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        startActivity(intent)
        return true
    }

    fun shareText(text: String, title: String?) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }, title ?: "Share"))
    }

    fun saveDataUrl(fileName: String, dataUrl: String): Boolean {
        return try {
            val comma = dataUrl.indexOf(',')
            if (comma < 0) return false
            val bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1))
            val mime = dataUrl.substringAfter("data:").substringBefore(';').ifBlank { "application/octet-stream" }
            val values = android.content.ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/iPod Phone OS")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) { false }
    }

    private fun saveDownload(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            val request = android.app.DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            (getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager).enqueue(request)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { Toast.makeText(this, "Download failed", Toast.LENGTH_SHORT).show() }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    class NativeBridge(private val activity: MainActivity) {
        @android.webkit.JavascriptInterface fun openExternal(url: String) = activity.runOnUiThread { activity.openExternal(url) }
        @android.webkit.JavascriptInterface fun openBrowser(url: String) = activity.runOnUiThread { activity.openNativeBrowser(url) }
        @android.webkit.JavascriptInterface fun installApk() = activity.runOnUiThread { activity.webView.evaluateJavascript("document.getElementById('apk-input')?.click()", null) }
        @android.webkit.JavascriptInterface fun getInstalledApps(): String = activity.listLaunchableApps()
        @android.webkit.JavascriptInterface fun isPackageInstalled(packageName: String): Boolean = activity.isPackageInstalled(packageName)
        @android.webkit.JavascriptInterface fun launchPackage(packageName: String): Boolean = try { activity.launchPackage(packageName) } catch (_: Exception) { false }
        @android.webkit.JavascriptInterface fun shareText(text: String, title: String?) = activity.runOnUiThread { activity.shareText(text, title) }
        @android.webkit.JavascriptInterface fun saveDataUrl(fileName: String, dataUrl: String): Boolean = activity.saveDataUrl(fileName, dataUrl)
    }
}
