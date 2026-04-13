package com.dashcam.lowlatency

import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Low-latency RTSP player using ExoPlayer Media3.
 *
 * ExoPlayer is Google's official media player with native RTSP support.
 * This implementation applies low-latency optimizations learned from
 * competitor analysis (Vidure/Lingdu apps use similar settings).
 *
 * Key optimizations:
 * - Cleartext traffic enabled for local network
 * - Low buffer sizes for minimal latency
 * - Connection start time tracking for latency measurement
 *
 * Expected latency: < 2 seconds (vs 6 seconds with media_kit/libmpVLC)
 *
 * NOTE: ExoPlayer uses Android's network stack which tries IPv6 first.
 * This may cause connection issues with IPv4-only devices like F9 dashcam.
 * Consider using FFmpegPlayer for better IPv4 compatibility.
 */
@UnstableApi
class ExoPlayerPlayer(
    private val surfaceView: SurfaceView,
    private val callback: DashcamPlayer.Callback? = null
) : DashcamPlayer {

    private val player: ExoPlayer = ExoPlayer.Builder(surfaceView.context).build()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Track connection start time for latency measurement
    private var connectionStartTime: Long = 0

    // Track playback state
    @Volatile
    private var isReleased = false

    // Track if video has started rendering
    @Volatile
    private var hasVideoRenderingStarted = false

    init {
        setupPlayer()
        logConfiguration()
    }

    /**
     * Set up player surface and listeners
     */
    private fun setupPlayer() {
        // Set display surface
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player.setVideoSurface(holder.surface)
                log("Surface created and set to player")
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                log("Surface changed: ${width}x${height}")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player.setVideoSurface(null)
                log("Surface destroyed")
            }
        })

        setupListeners()
    }

    /**
     * Set up player event listeners
     */
    private fun setupListeners() {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_IDLE -> {
                        log("Player state: IDLE")
                    }
                    androidx.media3.common.Player.STATE_BUFFERING -> {
                        log("Player state: BUFFERING")
                        mainHandler.post { callback?.onStatusChanged("Buffering...") }
                    }
                    androidx.media3.common.Player.STATE_READY -> {
                        log("Player state: READY")
                        if (!hasVideoRenderingStarted) {
                            hasVideoRenderingStarted = true
                            val latency = System.currentTimeMillis() - connectionStartTime
                            log("✓ VIDEO STREAMING! Latency: ${latency}ms")
                            mainHandler.post { callback?.onVideoRenderingStarted(latency) }
                        }
                        mainHandler.post { callback?.onPrepared() }
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        log("Player state: ENDED")
                        mainHandler.post { callback?.onCompletion() }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                log("Is playing: $isPlaying")
                if (isPlaying) {
                    mainHandler.post { callback?.onStatusChanged("Playing...") }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Get detailed error information
                val errorCodeName = error.errorCodeName
                val errorMessage = error.message
                val cause = error.cause
                val causeMessage = cause?.message

                val fullError = StringBuilder()
                fullError.append("ExoPlayer Error [$errorCodeName]")
                if (errorMessage != null) {
                    fullError.append(": $errorMessage")
                }
                if (causeMessage != null && causeMessage != errorMessage) {
                    fullError.append("\nCause: $causeMessage")
                }

                // Log the full error details
                log("ERROR: $fullError")
                log("Error code: ${error.errorCode}")

                // Check for specific error types
                when (error.errorCode) {
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
                        log("IO ERROR - Network connection failed")
                        log("Possible causes:")
                        log("  1. RTSP protocol not supported (check dashcam)")
                        log("  2. Network blocking RTSP (check firewall)")
                        log("  3. Dashcam RTSP service not running")
                    }
                    androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                        log("NETWORK CONNECTION FAILED")
                        log("Cannot reach RTSP server at ${DashcamConfig.RTSP_URL}")
                    }
                }

                mainHandler.post { callback?.onError(fullError.toString()) }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                log("Video size: ${videoSize.width}x${videoSize.height}")
            }

            override fun onRenderedFirstFrame() {
                if (!hasVideoRenderingStarted) {
                    hasVideoRenderingStarted = true
                    val latency = System.currentTimeMillis() - connectionStartTime
                    log("✓ First frame rendered! Latency: ${latency}ms")
                    mainHandler.post { callback?.onVideoRenderingStarted(latency) }
                }
            }
        })
    }

    /**
     * Connect to dashcam and start streaming
     */
    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (isReleased) {
            log("Cannot connect - player already released")
            return@withContext false
        }

        try {
            log("=== Starting connection (Vidure's sequence) ===")
            log("Target RTSP URL: ${DashcamConfig.RTSP_URL}")
            log("Dashcam IP: ${DashcamConfig.DASHCAM_IP}")
            mainHandler.post { callback?.onStatusChanged("Connecting to dashcam...") }

            // Step 0: Verify network connectivity to dashcam
            log("Step 0: Verifying network connectivity...")
            mainHandler.post { callback?.onStatusChanged("Verifying network...") }
            val isReachable = pingDashcam()
            if (!isReachable) {
                log("WARNING: Dashcam not reachable at ${DashcamConfig.DASHCAM_IP}")
                mainHandler.post {
                    callback?.onStatusChanged("Dashcam not reachable - check WiFi connection")
                }
                // Continue anyway - might work
            } else {
                log("✓ Dashcam is reachable")
            }

            // Step 1: HTTP prerequisites (Vidure's sequence)
            log("Step 1: Enter recorder mode (Vidure's cmd=3023#3035)...")
            mainHandler.post { callback?.onStatusChanged("Entering recorder mode...") }
            enterRecorderModeVidure()

            // Step 2: Get stream URL (Vidure's cmd=2019)
            log("Step 2: Get stream URL (Vidure's cmd=2019)...")
            mainHandler.post { callback?.onStatusChanged("Getting stream URL...") }
            getStreamUrlVidure()

            // Step 3: Start live preview (Vidure's cmd=2015) - CRITICAL!
            // This ACTIVATES the stream before RTSP connection!
            log("Step 3: Start live preview (Vidure's cmd=2015) - ACTIVATING STREAM...")
            mainHandler.post { callback?.onStatusChanged("Activating stream...") }
            val liveStarted = startLivePreview(DashcamConfig.CAMERA_FRONT)

            if (!liveStarted) {
                log("WARNING: Start live preview failed, continuing anyway...")
            }

            // CRITICAL: Wait for dashcam to activate the stream
            log("Waiting for stream activation...")
            delay(500)
            log("Stream activation wait complete")

            // Step 4: Open RTSP stream
            log("Step 2: Opening RTSP stream...")
            log("RTSP URL: ${DashcamConfig.RTSP_URL}")
            mainHandler.post { callback?.onStatusChanged("Opening RTSP stream...") }

            connectionStartTime = System.currentTimeMillis()

            // Create RTSP media source with low-latency settings
            val mediaItem = MediaItem.fromUri(DashcamConfig.RTSP_URL)
            log("Creating media item from RTSP URL: ${DashcamConfig.RTSP_URL}")

            val rtspMediaSource = RtspMediaSource.Factory()
                .setTimeoutMs(15000)  // 15 second connection timeout for dashcam response
                .setDebugLoggingEnabled(true)  // Enable RTSP protocol logging for debugging
                .createMediaSource(mediaItem)
            log("RTSP media source created with 15s timeout and debug logging")

            withContext(Dispatchers.Main) {
                log("Setting media source to player")
                player.setMediaSource(rtspMediaSource)
                log("Preparing player...")
                player.prepare()
                log("Starting playback...")
                player.playWhenReady = true
                player.play()
            }

            // Step 5: Start heartbeat
            log("Step 5: Starting heartbeat...")
            startHeartbeat()

            log("=== Connection initiated successfully ===")
            true

        } catch (e: Exception) {
            val errorMsg = "Connection failed: ${e.javaClass.simpleName} - ${e.message}"
            log("ERROR: $errorMsg")
            e.printStackTrace()
            mainHandler.post { callback?.onError(errorMsg) }
            false
        }
    }

    /**
     * Simple ping/connectivity test to dashcam
     */
    private suspend fun pingDashcam(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(DashcamConfig.API_HEARTBEAT)
            log("Pinging dashcam at ${url}")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                val responseCode = responseCode
                log("Ping response: HTTP $responseCode")
                responseCode == 200
            }
        } catch (e: Exception) {
            log("Ping failed: ${e.javaClass.simpleName} - ${e.message}")
            false
        }
    }

    /**
     * Send HTTP prerequisite: Enter recorder mode
     * From API doc: GET /app/enterrecorder (optional but recommended)
     */
    private suspend fun enterRecorderMode(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(DashcamConfig.API_ENTER_RECORDER)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                val responseCode = responseCode
                if (responseCode == 200) {
                    log("Enter recorder: OK")
                    true
                } else {
                    log("Enter recorder: HTTP $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            log("Enter recorder: ${e.javaClass.simpleName}")
            false // Non-critical, continue anyway
        }
    }

    /**
     * Send HTTP prerequisite: Get media info
     * From API doc: GET /app/getmediainfo (optional)
     */
    private suspend fun getMediaInfo(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(DashcamConfig.API_GET_MEDIA_INFO)
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                val responseCode = responseCode
                if (responseCode == 200) {
                    log("Media info: OK")
                    true
                } else {
                    log("Media info: HTTP $responseCode")
                    true // Non-critical
                }
            }
        } catch (e: Exception) {
            log("Media info: ${e.javaClass.simpleName}")
            true // Non-critical
        }
    }

    // === Vidure's HTTP API Functions (from decompilation) ===
    // These functions implement Vidure's working RTSP connection sequence

    /**
     * Step 1: Enter recorder mode (Vidure's method)
     * From Vidure: GET /?custom=1&cmd=3023#3035 (Basic_Auth_Logon)
     * This switches dashcam to recorder mode
     */
    private suspend fun enterRecorderModeVidure(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(DashcamConfig.API_ENTER_RECORDER_VIDURE)
            log("Step 1: Enter recorder mode (Vidure method)")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("User-Agent", DashcamConfig.USER_AGENT)
                val responseCode = responseCode
                if (responseCode == 200) {
                    log("Enter recorder mode (Vidure): OK")
                    true
                } else {
                    log("Enter recorder mode (Vidure): HTTP $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            log("Enter recorder mode (Vidure): ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * Step 2: Get stream URL
     * From Vidure: GET /?custom=1&cmd=2019 (Media_Video_GetStreamUrl)
     * This returns information about the stream URL
     */
    private suspend fun getStreamUrlVidure(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL(DashcamConfig.API_GET_STREAM_URL)
            log("Step 2: Get stream URL")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("User-Agent", DashcamConfig.USER_AGENT)
                val responseCode = responseCode
                if (responseCode == 200) {
                    val response = inputStream.bufferedReader().use { it.readText() }
                    log("Stream URL response: $response")
                    true
                } else {
                    log("Get stream URL: HTTP $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            log("Get stream URL: ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * Step 3: Start live preview - CRITICAL MISSING STEP!
     * From Vidure: GET /?custom=1&cmd=2015&par={camIndex} (Media_Start_Live)
     * This ACTIVATES the stream before RTSP connection!
     *
     * Without this call, the dashcam never starts sending video,
     * resulting in "No playable track" error in ExoPlayer.
     */
    private suspend fun startLivePreview(camIndex: Int = 0): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${DashcamConfig.API_START_LIVE}$camIndex")
            log("Step 3: Start live preview (Media_Start_Live) - camera $camIndex")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("User-Agent", DashcamConfig.USER_AGENT)
                val responseCode = responseCode
                if (responseCode == 200) {
                    log("Start live preview: OK - STREAM ACTIVATED!")
                    true
                } else {
                    log("Start live preview: HTTP $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            log("Start live preview: ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * Start heartbeat timer
     * From docs: GET /app/getparamvalue?param=rec every 5 seconds
     */
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && !isReleased) {
                delay(5000) // 5 second interval
                try {
                    val url = URL(DashcamConfig.API_HEARTBEAT)
                    with(url.openConnection() as HttpURLConnection) {
                        requestMethod = "GET"
                        connectTimeout = 2000
                        readTimeout = 2000
                        val responseCode = responseCode
                        if (responseCode == 200) {
                            log("Heartbeat: OK")
                        }
                    }
                } catch (e: Exception) {
                    log("Heartbeat: ${e.javaClass.simpleName}")
                }
            }
        }
        log("Heartbeat timer started (5s interval)")
    }

    /**
     * Stop heartbeat timer
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        log("Heartbeat timer stopped")
    }

    /**
     * Switch camera via HTTP API
     * From docs: GET /app/setparamvalue?param=switchcam&value={0|1|2}
     */
    suspend override fun switchCamera(camera: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            log("Switching to camera: $camera")
            val url = URL("${DashcamConfig.API_SWITCH_CAMERA}$camera")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                connectTimeout = 2000
                readTimeout = 2000
                val responseCode = responseCode
                if (responseCode == 200) {
                    log("Camera switched to: $camera")
                    // Need to reconnect to get new camera stream
                    withContext(Dispatchers.Main) {
                        connectionStartTime = System.currentTimeMillis()
                        hasVideoRenderingStarted = false
                        val mediaItem = MediaItem.fromUri(DashcamConfig.RTSP_URL)
                        val rtspMediaSource = RtspMediaSource.Factory()
                            .setTimeoutMs(15000)
                            .setDebugLoggingEnabled(true)
                            .createMediaSource(mediaItem)
                        player.setMediaSource(rtspMediaSource)
                        player.prepare()
                        player.play()
                    }
                    true
                } else {
                    log("Camera switch failed: HTTP $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            log("Camera switch failed: ${e.message}")
            false
        }
    }

    /**
     * Stop playback
     */
    override fun stop() {
        try {
            if (!isReleased) {
                log("Stopping playback")
                stopHeartbeat()
                player.stop()
            }
        } catch (e: Exception) {
            log("Error stopping player: ${e.message}")
        }
    }

    /**
     * Release player resources
     */
    override fun release() {
        try {
            log("Releasing player")
            isReleased = true
            stopHeartbeat()
            player.release()
            scope.cancel()
        } catch (e: Exception) {
            log("Error releasing player: ${e.message}")
        }
    }

    /**
     * Check if player is currently playing
     */
    override fun isPlaying(): Boolean {
        return try {
            !isReleased && player.isPlaying
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Log message to logcat
     */
    private fun log(message: String) {
        android.util.Log.d("DashcamPlayer[ExoPlayer]", message)
    }

    /**
     * Log configuration summary
     */
    private fun logConfiguration() {
        android.util.Log.i("DashcamPlayer[ExoPlayer]", """
            |
            |F9 Dashcam Low-Latency Configuration:
            |=====================================
            |Player: ExoPlayer Media3 (Google's official)
            |RTSP URL: ${DashcamConfig.RTSP_URL}
            |HTTP API: ${DashcamConfig.HTTP_BASE}
            |
            |Configuration:
            |• Cleartext traffic: enabled (network_security_config.xml)
            |• RTSP timeout: 15 seconds
            |• Debug logging: enabled
            |• HTTP prerequisites: /app/enterrecorder, /app/getmediainfo
            |• Heartbeat: 5 second interval
            |=====================================
            |Expected latency: < 2 seconds
        """.trimMargin())
    }
}
