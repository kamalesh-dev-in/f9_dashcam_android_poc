package com.dashcam.lowlatency

import android.view.SurfaceView

/**
 * Factory for creating dashcam player instances.
 *
 * This factory allows easy switching between different player implementations:
 * - FFmpeg: Native FFmpeg with IPv4-only sockets (recommended for F9 dashcam)
 * - ExoPlayer: Google's ExoPlayer Media3 (may have IPv6 issues)
 *
 * To change the player implementation, modify the createPlayer() method.
 */
object PlayerFactory {

    /**
     * Player type selector
     * Change this to switch between player implementations
     */
    enum class PlayerType {
        FFMPEG,    // Native FFmpeg (IPv4-only sockets, works with F9 dashcam)
        EXOPLAYER  // Google's ExoPlayer (may have IPv6 connection issues)
    }

    /**
     * Current player type - change to EXOPLAYER to use ExoPlayer instead
     */
    private val currentType = PlayerType.FFMPEG

    /**
     * Create a new player instance
     *
     * @param surfaceView The SurfaceView to render video to
     * @param callback Optional callback for player events
     * @return A new DashcamPlayer instance
     */
    fun createPlayer(
        surfaceView: SurfaceView,
        callback: DashcamPlayer.Callback? = null
    ): DashcamPlayer {
        return when (currentType) {
            PlayerType.FFMPEG -> {
                android.util.Log.i("PlayerFactory", "Creating FFmpeg player (IPv4-only sockets)")
                FFmpegPlayer(surfaceView, callback)
            }
            PlayerType.EXOPLAYER -> {
                android.util.Log.i("PlayerFactory", "Creating ExoPlayer (may have IPv6 issues)")
                ExoPlayerPlayer(surfaceView, callback)
            }
        }
    }

    /**
     * Create a player with a specific type
     *
     * @param type The player type to create
     * @param surfaceView The SurfaceView to render video to
     * @param callback Optional callback for player events
     * @return A new DashcamPlayer instance
     */
    fun createPlayer(
        type: PlayerType,
        surfaceView: SurfaceView,
        callback: DashcamPlayer.Callback? = null
    ): DashcamPlayer {
        return when (type) {
            PlayerType.FFMPEG -> FFmpegPlayer(surfaceView, callback)
            PlayerType.EXOPLAYER -> ExoPlayerPlayer(surfaceView, callback)
        }
    }
}
