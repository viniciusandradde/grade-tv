package br.com.vsa.horadodesenho

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

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
            databaseEnabled = true
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

        // Na primeira abertura: TV vira tela inicial; celular e tablet ficam como app comum.
        val prefs = getSharedPreferences("horadodesenho", MODE_PRIVATE)
        if (!prefs.contains("launcher")) {
            val tv = ehTv()
            definirLauncher(tv)
            prefs.edit().putBoolean("launcher", tv).apply()
        }

        web.loadUrl(urlAtual())
    }

    private fun ehTv(): Boolean = packageManager.hasSystemFeature("android.software.leanback")

    private val aliasInicial get() = ComponentName(this, "$packageName.Inicial")

    private fun definirLauncher(ativo: Boolean) {
        packageManager.setComponentEnabledSetting(
            aliasInicial,
            if (ativo) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        getSharedPreferences("horadodesenho", MODE_PRIVATE).edit().putBoolean("launcher", ativo).apply()
    }

    private fun launcherAtivo(): Boolean =
        packageManager.getComponentEnabledSetting(aliasInicial) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    // ----- Modo criança (app fixado: Home e Recentes bloqueados até os pais liberarem) -----
    private fun travar() {
        if (ehTv()) return
        try { startLockTask() } catch (_: Exception) {}
    }
    private fun destravar() { try { stopLockTask() } catch (_: Exception) {} }
    @Suppress("DEPRECATION")
    private fun travado(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= 23) am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE else am.isInLockTaskMode
    }

    // ----- Dados: cópia em arquivo, independente do localStorage do WebView -----
    private val gravador = Executors.newSingleThreadExecutor()
    private val arquivoDados get() = File(filesDir, "dados.json")
    private fun salvarDados(json: String) {
        gravador.execute {
            try {
                val tmp = File(filesDir, "dados.json.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(arquivoDados)) { arquivoDados.writeText(json); tmp.delete() }
            } catch (_: Exception) {}
        }
    }
    private fun carregarDados(): String = try { if (arquivoDados.exists()) arquivoDados.readText() else "" } catch (_: Exception) { "" }

    // ----- Agregador de aplicativos infantis -----
    private val appsPrefs get() = getSharedPreferences("crescer_apps", MODE_PRIVATE)
    private val adminComponent get() = ComponentName(this, VsaDeviceAdminReceiver::class.java)

    private fun uiDeConfianca(): Boolean {
        val atual = web.url ?: return false
        return atual.startsWith("https://appassets.androidplatform.net/assets/") || atual.startsWith("https://viniciusandradde.github.io/grade-tv/")
    }

    private fun ehDeviceOwner(): Boolean = try {
        (getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager).isDeviceOwnerApp(packageName)
    } catch (_: Exception) { false }

    private val pacotesBloqueados = setOf(
        "com.android.settings", "com.android.systemui", "com.android.packageinstaller",
        "com.google.android.packageinstaller", "com.google.android.permissioncontroller",
        "com.android.permissioncontroller", "com.google.android.apps.nexuslauncher",
        "com.android.vending", "com.google.android.vending", "com.android.chrome",
        "com.google.android.chrome", "com.google.android.apps.chrome", "org.mozilla.firefox",
        "com.microsoft.emmx", "com.opera.browser", "com.google.android.youtube",
        "com.google.android.youtube.tv", "com.android.browser"
    )

    private fun pacoteBloqueado(pkg: String): Boolean =
        pacotesBloqueados.any { pkg == it || pkg.startsWith("$it.") }

    private fun appsDeInicializacao(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            packageManager.queryIntentActivities(intent, 0)
                .filter { it.activityInfo?.packageName != packageName && !pacoteBloqueado(it.activityInfo.packageName) }
                .distinctBy { it.activityInfo.packageName }
                .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        } catch (_: Exception) { emptyList() }
    }

    private fun appPodeAbrir(pkg: String): Boolean =
        pkg.isNotBlank() && pkg != packageName && !pacoteBloqueado(pkg) &&
            packageManager.getLaunchIntentForPackage(pkg) != null

    private fun appsPermitidosNativos(): Set<String> = try {
        val json = JSONArray(appsPrefs.getString("allowed", "[]") ?: "[]")
        (0 until json.length()).mapNotNull { json.optString(it).takeIf { p -> p.isNotBlank() } }.toSet()
    } catch (_: Exception) { emptySet() }

    private fun appsAtivosNativos(): Set<String> = try {
        val json = JSONArray(appsPrefs.getString("active_allowed", "[]") ?: "[]")
        (0 until json.length()).mapNotNull { json.optString(it).takeIf { p -> p.isNotBlank() } }.toSet()
    } catch (_: Exception) { emptySet() }

    private fun sincronizarPacotesQuiosque(permitidos: Set<String>): Boolean {
        if (!ehDeviceOwner() || Build.VERSION.SDK_INT < 23) return false
        return try {
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.setLockTaskPackages(adminComponent, (permitidos + packageName).filter { appPodeAbrir(it) || it == packageName }.toTypedArray())
            true
        } catch (_: Exception) { false }
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
        @JavascriptInterface fun isTv(): Boolean = ehTv()
        @JavascriptInterface fun sdk(): Int = Build.VERSION.SDK_INT

        /** Catálogo de apps instalados que podem ser iniciados; nenhum é liberado por padrão. */
        @JavascriptInterface fun listarAppsInstalados(): String {
            if (!uiDeConfianca()) return "[]"
            val arr = JSONArray()
            appsDeInicializacao().forEach { item ->
                val info = item.activityInfo
                val obj = JSONObject()
                obj.put("packageName", info.packageName)
                obj.put("name", item.loadLabel(packageManager).toString())
                obj.put("system", (info.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
                arr.put(obj)
            }
            return arr.toString()
        }

        @JavascriptInterface fun appsPermitidos(): String {
            if (!uiDeConfianca()) return "[]"
            val arr = JSONArray()
            appsPermitidosNativos().sorted().forEach { arr.put(it) }
            return arr.toString()
        }

        /** Persiste somente pacotes instalados e com launcher. O catálogo nativo é a autoridade para abrir apps. */
        @JavascriptInterface fun definirAppsPermitidos(json: String): String {
            if (!uiDeConfianca()) return JSONObject().put("saved", false).put("reason", "Origem da interface não confiável.").toString()
            val requested = try {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { p -> appPodeAbrir(p) } }.toSet()
            } catch (_: Exception) { emptySet() }
            val saved = JSONArray()
            requested.sorted().forEach { saved.put(it) }
            appsPrefs.edit().putString("allowed", saved.toString()).apply()
            val kioskReady = sincronizarPacotesQuiosque(requested)
            return JSONObject().put("saved", true).put("deviceOwner", ehDeviceOwner()).put("kioskReady", kioskReady).toString()
        }

        /** Define a lista do perfil infantil ativo; limita tanto a abertura via ponte quanto o Lock Task. */
        @JavascriptInterface fun definirAppsAtivos(json: String): String {
            if (!uiDeConfianca()) return JSONObject().put("saved", false).put("reason", "Origem da interface não confiável.").toString()
            // Defesa nativa: um perfil infantil nunca pode ativar apps fora da allowlist
            // aprovada pelos responsáveis, mesmo se a interface enviar pacotes arbitrários.
            val parentAllowed = appsPermitidosNativos()
            val requested = try {
                val arr = JSONArray(json)
                (0 until arr.length())
                    .mapNotNull { arr.optString(it).takeIf { p -> p in parentAllowed && appPodeAbrir(p) } }
                    .toSet()
            } catch (_: Exception) { emptySet() }
            val saved = JSONArray()
            requested.sorted().forEach { saved.put(it) }
            appsPrefs.edit().putString("active_allowed", saved.toString()).apply()
            val kioskReady = sincronizarPacotesQuiosque(requested)
            return JSONObject().put("saved", true).put("deviceOwner", ehDeviceOwner()).put("kioskReady", kioskReady).toString()
        }

        @JavascriptInterface fun statusControleDispositivo(): String {
            if (!uiDeConfianca()) return JSONObject().put("deviceOwner", false).put("lockTaskAllowed", false).toString()
            val owner = ehDeviceOwner()
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return JSONObject()
                .put("deviceOwner", owner)
                .put("lockTaskAllowed", try { Build.VERSION.SDK_INT >= 23 && dpm.isLockTaskPermitted(packageName) } catch (_: Exception) { false })
                .put("sdk", Build.VERSION.SDK_INT)
                .toString()
        }

        /** Só abre pacotes liberados nativamente; no modo fixado comum, bloqueia a troca de app. */
        @JavascriptInterface fun abrirAppPermitido(pkg: String): String {
            if (!uiDeConfianca()) return JSONObject().put("ok", false).put("reason", "Origem da interface não confiável.").toString()
            if (pkg !in appsAtivosNativos() || !appPodeAbrir(pkg)) {
                return JSONObject().put("ok", false).put("reason", "Este aplicativo não está liberado pelos responsáveis ou não está instalado.").toString()
            }
            if (travado() && !ehDeviceOwner()) {
                return JSONObject().put("ok", false).put("reason", "Para trocar entre aplicativos mantendo o celular bloqueado, é necessário configurar o modo quiosque gerenciado neste dispositivo.").toString()
            }
            return try {
                if (ehDeviceOwner()) sincronizarPacotesQuiosque(appsAtivosNativos())
                val launch = packageManager.getLaunchIntentForPackage(pkg) ?: throw IllegalStateException("Aplicativo sem tela inicial.")
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                JSONObject().put("ok", true).toString()
            } catch (e: Exception) {
                JSONObject().put("ok", false).put("reason", e.message ?: "Não foi possível abrir o aplicativo.").toString()
            }
        }

        /** Tela inicial (launcher) ligada/desligada — controlado na área dos pais. */
        @JavascriptInterface fun launcherAtivo(): Boolean =
            uiDeConfianca() && this@MainActivity.launcherAtivo()

        @JavascriptInterface fun definirLauncher(ativo: Boolean) {
            if (!uiDeConfianca()) return
            this@MainActivity.definirLauncher(ativo)
        }

        /** Modo criança: fixa o app (só sai pela área dos pais). */
        @JavascriptInterface fun modoCrianca(ativo: Boolean) {
            if (!uiDeConfianca()) return
            runOnUiThread { if (ativo) travar() else destravar() }
        }

        @JavascriptInterface fun modoCriancaAtivo(): Boolean =
            uiDeConfianca() && this@MainActivity.travado()

        /** Dados persistidos em arquivo do app (além do localStorage). */
        @JavascriptInterface fun salvarDados(json: String) {
            if (!uiDeConfianca()) return
            this@MainActivity.salvarDados(json)
        }

        @JavascriptInterface fun carregarDados(): String =
            if (uiDeConfianca()) this@MainActivity.carregarDados() else ""

        /** Celular: trava em paisagem só enquanto o vídeo está aberto. */
        @JavascriptInterface fun travarPaisagem(travar: Boolean) {
            if (!uiDeConfianca()) return
            runOnUiThread {
                requestedOrientation = if (travar && !ehTv()) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
