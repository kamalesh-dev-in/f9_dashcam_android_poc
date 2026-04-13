package com.dashcam.lowlatency

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch

/**
 * Main activity for low-latency dashcam RTSP streaming.
 *
 * This POC demonstrates ExoPlayer Media3 achieving lower latency
 * compared to Flutter's media_kit which has ~6 second latency.
 *
 * ExoPlayer Advantage:
 * - Google's official media player with native RTSP support
 * - Actively maintained and documented
 * - Works with cleartext traffic configuration
 *
 * Usage:
 * 1. Connect phone to F9 dashcam WiFi (192.168.169.1)
 * 2. Open app
 * 3. Video streams with reduced latency
 */
@OptIn(UnstableApi::class)
class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var statusBar: TextView
    private lateinit var player: com.dashcam.lowlatency.DashcamPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkNetworkAndStart()
    }

    private fun initViews() {
        surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        statusBar = findViewById<TextView>(R.id.statusBar)

        // Initialize player using factory (change PlayerFactory.currentType to switch players)
        player = PlayerFactory.createPlayer(surfaceView, PlayerCallback())

        // Log startup
        android.util.Log.i("MainActivity", "F9 Dashcam Low-Latency Player initialized")
        android.util.Log.i("MainActivity", DashcamConfig.getConfigSummary())
    }

    private fun checkNetworkAndStart() {
        if (!isNetworkAvailable()) {
            updateStatus("ERROR: No network connection")
            showError("Please connect to WiFi network")
            return
        }

        // NEW: Check if on correct dashcam subnet
        if (!isOnDashcamNetwork()) {
            updateStatus("ERROR: Wrong WiFi network")
            showError(
                "Not connected to dashcam WiFi.\n\n" +
                "Please connect to F9 dashcam WiFi.\n" +
                "Your device IP should be 192.168.169.x"
            )
            return
        }

        // Check if dashcam is reachable
        updateStatus("Checking dashcam connectivity...")
        lifecycleScope.launch {
            val isReachable = checkDashcamReachability()

            if (isReachable) {
                updateStatus("Dashcam found! Connecting...")
                player.connect()
            } else {
                updateStatus("WARNING: Dashcam not reachable")
                showError(
                    "Dashcam at 192.168.169.1 not responding.\n\n" +
                    "Make sure:\n" +
                    "• Dashcam is powered ON\n" +
                    "• Phone is connected to dashcam WiFi\n" +
                    "• Not in airplane mode"
                )
                // Still try to connect - it might work
                updateStatus("Attempting connection anyway...")
                player.connect()
            }
        }
    }

    /**
     * Check if dashcam at 192.168.169.1 is reachable
     */
    private suspend fun checkDashcamReachability(): Boolean = kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO
    ) {
        try {
            val url = java.net.URL("${DashcamConfig.HTTP_BASE}/app/getparamvalue?param=rec")
            android.util.Log.d("MainActivity", "Checking dashcam at ${DashcamConfig.HTTP_BASE}...")
            with(url.openConnection() as java.net.HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                val responseCode = responseCode
                android.util.Log.d("MainActivity", "Dashcam response: HTTP $responseCode")
                responseCode == 200
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Dashcam not reachable: ${e.javaClass.simpleName}")
            false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Check if device is on dashcam network (192.168.169.x)
     * This verifies the phone is connected to the dashcam's WiFi hotspot
     */
    private fun isOnDashcamNetwork(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)

        linkProperties?.linkAddresses?.forEach { linkAddress ->
            val hostAddress = linkAddress.address.hostAddress
            if (hostAddress?.startsWith("192.168.169.") == true) {
                android.util.Log.d("MainActivity", "✓ On dashcam network: $hostAddress")
                return true
            }
        }

        android.util.Log.d("MainActivity", "✗ Not on dashcam network (192.168.169.x)")
        return false
    }

    private fun updateStatus(message: String) {
        statusBar.text = message
        android.util.Log.d("MainActivity", message)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        android.util.Log.i("MainActivity", "Player released")
    }

    override fun onPause() {
        super.onPause()
        // Keep streaming when app goes to background
        // User can disable this by uncommenting:
        // player.stop()
    }

    override fun onResume() {
        super.onResume()
        // Resume when app comes to foreground
        // Uncomment if using pause:
        // if (!player.isPlaying()) lifecycleScope.launch { player.connect() }
    }

    /**
     * Player callback for status updates
     */
    private inner class PlayerCallback : com.dashcam.lowlatency.DashcamPlayer.Callback {
        override fun onStatusChanged(message: String) {
            runOnUiThread { updateStatus(message) }
        }

        override fun onPrepared() {
            runOnUiThread { updateStatus("Player prepared - starting...") }
        }

        override fun onVideoRenderingStarted(latencyMs: Long) {
            runOnUiThread {
                updateStatus("✓ STREAMING! Latency: ${latencyMs}ms")
                Toast.makeText(
                    this@MainActivity,
                    "Low-latency streaming active (~${latencyMs}ms)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onError(error: String) {
            runOnUiThread {
                updateStatus("ERROR: $error")
                showError(error)
            }
        }

        override fun onCompletion() {
            runOnUiThread { updateStatus("Stream completed") }
        }
    }
}
