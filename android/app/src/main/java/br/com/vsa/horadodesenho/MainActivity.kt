package br.com.vsa.horadodesenho

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Hora do Desenho para Android / Android TV.
 *
 * A interface inteira é o index.html do repositório, servido ao WebView por
 * https://appassets.androidplatform.net/ (origem https de verdade, exigida pelo
 * player do YouTube). Os dados ficam no localStorage do WebView, como no navegador.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var customView: View? = null
    private lateinit var root: FrameLayout
    private lateinit var chrome: WebChromeClient

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web)
        setContentView(root)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            userAgentString = "$userAgentString HoraDoDesenho/${BuildConfig.VERSION_NAME}"
        }
        web.isFocusable = true
        web.isFocusableInTouchMode = true
        web.setBackgroundColor(0xFFF4F6FB.toInt())

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }

        // Tela cheia pedida pelo player (quando o modo livre usa os controles do YouTube)
        chrome = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView?.let { root.removeView(it) }
                customView = view
                root.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                web.visibility = View.GONE
            }
            override fun onHideCustomView() {
                customView?.let { root.removeView(it) }
                customView = null
                web.visibility = View.VISIBLE
            }
        }
        web.webChromeClient = chrome

        web.addJavascriptInterface(Bridge(), "HoraDoDesenho")

        // Por padrão abre a interface embutida no APK. Para apontar para outra versão
        // (ex.: GitHub Pages) sem reinstalar:
        //   adb shell am start -n br.com.vsa.horadodesenho/.MainActivity --es url https://SEU-USUARIO.github.io/grade-tv/
        intent?.getStringExtra("url")?.let { salvarUrl(it) }
        web.loadUrl(urlAtual())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra("url")?.let { salvarUrl(it); web.loadUrl(it) }
    }

    private fun urlAtual(): String =
        getSharedPreferences("horadodesenho", MODE_PRIVATE).getString("url", null)
            ?: "https://appassets.androidplatform.net/assets/index.html"

    private fun salvarUrl(url: String) {
        getSharedPreferences("horadodesenho", MODE_PRIVATE).edit().putString("url", url).apply()
    }

    override fun onResume() {
        super.onResume()
        esconderBarras()
        web.onResume()
        web.requestFocus()
    }

    override fun onPause() {
        web.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) esconderBarras()
    }

    @Suppress("DEPRECATION")
    private fun esconderBarras() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    /** O botão VOLTAR do controle vira Esc na interface; o app nunca fecha por ele. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customView != null) {
            chrome.onHideCustomView()
            return
        }
        enviarTecla("Escape")
    }

    /** O botão MENU do controle abre a área dos pais (tecla A na interface). */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            enviarTecla("a")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun enviarTecla(key: String) {
        web.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{key:'$key',bubbles:true}))", null)
    }

    /** Pequena ponte lida pelo index.html: window.HoraDoDesenho */
    inner class Bridge {
        @JavascriptInterface fun version(): String = BuildConfig.VERSION_NAME
        @JavascriptInterface fun isTv(): Boolean =
            packageManager.hasSystemFeature("android.software.leanback")
        @JavascriptInterface fun sdk(): Int = Build.VERSION.SDK_INT
    }
}
