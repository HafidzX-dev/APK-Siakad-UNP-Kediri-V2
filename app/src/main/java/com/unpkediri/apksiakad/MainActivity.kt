package com.unpkediri.apksiakad

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.unpkediri.apksiakad.data.SecureStorage
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    private var isLoading by mutableStateOf(true)
    private var isRegistered by mutableStateOf(false)
    private var isAuthenticated by mutableStateOf(false)
    private var savedNpm by mutableStateOf("")
    private var savedPassword by mutableStateOf("")

    private var showResetDialog by mutableStateOf(false)
    private var showLoginErrorDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        executor = ContextCompat.getMainExecutor(this)

        //BIOMETRIC SEGMEN
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)

                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(applicationContext, "Keamanan: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticated = true // Triggers Recomposition untuk masuk ke WebView
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Sidik jari tidak dikenali", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verifikasi Keamanan")
            .setSubtitle("Gunakan sidik jari atau kunci layar Anda")
            .setNegativeButtonText("Keluar")
            .setConfirmationRequired(false)
            .build()

        // Periksa data tersimpan secara asinkron di latar belakang tanpa freeze UI thread
        lifecycleScope.launch {
            val saved = SecureStorage.getCredentials(this@MainActivity)
            if (saved != null) {
                savedNpm = saved.first
                savedPassword = saved.second
                isRegistered = true
                authenticateBiometric()
            }
            isLoading = false
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        if (isLoading) {
                            // Loading awal instan tanpa freeze
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else if (!isRegistered) {
                            // Tampilan Form Pendaftaran Awal
                            LoginFormView { n, p ->
                                lifecycleScope.launch {
                                    SecureStorage.saveCredentials(this@MainActivity, n, p)
                                    savedNpm = n
                                    savedPassword = p
                                    isRegistered = true
                                    authenticateBiometric()
                                }
                            }
                        } else if (isAuthenticated) {
                            // Tampilan WebView Utama
                            MyWebView(savedNpm, savedPassword) {
                                showLoginErrorDialog = true
                            }

                            // Tombol Reset Melayang
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                                FloatingActionButton(
                                    onClick = { showResetDialog = true },
                                    containerColor = Color.Red.copy(alpha = 0.5f),
                                    contentColor = Color.White,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(painter = painterResource(R.drawable.ic_refresh), contentDescription = "Reset")
                                }
                            }
                        } else {
                            // Layar Transisi menunggu sidik jari
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(16.dp))
                                Text("Menunggu Verifikasi Keamanan...", color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { authenticateBiometric() }) {
                                    Text("Klik untuk Scan Sidik Jari")
                                }
                            }
                        }

                        // Identitas HafidzX Permanen
                        Text(
                            text = "Create By HafidzX",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        )

                        // DIALOG RESET
                        if (showResetDialog) {
                            AlertDialog(
                                onDismissRequest = { showResetDialog = false },
                                title = { Text("Konfirmasi") },
                                text = { Text("Anda yakin ingin menghapus info Login anda?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        lifecycleScope.launch {
                                            SecureStorage.clearCredentials(this@MainActivity)
                                            isRegistered = false
                                            isAuthenticated = false
                                            savedNpm = ""
                                            savedPassword = ""
                                            showResetDialog = false
                                        }
                                    }) { Text("Iya, Hapus") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetDialog = false }) { Text("Tidak") }
                                }
                            )
                        }

                        // DIALOG LOGIN GAGAL
                        if (showLoginErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { },
                                title = { Text("Login Gagal") },
                                text = { Text("NPM atau Password salah") },
                                confirmButton = {
                                    Button(onClick = {
                                        lifecycleScope.launch {
                                            SecureStorage.clearCredentials(this@MainActivity)
                                            isRegistered = false
                                            isAuthenticated = false
                                            savedNpm = ""
                                            savedPassword = ""
                                            showLoginErrorDialog = false
                                        }
                                    }) { Text("Close") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun authenticateBiometric() {
        try {
            val biometricManager = androidx.biometric.BiometricManager.from(this)
            val canAuth = biometricManager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuth == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                // Perangkat tidak mendukung sidik jari / tidak ada kunci layar, langsung loloskan
                isAuthenticated = true
            }
        } catch (e: Exception) {
            isAuthenticated = true
        }
    }
}

@Composable
fun LoginFormView(onSaveSuccess: (String, String) -> Unit) {
    var inputNpm by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.Gray,
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.LightGray
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_account_circle),
            contentDescription = "Logo SIAKAD",
            tint = Color.White,
            modifier = Modifier.size(100.dp)
        )

        Spacer(Modifier.height(24.dp))
        Text("Login SIAKAD", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = inputNpm,
            onValueChange = { inputNpm = it.filter { char -> char.isDigit() } },
            label = { Text("NPM") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = textFieldColors
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = inputPassword,
            onValueChange = { inputPassword = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (inputNpm.isNotBlank() && inputPassword.isNotBlank() && !isSubmitting) {
                        isSubmitting = true
                        onSaveSuccess(inputNpm.trim(), inputPassword.trim())
                    }
                }
            ),
            trailingIcon = {
                val iconRes = if (passwordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = "Toggle Password",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = textFieldColors
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (inputNpm.isNotBlank() && inputPassword.isNotBlank() && !isSubmitting) {
                    focusManager.clearFocus()
                    isSubmitting = true
                    onSaveSuccess(inputNpm.trim(), inputPassword.trim())
                }
            },
            enabled = !isSubmitting && inputNpm.isNotBlank() && inputPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Simpan & Lanjutkan")
            }
        }
    }
}

