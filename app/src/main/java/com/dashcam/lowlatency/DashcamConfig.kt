package com.dashcam.lowlatency

/**
 * Configuration for F9 Dashcam RTSP streaming.
 *
 * Based on learnings from docs/f9_dashcam_complete_learning.md and MPV analysis:
 * - Dashcam ignores URL paths and query parameters completely
 * - Single RTSP URL for all cameras: rtsp://192.168.169.1:554/ (plain URL)
 * - Camera switching via HTTP API: /app/setparamvalue?param=switchcam&value={0|1|2}
 * - Dashcam forces TCP transport (UDP attempts failed)
 * - Must call /app/enterrecorder before live stream
 *
 * MPV Analysis Confirmation (from Flutter app logs):
 * - MPV successfully connects to: rtsp://192.168.169.1:554/
 * - Video codec: H264 at 960x544 resolution
 * - The /livestream/0 path (Vidure's format) was causing "Connection refused" errors
 */
object DashcamConfig {

    // Network configuration
    const val DASHCAM_IP = "192.168.169.1"
    const val RTSP_PORT = 554
    const val HTTP_PORT = 80

    // RTSP URLs - Use plain URL (F9 dashcam format)
    // From MPV analysis: rtsp://192.168.169.1:554/ works correctly
    // The F9 dashcam IGNORES URL paths completely
    // Previous /livestream/0 path (Vidure's format) was causing connection failures
    private const val RTSP_URL_BASE = "rtsp://$DASHCAM_IP:$RTSP_PORT"  // No trailing slash
    const val RTSP_URL_PLAIN = "$RTSP_URL_BASE/"  // Plain URL - CORRECT for F9 dashcam
    const val RTSP_URL_CAM0 = "$RTSP_URL_BASE/livestream/0"  // Vidure's format - WRONG for F9

    // Use plain URL format (proven to work by MPV analysis)
    const val RTSP_URL = RTSP_URL_PLAIN  // Changed from RTSP_URL_CAM0

    // HTTP API endpoints
    const val HTTP_BASE = "http://$DASHCAM_IP:$HTTP_PORT"

    // Camera channels
    const val CAMERA_FRONT = 0
    const val CAMERA_REAR = 1
    const val CAMERA_PIP = 2

    // === Standard HTTP API endpoints ===
    const val API_ENTER_RECORDER = "$HTTP_BASE/app/enterrecorder"
    const val API_EXIT_RECORDER = "$HTTP_BASE/app/exitrecorder"
    const val API_SWITCH_CAMERA = "$HTTP_BASE/app/setparamvalue?param=switchcam&value="
    const val API_HEARTBEAT = "$HTTP_BASE/app/getparamvalue?param=rec"
    const val API_GET_MEDIA_INFO = "$HTTP_BASE/app/getmediainfo"

    // === Vidure's HTTP API endpoints (from decompilation) ===
    // These are the critical APIs that activate the stream before RTSP connection

    // Step 1: Enter recorder mode (Basic_Auth_Logon)
    // From Vidure: GET /?custom=1&cmd=3023#3035
    const val API_ENTER_RECORDER_VIDURE = "$HTTP_BASE/?custom=1&cmd=3023#3035"

    // Step 2: Get stream URL (Media_Video_GetStreamUrl)
    // From Vidure: GET /?custom=1&cmd=2019
    const val API_GET_STREAM_URL = "$HTTP_BASE/?custom=1&cmd=2019"

    // Step 3: Start live preview (Media_Start_Live) - CRITICAL! This activates the stream
    // From Vidure: GET /?custom=1&cmd=2015&par={camIndex}
    const val API_START_LIVE = "$HTTP_BASE/?custom=1&cmd=2015&par="

    // User-Agent header required by dashcam
    const val USER_AGENT = "HiCamera"

    /**
     * Low-latency IJKPlayer options derived from competitor analysis.
     *
     * Key insights from reverse-engineering Vidure/Lingdu apps:
     * - analyzeduration: 1 microsecond (vs 5s default!) - This is the BIGGEST latency factor
     * - rtsp_transport: tcp (dashcam forces TCP regardless of client preference)
     * - packet-buffering: 0 (disabled for zero buffering)
     *
     * Reference: docs/f9_dashcam_complete_learning.md section "IJKPlayer Configuration"
     */
    val IJK_OPTIONS: Map<String, Pair<Int, Any>> = mapOf(
        // === PLAYER OPTIONS (category = 1) ===
        "start-on-prepared" to (1 to "1"),           // Auto-start on prepared
        "packet-buffering" to (1 to 0),              // Disable packet buffering
        "max-buffer-size" to (1 to 20480),           // 20KB max buffer
        "framedrop" to (1 to 1),                     // Drop late frames
        "min-frames" to (1 to 1),                    // Min frames before playback
        "infbuf" to (1 to 0),                        // Disable infinite buffer

        // === FORMAT OPTIONS (category = 4) ===
        // CRITICAL: analyzeduration: 1 microsecond!
        // Default is 5000000 (5 seconds) - this is the BIGGEST latency factor
        "analyzeduration" to (4 to 1),

        // Maximum analyze duration - 100ms
        "analyzemaxduration" to (4 to 100),

        // Probe size for stream detection - 20KB
        "probesize" to (4 to 20480),

        // Format probe size - 100KB
        "formatprobesize" to (4 to 102400),

        // Flush packets immediately
        "flush_packets" to (4 to 1),

        // RTSP transport - TCP (dashcam forces TCP, UDP attempts failed)
        // From docs: "Dashcam forces TCP regardless of client preference"
        "rtsp_transport" to (4 to "tcp"),

        // Stream timeout - 5 seconds (in microseconds)
        "stimeout" to (4 to "5000000"),

        // DNS cache clear on connect
        "dns_cache_clear" to (4 to 1)
    )

    /**
     * Get configuration summary for logging
     */
    fun getConfigSummary(): String {
        return """
            |
            |F9 Dashcam Low-Latency Configuration:
            |=====================================
            |RTSP URL: $RTSP_URL
            |HTTP API: $HTTP_BASE
            |
            |IJKPlayer Options:
            |• analyzeduration: 1μs (default: 5s) - Saves ~5 seconds!
            |• rtsp_transport: tcp (dashcam forces TCP)
            |• packet-buffering: 0 (disabled)
            |• max-buffer-size: 20KB
            |• framedrop: enabled
            |• min-frames: 1
            |• probesize: 20KB
            |• stimeout: 5s
            |=====================================
            |Expected latency: < 1 second
        """.trimMargin()
    }

    /**
     * Get RTSP URL for specific camera (via API, not URL)
     * Note: Camera switching is done via HTTP API, not RTSP URL
     */
    fun getRtspUrl(): String = RTSP_URL
}
