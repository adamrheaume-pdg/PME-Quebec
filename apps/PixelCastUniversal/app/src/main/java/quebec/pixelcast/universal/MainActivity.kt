package quebec.pixelcast.universal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var urlText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val intent = Intent(this, ScreenShareService::class.java).apply {
                    action = ScreenShareService.ACTION_START
                    putExtra(ScreenShareService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenShareService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
                val ip = localIpv4() ?: "adresse-IP-du-téléphone"
                statusText.text = "Démarrage du partage…"
                urlText.text = "http://$ip:8080"
                startButton.isEnabled = false
                stopButton.isEnabled = true
            } catch (e: Exception) {
                statusText.text = "Erreur au démarrage : ${e.javaClass.simpleName}"
                startButton.isEnabled = true
                stopButton.isEnabled = false
            }
        } else statusText.text = "Autorisation de partage annulée."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        applySafeInsets(findViewById(android.R.id.content))
        statusText = findViewById(R.id.statusText)
        urlText = findViewById(R.id.urlText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        startButton.setOnClickListener {
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            captureLauncher.launch(mgr.createScreenCaptureIntent())
        }
        stopButton.setOnClickListener {
            startService(Intent(this, ScreenShareService::class.java).apply { action = ScreenShareService.ACTION_STOP })
            statusText.text = "Partage arrêté."
            urlText.text = ""
            startButton.isEnabled = true
            stopButton.isEnabled = false
        }
    }

    private fun applySafeInsets(root: View) {
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(initialLeft + bars.left, initialTop + bars.top, initialRight + bars.right, initialBottom + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun localIpv4(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>().firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }?.hostAddress
    } catch (_: Exception) { null }
}
