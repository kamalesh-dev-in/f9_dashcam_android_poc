package com.dashcam.lowlatency

import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope

/**
 * Interface for dashcam RTSP players.
 *
 * This interface allows switching between different player implementations:
 * - FFmpegPlayer: Native FFmpeg with IPv4-only sockets (works with F9 dashcam)
 * - ExoPlayerPlayer: Google's ExoPlayer Media3 (may have IPv6 issues)
 */
interface DashcamPlayer {
    interface Callback {
        fun onStatusChanged(message: String)
        fun onPrepared()
        fun onVideoRenderingStarted(latencyMs: Long)
        fun onError(error: String)
        fun onCompletion()
    }

    /**
     * Connect to dashcam and start streaming
     */
    suspend fun connect(): Boolean

    /**
     * Stop playback
     */
    fun stop()

    /**
     * Release player resources
     */
    fun release()

    /**
     * Check if player is currently playing
     */
    fun isPlaying(): Boolean

    /**
     * Switch camera (0=front, 1=rear, 2=external)
     */
    suspend fun switchCamera(camera: Int): Boolean
}
