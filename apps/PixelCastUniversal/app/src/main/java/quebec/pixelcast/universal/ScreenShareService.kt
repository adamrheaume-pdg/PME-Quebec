package quebec.pixelcast.universal

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScreenShareService : Service() {
    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        private const val CHANNEL_ID = "screen_share"
        private const val NOTIF_ID = 1001
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var server: ScreenWebServer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val processing = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var projectionCallback: MediaProjection.Callback? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) { ACTION_STOP -> stopEverything(); ACTION_START -> startEverything(intent) }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun startEverything(intent: Intent) {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Pixel Cast Universal").setContentText("Partage d’écran en cours")
            .setSmallIcon(android.R.drawable.presence_video_online).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIF_ID, notification)

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java) else intent.getParcelableExtra(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || resultData == null) { stopEverything(); return }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() { cleanup(false) }
            }
            projection?.registerCallback(projectionCallback!!, mainHandler)

            server = ScreenWebServer(8080).also { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }

            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics(); @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
            val scale = minOf(1f, 1280f / metrics.widthPixels)
            val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(320)
            val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(480)
            val density = (metrics.densityDpi * scale).toInt().coerceAtLeast(160)

            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            projection?.createVirtualDisplay("PixelCastDisplay", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, mainHandler)

            reader?.setOnImageAvailableListener({ ir ->
                if (!processing.compareAndSet(false, true)) { ir.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
                val image = ir.acquireLatestImage()
                if (image == null) { processing.set(false); return@setOnImageAvailableListener }
                worker.execute {
                    try {
                        val plane = image.planes[0]; val buffer = plane.buffer
                        val pixelStride = plane.pixelStride; val rowStride = plane.rowStride
                        val rowPadding = rowStride - pixelStride * width
                        val paddedWidth = width + rowPadding / pixelStride
                        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                        padded.copyPixelsFromBuffer(buffer)
                        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                        val out = ByteArrayOutputStream(256 * 1024)
                        cropped.compress(Bitmap.CompressFormat.JPEG, 72, out)
                        FrameStore.update(out.toByteArray())
                        cropped.recycle(); padded.recycle()
                    } catch (_: Exception) {} finally { image.close(); processing.set(false) }
                }
            }, mainHandler)
        } catch (_: Exception) {
            stopEverything()
        }
    }

    private fun stopEverything() = cleanup(true)
    private fun cleanup(stopProjection: Boolean) {
        try { reader?.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}; reader = null
        projectionCallback?.let { cb -> try { projection?.unregisterCallback(cb) } catch (_: Exception) {} }
        projectionCallback = null
        if (stopProjection) try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { server?.stop() } catch (_: Exception) {}; server = null
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Partage d’écran", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onDestroy() { try { reader?.close() } catch (_: Exception) {}; try { server?.stop() } catch (_: Exception) {}; worker.shutdownNow(); super.onDestroy() }
}
