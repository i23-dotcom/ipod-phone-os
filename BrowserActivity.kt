package com.ipod.phoneos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class BrowserActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10,14,20)) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,8); gravity = Gravity.CENTER_VERTICAL }
        fun b(text: String, action: () -> Unit) = Button(this).apply { this.text=text; setOnClickListener { action() }; minWidth=0; minimumWidth=0 }
        bar.addView(b("‹") { if (web.canGoBack()) web.goBack() }, LinearLayout.LayoutParams(52,56))
        bar.addView(b("›") { if (web.canGoForward()) web.goForward() }, LinearLayout.LayoutParams(52,56))
        bar.addView(b("↻") { web.reload() }, LinearLayout.LayoutParams(52,56))
        address = EditText(this).apply { hint="Search or enter URL"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY) }
        bar.addView(address, LinearLayout.LayoutParams(0,56,1f))
        bar.addView(b("GO") { navigate() }, LinearLayout.LayoutParams(64,56))
        root.addView(bar)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        web.settings.setSupportMultipleWindows(false)
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return request.url.scheme !in listOf("http","https")
            }
            override fun onPageFinished(view: WebView, url: String) { address.setText(url) }
        }
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
        val status = TextView(this).apply { text="iPod Native Browser"; setTextColor(Color.LTGRAY); setPadding(10,6,10,6) }
        root.addView(status)
        setContentView(root)
        val initial = intent.getStringExtra("url") ?: "https://www.google.com/"
        web.loadUrl(initial); address.setText(initial)
        address.setOnEditorActionListener { _,_,_-> navigate(); true }
    }

    private fun navigate() {
        var value = address.text.toString().trim()
        if (value.isBlank()) return
        if (!value.matches(Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*"))) {
            if (value.contains('.') && !value.contains(' ')) value = "https://$value"
            else value = "https://www.google.com/search?q=" + Uri.encode(value)
        }
        web.loadUrl(value)
    }
}
