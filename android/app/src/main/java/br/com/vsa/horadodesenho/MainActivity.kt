package br.com.vsa.horadodesenho

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
            .addPathHandler("/icones/", object : WebViewAssetLoader.PathHandler {
                override fun handle(path: String): WebResourceResponse? = iconeDoApp(path.removeSuffix(".png"))
            })
            .build()

        web.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                urlPagina = url ?: ""
                super.onPageStarted(view, url, favicon)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                urlPagina = url ?: ""
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            // A tela nunca sai do app: links do guia e da página de download (GitHub/Google) abrem
            // no navegador do aparelho; qualquer outro (ex.: logo do YouTube no player) é ignorado.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame || request.url.host == Uri.parse(view.url ?: "").host) return false
                if (linkExterno(request.url)) abrirNoNavegador(request.url)
                return true
            }
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

        carregarPagina(urlAtual())
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

    /**
     * Página principal carregada no WebView. A ponte roda na thread do JavaBridge, onde o WebView
     * não pode ser consultado (web.url lança exceção fora da thread principal); por isso o endereço
     * é guardado aqui pelo WebViewClient e por quem chama loadUrl.
     */
    @Volatile private var urlPagina: String = ""

    private fun carregarPagina(url: String) {
        urlPagina = url
        web.loadUrl(url)
    }

    private fun uiDeConfianca(): Boolean {
        val atual = urlPagina
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

    /** Apps com ícone no celular (LAUNCHER) ou só no menu da TV (LEANBACK_LAUNCHER). */
    private fun appsDeInicializacao(): List<ResolveInfo> {
        val categorias = listOf(Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER)
        val liberados = todosLiberadosNativos()
        return try {
            categorias.flatMap { packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
                .filter { it.activityInfo?.packageName != packageName && !pacoteBloqueado(it.activityInfo.packageName) }
                .filter { !appDoSistema(it.activityInfo.packageName, liberados) }
                .distinctBy { it.activityInfo.packageName }
                .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        } catch (_: Exception) { emptyList() }
    }

    private fun intentDeAbertura(pkg: String): Intent? =
        packageManager.getLaunchIntentForPackage(pkg) ?: packageManager.getLeanbackLaunchIntentForPackage(pkg)

    /**
     * Apps que vieram com o aparelho (câmera, relógio, Chrome, Gmail…) ficam fora do hub: só entram os
     * instalados pelo usuário. Os que os pais já tinham liberado antes desta regra continuam valendo.
     */
    private fun appDoSistema(pkg: String, liberados: Set<String> = todosLiberadosNativos()): Boolean {
        val sistema = try {
            (packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) { true }
        return sistema && pkg !in liberados
    }

    private fun todosLiberadosNativos(): Set<String> {
        val porPerfil = appsPorPerfilNativos()
        return porPerfil.keys().asSequence()
            .flatMap { id -> porPerfil.optJSONArray(id)?.let { arr -> (0 until arr.length()).map { arr.optString(it) } }.orEmpty().asSequence() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * Com o app fixado (modo criança) e sem Device Owner, o Android não deixa abrir outro app. Por padrão a
     * fixação é solta para abrir um app liberado e refeita quando a criança volta; os pais podem desligar isso.
     */
    private fun soltarFixacaoParaApps(): Boolean = appsPrefs.getBoolean("unpin_for_apps", true)
    @Volatile private var voltandoDeApp = false

    private fun appPodeAbrir(pkg: String): Boolean =
        pkg.isNotBlank() && pkg != packageName && !pacoteBloqueado(pkg) && !appDoSistema(pkg) && intentDeAbertura(pkg) != null

    /**
     * Ícone oficial de um app do hub, em PNG, servido em https://appassets.androidplatform.net/icones/<pacote>.png
     * para as listas da interface. Só responde por apps que o hub pode mostrar.
     */
    private fun iconeDoApp(pkg: String): WebResourceResponse? {
        if (!Regex("[A-Za-z0-9_.]+").matches(pkg) || pkg == packageName || pacoteBloqueado(pkg) || appDoSistema(pkg)) return null
        return try {
            val icone = packageManager.getApplicationIcon(pkg).mutate()
            val lado = 144
            val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
            icone.setBounds(0, 0, lado, lado)
            icone.draw(Canvas(bitmap))
            val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            WebResourceResponse("image/png", null, ByteArrayInputStream(png))
        } catch (_: Exception) { null }
    }

    private fun appsPermitidosNativos(): Set<String> = try {
        val json = JSONArray(appsPrefs.getString("allowed", "[]") ?: "[]")
        (0 until json.length()).mapNotNull { json.optString(it).takeIf { p -> p.isNotBlank() } }.toSet()
    } catch (_: Exception) { emptySet() }

    /** Allowlist parental independente para cada perfil infantil. */
    private fun appsPorPerfilNativos(): JSONObject = try {
        JSONObject(appsPrefs.getString("profile_allowed", "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }

    private fun appsPermitidosPerfilNativo(profileId: String): Set<String> {
        if (profileId.isBlank()) return emptySet()
        return try {
            val arr = appsPorPerfilNativos().optJSONArray(profileId) ?: return emptySet()
            (0 until arr.length())
                .mapNotNull { arr.optString(it).takeIf { pkg -> pkg.isNotBlank() } }
                .toSet()
        } catch (_: Exception) { emptySet() }
    }

    private fun perfilAtivoNativo(): String =
        appsPrefs.getString("active_profile_id", "") ?: ""

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
        intent?.getStringExtra("url")?.let { salvarUrl(it); carregarPagina(it) }
    }

    private fun linkExterno(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return uri.scheme == "https" && listOf("github.com", "github.io", "google.com").any { host == it || host.endsWith(".$it") }
    }

    private fun abrirNoNavegador(uri: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
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
        if (voltandoDeApp) {
            voltandoDeApp = false
            web.evaluateJavascript("window.aplicarModoCrianca && aplicarModoCrianca(telaAtual)", null)
        }
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

        /** Chave da YouTube Data API embutida no build (fase zero); vazia quando o APK foi gerado sem ela. */
        @JavascriptInterface fun chaveEmbutida(): String =
            if (uiDeConfianca()) BuildConfig.YOUTUBE_API_KEY else ""

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
            // Revoga imediatamente permissões ativas que os responsáveis removeram.
            val active = appsAtivosNativos().intersect(requested)
            val activeJson = JSONArray()
            active.sorted().forEach { activeJson.put(it) }
            appsPrefs.edit().putString("active_allowed", activeJson.toString()).apply()
            val kioskReady = sincronizarPacotesQuiosque(requested)
            return JSONObject().put("saved", true).put("deviceOwner", ehDeviceOwner()).put("kioskReady", kioskReady).toString()
        }

        /** Persiste as allowlists parentais por perfil; não aceita pacotes arbitrários. */
        @JavascriptInterface fun definirAppsPorPerfil(json: String): String {
            if (!uiDeConfianca()) return JSONObject().put("saved", false).put("reason", "Origem da interface não confiável.").toString()
            val requested = try { JSONObject(json) } catch (_: Exception) { JSONObject() }
            val sanitized = JSONObject()
            val keys = requested.keys()
            while (keys.hasNext()) {
                val profileId = keys.next()
                if (profileId.isBlank()) continue
                val arr = requested.optJSONArray(profileId) ?: continue
                val allowed = (0 until arr.length())
                    .mapNotNull { arr.optString(it).takeIf { pkg -> appPodeAbrir(pkg) } }
                    .toSet()
                val clean = JSONArray()
                allowed.sorted().forEach { clean.put(it) }
                sanitized.put(profileId, clean)
            }
            appsPrefs.edit().putString("profile_allowed", sanitized.toString()).apply()
            val activeProfile = perfilAtivoNativo()
            val activeAllowed = appsPermitidosPerfilNativo(activeProfile)
            val activeJson = JSONArray()
            activeAllowed.sorted().forEach { activeJson.put(it) }
            appsPrefs.edit().putString("active_allowed", activeJson.toString()).apply()
            val kioskReady = sincronizarPacotesQuiosque(activeAllowed)
            return JSONObject().put("saved", true).put("deviceOwner", ehDeviceOwner()).put("kioskReady", kioskReady).toString()
        }

        /** Ativa apenas apps aprovados nativamente para o perfil informado. */
        @JavascriptInterface fun definirAppsAtivos(profileId: String, json: String): String {
            if (!uiDeConfianca()) return JSONObject().put("saved", false).put("reason", "Origem da interface não confiável.").toString()
            val parentAllowed = appsPermitidosPerfilNativo(profileId)
            val requested = try {
                val arr = JSONArray(json)
                (0 until arr.length())
                    .mapNotNull { arr.optString(it).takeIf { pkg -> pkg in parentAllowed && appPodeAbrir(pkg) } }
                    .toSet()
            } catch (_: Exception) { emptySet() }
            val saved = JSONArray()
            requested.sorted().forEach { saved.put(it) }
            appsPrefs.edit()
                .putString("active_profile_id", profileId)
                .putString("active_allowed", saved.toString())
                .apply()
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

        /** Escolha dos pais: com o modo criança ligado, soltar a fixação para abrir um app liberado. */
        @JavascriptInterface fun definirSoltarFixacaoParaApps(ativo: Boolean) {
            if (!uiDeConfianca()) return
            appsPrefs.edit().putBoolean("unpin_for_apps", ativo).apply()
        }

        /** Só abre pacotes liberados nativamente; com o app fixado sem Device Owner, solta a fixação se os pais permitirem. */
        @JavascriptInterface fun abrirAppPermitido(pkg: String): String {
            if (!uiDeConfianca()) return JSONObject().put("ok", false).put("reason", "Origem da interface não confiável.").toString()
            val activeProfile = perfilAtivoNativo()
            if (pkg !in appsAtivosNativos() || pkg !in appsPermitidosPerfilNativo(activeProfile) || !appPodeAbrir(pkg)) {
                return JSONObject().put("ok", false).put("reason", "Este aplicativo não está liberado pelos responsáveis ou não está instalado.").toString()
            }
            val fixadoSemGerencia = travado() && !ehDeviceOwner()
            if (fixadoSemGerencia && !soltarFixacaoParaApps()) {
                return JSONObject().put("ok", false).put("reason", "Com o modo criança ligado, os apps liberados só abrem se os responsáveis permitirem na área dos pais.").toString()
            }
            return try {
                if (ehDeviceOwner()) sincronizarPacotesQuiosque(appsAtivosNativos())
                val launch = intentDeAbertura(pkg) ?: throw IllegalStateException("Aplicativo sem tela inicial.")
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runOnUiThread {
                    if (fixadoSemGerencia) { voltandoDeApp = true; destravar() }
                    try { startActivity(launch) } catch (_: Exception) {}
                }
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

        /** Cena "Escuro" do bloqueio: brilho da tela no mínimo enquanto ela aparece; depois volta ao do sistema. */
        @JavascriptInterface fun luzMinima(ativa: Boolean) {
            if (!uiDeConfianca()) return
            runOnUiThread {
                window.attributes = window.attributes.apply {
                    screenBrightness = if (ativa) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
}