@Composable
fun MyWebView(npm: String, pass: String, onError: () -> Unit) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var backPressedTime by remember { mutableStateOf(0L) }
    val loginAttempts = remember { AtomicInteger(0) }
    val hasShownToast = remember { AtomicBoolean(false) }

    // Status kontrol popup pengumuman kampus & refresh
    val hasSeenCampusNotice = remember { AtomicBoolean(false) }
    val isManualRefresh = remember { AtomicBoolean(false) }
    val isLoggedIn = remember { AtomicBoolean(false) }

    // Escape NPM dan Password agar aman saat di-inject ke string literal JavaScript
    val escapedNpm = remember(npm) {
        npm.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "").replace("\r", "")
    }
    val escapedPass = remember(pass) {
        pass.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", "").replace("\r", "")
    }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000) {
                (webViewInstance?.context as? android.app.Activity)?.finish()
            } else {
                Toast.makeText(webViewInstance?.context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                backPressedTime = now
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val swipeRefreshLayout = SwipeRefreshLayout(context)
            swipeRefreshLayout.setColorSchemeColors(
                android.graphics.Color.parseColor("#6200EE"),
                android.graphics.Color.parseColor("#03DAC5")
            )

            val webView = WebView(context).apply {
                // Optimalisasi hardware acceleration & scroll smoothness
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                overScrollMode = View.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    // Gunakan cache maksimal, kurangi network request yang tidak perlu
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    // Pre-rasterize konten di luar layar agar scrolling mulus tanpa jeda/stutter
                    offscreenPreRaster = true

                    // Nonaktifkan zoom: mengurangi gesture processing overhead secara signifikan
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false

                    // Nonaktifkan auto text size - mengurangi reflow/layout recalculation
                    textZoom = 100
                    setSupportMultipleWindows(false)

                    // Blokir popup/overlay yang tidak perlu
                    javaScriptCanOpenWindowsAutomatically = false

                    // Nonaktifkan safe browsing untuk kecepatan navigasi (domain kampus terpercaya)
                    safeBrowsingEnabled = false
                }

                webViewClient = object : WebViewClient() {

                    // Ekstensi file dokumen yang akan dibuka di aplikasi eksternal
                    private val docExtensions = setOf(
                        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
                        "csv", "txt", "zip", "rar", "7z", "odt", "ods", "odp"
                    )

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val url = uri.toString()
                        val host = uri.host?.lowercase() ?: ""
                        val path = uri.path?.lowercase() ?: ""
                        val ext = path.substringAfterLast('.', "").lowercase()

                        // Jika URL adalah dokumen (PDF, Excel, Word, dll) → lempar ke aplikasi yang bisa membukanya
                        if (ext in docExtensions) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setData(uri)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                // Fallback: buka di browser jika tidak ada aplikasi pendukung
                                openInBrowser(context, uri)
                            }
                            return true
                        }

                        // Jika URL masih di domain SIAKAD kampus → tetap di dalam WebView
                        if (host.contains("unpkediri.ac.id")) {
                            return false
                        }

                        // Semua link eksternal lainnya → buka di browser default Android
                        openInBrowser(context, uri)
                        return true
                    }

                    private fun openInBrowser(ctx: android.content.Context, uri: Uri) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            ctx.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(ctx, "Tidak ada aplikasi yang bisa membuka link ini", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        swipeRefreshLayout.isRefreshing = false
                        if (url == null || view == null) return

                        val manual = isManualRefresh.getAndSet(false)
                        val isLoginUrl = url.contains("login", ignoreCase = true)

                        if (!isLoggedIn.get() || isLoginUrl) {
                            // Cek status halaman login secara akurat via DOM hanya jika belum login atau di URL login
                            val checkPageJs = """
                                (function() {
                                    var u = document.querySelector('input[name="username"]');
                                    var p = document.querySelector('input[name="password"]');
                                    var currentUrl = window.location.href.toLowerCase();
                                    if (u !== null && p !== null) {
                                        return "LOGIN_FORM";
                                    }
                                    if (currentUrl.indexOf('login') > -1) {
                                        return "LOGIN_FORM";
                                    }
                                    if (currentUrl.indexOf('unpkediri.ac.id') > -1) {
                                        return "LOGGED_IN";
                                    }
                                    return "OTHER";
                                })();
                            """.trimIndent()

                            view.evaluateJavascript(checkPageJs) { result ->
                                val status = result?.replace("\"", "") ?: "OTHER"

                                if (status == "LOGGED_IN") {
                                    isLoggedIn.set(true)
                                    loginAttempts.set(0)
                                    if (!hasShownToast.getAndSet(true)) {
                                        Toast.makeText(context, "Created By HafidzX", Toast.LENGTH_LONG).show()
                                    }

                                    // Kontrol pengumuman kampus: muncul saat awal masuk, sembunyikan jika kembali dari menu
                                    if (!hasSeenCampusNotice.getAndSet(true)) {
                                        // Pertama kali masuk: biarkan pengumuman muncul agar user dapat membaca
                                    } else if (!manual) {
                                        suppressCampusPopup(view)
                                    }
                                    CookieManager.getInstance().flush()

                                } else if (status == "LOGIN_FORM") {
                                    isLoggedIn.set(false)
                                    val currentAttempt = loginAttempts.get()
                                    if (currentAttempt >= 2) {
                                        loginAttempts.set(0)
                                        onError()
                                    } else {
                                        loginAttempts.incrementAndGet()
                                        val fillAndSubmitJs = """
                                            (function() {
                                                var u = document.querySelector('input[name="username"]');
                                                var p = document.querySelector('input[name="password"]');
                                                var btn = document.querySelector('button[type="submit"]') || 
                                                          document.querySelector('.btn-primary') || 
                                                          document.querySelector('input[type="submit"]');
                                                if (u && p) {
                                                    u.value = '$escapedNpm';
                                                    p.value = '$escapedPass';
                                                    u.dispatchEvent(new Event('input', { bubbles: true }));
                                                    u.dispatchEvent(new Event('change', { bubbles: true }));
                                                    p.dispatchEvent(new Event('input', { bubbles: true }));
                                                    p.dispatchEvent(new Event('change', { bubbles: true }));

                                                    setTimeout(function() {
                                                        if (btn) {
                                                            btn.click();
                                                        } else if (u.form) {
                                                            u.form.submit();
                                                        }
                                                    }, 300);
                                                }
                                            })();
                                        """.trimIndent()
                                        view.evaluateJavascript(fillAndSubmitJs, null)
                                    }
                                }
                            }
                        } else {
                            // User sudah login dan sedang navigasi antar menu SIAKAD
                            // Transisi instan tanpa overhead evaluasi JS login!
                            if (!hasSeenCampusNotice.getAndSet(true)) {
                                // Biarkan pengumuman muncul jika belum pernah tampil
                            } else if (!manual) {
                                suppressCampusPopup(view)
                            }
                        }
                    }
                }
                loadUrl("https://siakad2.unpkediri.ac.id/")
                webViewInstance = this
            }

            swipeRefreshLayout.apply {
                addView(
                    webView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                // canScrollVertically(-1) mencegah SwipeRefreshLayout memotong touch stream saat scrolling halaman
                setOnChildScrollUpCallback { _, _ -> webView.canScrollVertically(-1) }
                setOnRefreshListener {
                    // Refresh manual: user menggeser bagian atas sampai muncul lingkaran refresh
                    isManualRefresh.set(true)
                    webView.reload()
                }
            }
        }
    )
}

/**
 * Menyembunyikan popup pengumuman kampus secara instan via CSS injection tanpa MutationObserver,
 * menghilangkan frame-drop, freeze, dan stutter saat scrolling dan transisi menu.
 */
private fun suppressCampusPopup(view: WebView) {
    val jsSuppressPopup = """
        (function() {
            var styleId = 'anti-modal-style';
            if (!document.getElementById(styleId)) {
                var style = document.createElement('style');
                style.id = styleId;
                style.textContent = '.modal, .modal-backdrop, .swal2-container, .sweet-alert, #modal-pengumuman { display: none !important; opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; } body.modal-open { overflow: auto !important; padding-right: 0px !important; }';
                (document.head || document.documentElement).appendChild(style);
            }
            if (document.body) {
                document.body.classList.remove('modal-open');
                document.body.style.overflow = '';
                document.body.style.paddingRight = '';
            }
            var modals = document.querySelectorAll('.modal.show, .modal.in, .modal-backdrop, .swal2-container');
            for (var i = 0; i < modals.length; i++) {
                modals[i].remove();
            }
        })();
    """.trimIndent()
    view.evaluateJavascript(jsSuppressPopup, null)
}
