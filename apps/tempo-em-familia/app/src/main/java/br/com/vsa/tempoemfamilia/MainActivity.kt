package br.com.vsa.tempoemfamilia

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Tempo em Família — bloqueia este celular nos horários marcados, com a foto da família.
 * Interface em assets/index.html; o Kotlin cuida de alarmes, fixação (lock task), foto e dados.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private val gravador = Executors.newSingleThreadExecutor()
    private val pastaFoto get() = File(filesDir, "foto").apply { mkdirs() }
    private val arquivoDados get() = File(filesDir, "dados.json")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        web = WebView(this)
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
            useWideViewPort = true; loadWithOverviewMode = true; setSupportZoom(false)
        }
        web.setBackgroundColor(0xFF101418.toInt())
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/foto/", WebViewAssetLoader.InternalStoragePathHandler(this, pastaFoto))
            .build()
        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.addJavascriptInterface(Ponte(), "Familia")
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web.evaluateJavascript("if(window.verificar) verificar()", null)
    }

    override fun onResume() {
        super.onResume()
        esconderBarras()
        web.evaluateJavascript("if(window.verificar) verificar()", null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) esconderBarras() }

    @Suppress("DEPRECATION")
    private fun esconderBarras() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        web.evaluateJavascript("document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))", null)
    }

    // ----- fixação -----
    private fun travar() { try { startLockTask() } catch (_: Exception) {} }
    private fun destravar() { try { stopLockTask() } catch (_: Exception) {} }
    private fun travado(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    // ----- foto -----
    private fun escolherFotos() {
        val i = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        try { startActivityForResult(Intent.createChooser(i, "Escolher fotos da família"), 7) } catch (_: Exception) {}
    }

    private fun listarFotos(): List<String> =
        (pastaFoto.listFiles { f -> f.name.endsWith(".jpg") }?.map { it.name } ?: emptyList()).sorted()

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 7 || resultCode != RESULT_OK || data == null) return
        val uris = ArrayList<Uri>()
        data.clipData?.let { cd -> for (k in 0 until cd.itemCount) uris.add(cd.getItemAt(k).uri) }
        data.data?.let { if (uris.isEmpty()) uris.add(it) }
        if (uris.isEmpty()) return
        gravador.execute {
            for (uri in uris) try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                var amostra = 1
                while (opts.outWidth / amostra > 2400 || opts.outHeight / amostra > 2400) amostra *= 2
                val bmp = contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = amostra })
                } ?: continue
                val nome = "f" + System.currentTimeMillis() + "_" + (100 + (Math.random() * 900).toInt()) + ".jpg"
                FileOutputStream(File(pastaFoto, nome)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 86, it) }
                bmp.recycle()
            } catch (_: Exception) {}
            runOnUiThread { web.evaluateJavascript("if(window.fotosAtualizadas) fotosAtualizadas()", null) }
        }
    }

    // ----- permissões -----
    private fun temSobreposicao(): Boolean = Settings.canDrawOverlays(this)
    private fun pedirSobreposicao() {
        try { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
    }
    private fun temAlarmeExato(): Boolean =
        Build.VERSION.SDK_INT < 31 || (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    private fun pedirAlarmeExato() {
        if (Build.VERSION.SDK_INT >= 31) try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
    }

    inner class Ponte {
        @JavascriptInterface fun versao(): String = BuildConfig.VERSION_NAME
        @JavascriptInterface fun salvarDados(json: String) { gravador.execute { try { arquivoDados.writeText(json) } catch (_: Exception) {} } }
        @JavascriptInterface fun carregarDados(): String = try { if (arquivoDados.exists()) arquivoDados.readText() else "" } catch (_: Exception) { "" }
        @JavascriptInterface fun agendar(json: String) = Agenda.salvar(this@MainActivity, json)
        @JavascriptInterface fun travar(ativo: Boolean) { runOnUiThread { if (ativo) this@MainActivity.travar() else destravar() } }
        @JavascriptInterface fun travado(): Boolean = this@MainActivity.travado()
        @JavascriptInterface fun escolherFotos() { runOnUiThread { this@MainActivity.escolherFotos() } }
        @JavascriptInterface fun listarFotos(): String = org.json.JSONArray(this@MainActivity.listarFotos()).toString()
        @JavascriptInterface fun removerFoto(nome: String): Boolean = try { File(pastaFoto, nome.substringAfterLast('/')).delete() } catch (_: Exception) { false }
        @JavascriptInterface fun temSobreposicao(): Boolean = this@MainActivity.temSobreposicao()
        @JavascriptInterface fun pedirSobreposicao() { runOnUiThread { this@MainActivity.pedirSobreposicao() } }
        @JavascriptInterface fun temAlarmeExato(): Boolean = this@MainActivity.temAlarmeExato()
        @JavascriptInterface fun pedirAlarmeExato() { runOnUiThread { this@MainActivity.pedirAlarmeExato() } }
        @JavascriptInterface fun irParaInicio() { runOnUiThread { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
