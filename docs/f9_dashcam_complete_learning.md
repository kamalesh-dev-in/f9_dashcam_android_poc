# F9 Dashcam - Complete Learning Documentation

**Project:** Flutter F9 Dashcam Mobile App
**Device:** EEASY-TECH F9 WiFi Dashcam
**Device IP:** `192.168.169.1`
**Documentation Period:** March 2026
**Status:** ✅ Complete with Documented Failures & Successes

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Device Specifications](#2-device-specifications)
3. [Complete API Reference](#3-complete-api-reference)
4. [Network Architecture](#4-network-architecture)
5. [Failure Stories & Solutions](#5-failure-stories--solutions)
6. [Implementation Successes](#6-implementation-successes)
7. [Code Architecture](#7-code-architecture)
8. [Key Learnings](#8-key-learnings)
9. [Troubleshooting Guide](#9-troubleshooting-guide)
10. [Quick Reference](#10-quick-reference)

---

## 1. Project Overview

### Objective
Build a Flutter mobile application that connects to an F9 WiFi Dashcam to provide:
- **Live streaming** from front/rear cameras
- **File browsing** of recorded videos/photos from SD card
- **Video playback** via RTSP
- **Photo viewing** with zoom/pan support
- **Timeline view** for loop/emergency/parking recordings
- **Settings control** (speaker volume, recording controls)

### Technology Stack

| Component | Technology                     |
|-----------|--------------------------------|
| **Framework** | Flutter                        |
| **Video Player** | media_kit (libmpv)             |
| **HTTP Client** | http package                   |
| **State Management** | setState                       |
| **Navigation** | IndexedStack (preserves state) |

---

## 2. Device Specifications

### F9 Dashcam Details

| Attribute | Value |
|-----------|-------|
| **Display Name** | F9 WiFi Dashcam |
| **IP Address** | 192.168.169.1 |
| **RTSP Port** | 554 (primary), 8554 (alternative) |
| **HTTP Port** | 80 |
| **TCP Push Port** | 5000 |
| **WiFi Mode** | Access Point (no internet gateway) |
| **Cameras** | Dual (Front + Rear) |
| **Folders** | Loop, Emergency, Event, Parking|

### Camera Channels

| Camera | API Value | Description |
|--------|-----------|-------------|
| Front | 0 | Default front camera |
| Rear | 1 | Rear camera |

### Folder Types

| Folder | API Value | Description |
|--------|-----------|-------------|
| Loop | `loop` | Continuous recording |
| Emergency | `emr` | Emergency recordings |
| Event | `event` | Event-triggered recordings |
| Parking | `park` | Parking mode recordings |


---

## 3. Complete API Reference

### Base URL
```
http://192.168.169.1
```

### Standard Response Format
```json
{
  "result": 0,
  "info": { ... }
}
```
- `result: 0` = Success
- `result: !0` = Error

---

## 3.1 Device Information APIs

### Get Product Information
```
GET /app/getproductinfo
```

**Response:**
```json
{
  "result": 0,
  "info": {
    "model": "recorder",
    "company": "EEASY-TECH",
    "soc": "chip_platform",
    "sp": "solution_provider"
  }
}
```

---

### Get Device Attributes
```
GET /app/getdeviceattr
```

**Response:**
```json
{
  "result": 0,
  "info": {
    "uuid": "unique_device_id",
    "softver": "1.0.0",
    "otaver": "1.0.1",
    "hwver": "1.0",
    "ssid": "F9-Dashcam-XXXX",
    "bssid": "AA:BB:CC:DD:EE:FF",
    "camnum": 2,
    "curcamid": 0,
    "wifireboot": 0
  }
}
```

---

### Get Storage Information
```
GET /app/getsdinfo
```

**Response:**
```json
{
  "result": 0,
  "info": {
    "status": 0,
    "free": 1024,
    "total": 32768
  }
}
```

**Status Codes:**
- `0` = Normal
- `1` = Unformatted
- `2` = Removed
- `3` = Damaged
- `10` = Locked
- `11` = Low speed
- `12` = Abnormal
- `13` = Formatting
- `99` = Unknown

---

### Get Battery Status
```
GET /app/getbatteryinfo
```

**Response:**
```json
{
  "result": 0,
  "info": {
    "capacity": 85,
    "charge": 0
  }
}
```
- `capacity`: 0-100 (battery percentage)
- `charge`: 0 = Not charging, 1 = Charging

---

### Get Media Information
```
GET /app/getmediainfo
```

**Response:**
```json
{
  "result": 0,
  "info": {
    "rtsp": "rtsp://192.168.169.1",
    "transport": "tcp",
    "port": 5000
  }
}
```

**Use:** Get RTSP server details before opening live stream.

---

## 3.2 Device Management APIs

### Enter Recorder Mode
```
GET /app/enterrecorder
```

**Purpose:** Notify dashcam that app has connected and entered recorder view.

**Response:**
```json
{
  "result": 0,
  "info": "set success"
}
```

**When to Call:** Before opening RTSP live stream (optional but recommended).

---

### Exit Recorder Mode
```
GET /app/exitrecorder
```

**Purpose:** Notify dashcam that app is exiting recorder view.

**When to Call:** When closing live stream view.

---

### Switch Camera
```
GET /app/setparamvalue?param=switchcam&value={0|1}
```

**Parameters:**
- `param`: `switchcam` (fixed)
- `value`: `0` (Front), `1` (Rear)

**Response:**
```json
{
  "result": 0,
  "info": "set success"
}
```

**Critical:** Must call this BEFORE opening RTSP stream for non-front cameras.

---

### Take Snapshot
```
GET /app/snapshot
```

**Purpose:** Capture photo to SD card (EVENT folder).

**Response:**
```json
{
  "result": 0,
  "info": "snapshot success"
}
```

---

### Lock Current Recording
```
GET /app/lockvideo
```

**Purpose:** Protect current recording from overwrite (emergency lock).

**Response:**
```json
{
  "result": 0,
  "info": "lock success"
}
```

---

## 3.3 File Management APIs

### Get File List
```
GET /app/getfilelist?folder={type}&start={n}&end={n}
```

**Parameters:**
- `folder`: `loop`, `emr`, `event`, `park`
- `start`: Start index (0-based)
- `end`: End index (inclusive, 0-based)

**Response (Vidure Format):**
```json
{
  "result": 0,
  "info": [
    {
      "folder": "loop",
      "count": 20,
      "files": [
        {
          "name": "/mnt/card/video_front/20260311_170927_f.ts",
          "duration": -1,
          "size": 85248,
          "createtime": 1773248967,
          "createtimestr": "20260311170927",
          "type": 2,
          "gps": "/GPSdata/20260311_170927.txt",
          "position": 0
        }
      ]
    }
  ]
}
```

**Important:** Files are nested at `info[0]['files']` - not at root level!

---

### Get Thumbnail
```
GET /app/getthumbnail?file={relative_path}
```

**Parameters:**
- `file`: Relative file path from file list

**Response:** Raw JPEG image bytes

---

### Delete File
```
GET /app/deletefile?file={relative_path}
```

**Response:**
```json
{
  "result": 0,
  "info": "delete success"
}
```

---

## 3.4 Playback Control APIs

### Enter Playback Mode
```
GET /app/playback?param=enter
```

**Purpose:** Switch dashcam to playback mode.

**Critical:** **Must call before** opening RTSP file playback!

**When to Call:** Before playing recorded video files.

---

### Exit Playback Mode
```
GET /app/playback?param=exit
```

**Purpose:** Return dashcam to live recording mode.

**Critical:** **Must call after** playing files to restore live stream!

**When to Call:** After video playback, in dispose().

---

## 3.5 Settings APIs

### Get Parameter Items
```
GET /app/getparamitems?param={name}
```

**Get allowed parameter list for a setting.**

---

### Get Parameter Value
```
GET /app/getparamvalue?param={name}
```

**Get current value of a setting.**

**Common Parameters:**
- `param=rec` - Recording status (heartbeat)
- `param=encodec` - Video encoding format
- `param=speaker` - Speaker volume

---

### Set Parameter Value
```
GET /app/setparamvalue?param={name}&value={index}
```

**Common Settings:**

| Setting              | Param                | Values                                   | Description              |
|----------------------|----------------------|------------------------------------------|--------------------------|
| Microphone           | `mic`                | 0=off, 1=on                              | Microphone switch        |
| Recording            | `rec`                | 0=off, 1=on                              | Recording status         |
| Speaker              | `speaker`            | 0=off, 1=low, 2=mid, 3=high, 4=very high | Volume                   |
| Resolution           | `rec_resolution`     | 0=720P, 1=1080P, 2=1296P, 3=2K           | Recording resolution     |
| Duration             | `rec_split_duration` | 0=1MIN, 1=2MIN                           | Recording file duration  |
| Encoding             | `encodec`            | 0=h.264, 1=h.265                         | Video encoding           |
| Collision            | `gsr_sensitivity`    | 0=high, 1=low, 2=off                     | G-sensor sensitivity     |
| wifi Password change | `wifipwd`            | Password                                 | User can change Password |
  

**Note:** All parameter names are **lowercase**.

---

## 3.6 RTSP Streaming

### Live Stream URLs

**Format (Single URL for all cameras):**
```
rtsp://192.168.169.1:554
```

**Camera switching via API:**
```
1. Call: GET /app/setparamvalue?param=switchcam&value={0|1}
2. Open: rtsp://192.168.169.1:554
```


**Note:** Dashcam ignores URL paths - always shows current camera from API.

---

### File Playback URLs

**Format:**
```
rtsp://192.168.169.1:554{file_path}
```

**Example:**
```
rtsp://192.168.169.1:554/mnt/card/video_front/20260311_170927_f.ts
```

**Prerequisite:** Must call `/app/playback?param=enter` first!

---

## 3.7 GPS Data

### GPS Data Format (TCP Push) (But not available for F9 Dashcam)

**Device → App via TCP socket 192.168.169.1:5000**

**Message Format:**
```json
{
  "msgid": "gps",
  "info": {
    "value": "2023/07/06 16:25:59 N:22.524790 E:113.935379 80.00 km/h 271.80 500.00 17 x:-0.004 y:-0.137 z:+0.054"
  },
  "time": 1688675159
}
```

**Fields:**
| Field | Description | Format |
|-------|-------------|--------|
| Date | Date | yyyy/MM/dd |
| Time | Time | HH:mm:ss |
| Latitude | North/South + degrees | N:22.524790 |
| Longitude | East/West + degrees | E:113.935379 |
| Speed | km/h | 80.00 |
| Heading | Degrees 0-360 | 271.80 |
| Altitude | Meters | 500.00 |
| Satellites | Count | 17 |
| G-sensor | X/Y/Z acceleration in G | x:-0.004 y:-0.137 z:+0.054 |

---

### Download GPS Data File (Not applicable for F9 Dashcam)
```
GET /GPSdata/{filename}.TXT
```

**Example:**
```
http://192.168.169.1/GPSdata/20260311_170927.txt
```

---

## 4. Network Architecture

### WiFi Connection Model

```
┌─────────────────┐         ┌─────────────────┐
│   Mobile Phone  │         │   F9 Dashcam    │
│                 │         │                 │
│  ┌───────────┐  │         │  ┌───────────┐  │
│  │ Flutter App│  │         │  │  Firmware │  │
│  └─────┬─────┘  │         │  └─────┬─────┘  │
│        │        │         │        │        │
│  ┌─────▼─────┐  │         │  ┌─────▼─────┐  │
│  │   WiFi    │◄─┼─────────┼─►│   WiFi    │  │
│  │  Client   │  │ WiFi    │  │  AP Mode  │  │
│  └───────────┘  │ Radio   │  └───────────┘  │
└─────────────────┘         └─────────────────┘
     192.168.169.x              192.168.169.1
         (DHCP Client)            (DHCP Server)
```

### Network Characteristics

| Characteristic | Value |
|----------------|-------|
| **IP Address** | 192.168.169.1 (fixed) |
| **WiFi Mode** | Access Point (Hotspot) |
| **Internet Access** | None (closed network) |
| **DHCP** | Dashcam is DHCP server |
| **Connection** | Manual (no auto-connect) |

### Communication Protocols

```
Flutter App                    F9 Dashcam
     │                              │
     ├─ HTTP API (port 80) ────────►│ Device control
     │                              │
     ├─ RTSP (port 554) ──────────►│ Live streaming
     │                              │
     ├─ TCP Push (port 5000) ──────►│ GPS/status updates
     │                              │
```

---

## 5. Failure Stories & Solutions

### Failure Story 1: RTSP Live Stream Connection

#### Initial Assumption (Wrong)
Based on experience with other dashcams, assumed RTSP URLs use channel format:
```
rtsp://192.168.169.1:554/ch01  # Front
rtsp://192.168.169.1:554/ch13  # Rear
```

#### Attempts & Failures

| Attempt | Result |
|---------|--------|
| `rtsp://192.168.169.1:554/ch01` | ❌ VLC: "Can't open" |
| `rtsp://192.168.169.1:554/ch13` | ❌ VLC: "Can't open" |
| Call `/app/enterrecorder` then VLC | ❌ Still won't play |
| `rtsp://192.168.169.1:554` (simple) | ❌ VLC refuses |

#### Discovery Process

**Step 1: Re-read Documentation**
Found camera switching API:
```
GET /app/setparamvalue?param=switchcam&value={0,1}
```

**Step 2: Realized Dashcam is NOT Channel-Path Based**
- Same URL for all cameras
- Camera switched via API call
- URL paths are ignored by dashcam

**Step 3: Investigated VLC vs ffplay Difference**
Created Node.js POC to test:
```
Stream contains:
- AAC codec errors
- Index errors
- Corrupted codec data
```

| Player | Behavior | Why |
|--------|----------|-----|
| VLC | Refuses to play | Strict - rejects malformed streams |
| ffplay | Plays with errors | Lenient - tolerates codec issues |

#### Final Solution

**RTSP URL:**
```dart
const rtspUrl = 'rtsp://192.168.169.1:554';  // Single URL for all cameras
```

**Camera Switching:**
```dart
Future<void> switchCamera(int cameraIndex) async {
  // 1. Tell dashcam to switch camera
  await _switchCameraApi(cameraIndex);

  // 2. Disconnect current stream
  await disconnect();

  // 3. Wait for switch to complete
  await Future.delayed(Duration(milliseconds: 500));

  // 4. Reconnect (same URL, different camera)
  await connect(cameraIndex: cameraIndex);
}

Future<bool> _switchCameraApi(int cameraIndex) async {
  final response = await http.get(
    Uri.parse('http://192.168.169.1/app/setparamvalue?param=switchcam&value=$cameraIndex')
  );
  return response.statusCode == 200;
}
```

#### Key Learnings

| Lesson | Insight |
|--------|---------|
| Never assume industry conventions | Each manufacturer is different |
| Read docs fully | Camera switching API was there initially |
| Test multiple tools | VLC failed but ffplay worked |
| API-driven vs URL-driven | This dashcam uses API, not paths |
| Single URL concept | One URL serves all cameras via API |

---

### Failure Story 2: Camera Switching Delay

#### The Problem
Camera switching has 3-4 second delay between front and rear.

#### What Works
The switching API works perfectly:
- Front → Rear: ✅ Works
- Rear → Front: ✅ Works

#### The Delay Breakdown

| Component | Time |
|-----------|------|
| API call & dashcam switch | Unknown |
| Added delay in code | 500ms |
| Player disconnect | Part of disconnect() |
| **Player initialization** | **~2.5-3.5 seconds** |
| **Total** | **~3-4 seconds** |

#### Investigation

Checked competitor dashcam apps:
- Vidure app: ~0-1 second delay
- Other apps: ~0-1 second delay

**Finding:** This is **industry standard** delay.

#### Root Cause
```
API call triggers camera switch (fast)
    ↓
Dashcam needs time to physically switch cameras
    ↓
Player needs to reconnect and initialize
    ↓
New stream needs to buffer before display
```

#### Final Implementation

```dart
Future<void> switchCamera(int cameraIndex) async {
  await _switchCameraApi(cameraIndex);
  await disconnect();
  await Future.delayed(Duration(milliseconds: 500));  // Kept as-is
  await connect(cameraIndex: cameraIndex);
}
```

**Status:** Accepted as-is (competitors have same delay)

---

### Failure Story 3: WiFi Connection & Network Handling

#### Issue #1: Manual WiFi Connection Required

Dashcam WiFi is **not auto-connecting**. User must manually connect in phone settings.

**Why:** Dashcam creates Access Point mode WiFi (no internet gateway). Android doesn't auto-connect to hotspots without internet.

**Decision:** Accepted manual connection as required behavior.

---

#### Issue #2: Same IP Conflict (Multiple Dashcams)

**Problem:**
```
F9 Dashcam:     192.168.169.1
Other Dashcam:  192.168.169.1  (Same IP!)
If same then checks with SSID (new approach to fix the issue)
```

**Solution: SSID-Based Detection**
```dart
bool isF9Dashcam() {
  final ssid = getConnectedSSID();
  return ssid.toLowerCase().contains('f9');
}
```

---

#### Issue #3: Internet Cut When Connected

**The Problem:**
```
User connects to dashcam WiFi
    ↓
❌ Mobile data disconnected
    ↓
❌ No internet access
```

**Handling:** No warnings added. Accepted as limitation.

---

#### Background Behavior

**During phone calls:**
- ✅ Phone calls work
- ❌ Network/internet still cut

**Other apps needing internet:**
- Other apps can't access internet
- Must disconnect from dashcam WiFi

---

#### Reused WiFi Detection Solution

```dart
// Monitor WiFi state changes
Connectivity().onWiFiChanged.listen((ssid) {
  if (isDashcamSSID(ssid)) {
    navigateTo(DashcamScreen());
  } else {
    navigateTo(ConnectDashcamScreen());
  }
});

bool isDashcamSSID(String ssid) {
  return ssid.toLowerCase().contains('f9');
}
```

**User Experience Flow:**
```
User opens app
    ↓
Not connected to dashcam WiFi?
    ↓
Show "Connect to Dashcam" screen
    ↓
User connects to dashcam WiFi
    ↓
App detects WiFi change → Auto-navigate to Dashcam Screen
    ↓
User uses dashcam features (no internet)
    ↓
User disconnects → App detects → Back to "Connect Dashcam" screen
```

---

### Failure Story 4: The 6-Second Latency Issue (UNSOLVED)

#### The Problem
Live stream has **6-second delay** - real-time movements reflect after 6 seconds.

---

#### Hypothesis #1: TCP vs UDP (FAILED)

**Thought:** TCP causes high latency. UDP should fix it.

**Attempts:**
| Attempt | Result |
|---------|--------|
| `rtsp-transport=udp` libmpv option | ❌ Still TCP |
| `?transport=udp` URL parameter | ❌ Still TCP |
| FFmpeg `-rtsp_transport udp` | ❌ Still TCP |

**Discovery:** Dashcam **forces TCP** regardless of client preference.

---

#### Hypothesis #2: Audio Codec Errors (FAILED)

**Thought:** AAC errors cause buffering → delay

**Test:** Disabled audio completely
```dart
player.setLibmpvOption('aid', 'no');
```

**Result:** ❌ **Still 6 seconds delay**

---

#### Hypothesis #3: Buffer Size (FAILED)

**Tried buffer reductions:**
```dart
player.setLibmpvOption('cache', 'no');
player.setLibmpvOption('demuxer-max-bytes', '102400');
player.setLibmpvOption('demuxer-max-back-bytes', '102400');
```

**Result:** ❌ **Still 6 seconds delay**

---

#### Hypothesis #4: Low-Delay Flags (FAILED)

**Tried low-delay configuration:**
```dart
player.setLibmpvOption('demuxer-lavf-o', 'fflags=nobuffer');
player.setLibmpvOption('demuxer-lavf-o', 'flags=low_delay');
player.setLibmpvOption('untimed', 'yes');
```

**Result:** ❌ **Still 6 seconds delay**

---

#### All Optimizations Summary

| Optimization | Result |
|--------------|--------|
| Switch to UDP | ❌ No change (dashcam forces TCP) |
| Disable audio | ❌ No change |
| Reduce buffer size | ❌ No change |
| Low-delay flags | ❌ No change |
| Cache disabled | ❌ No change |

**All optimizations failed - delay remains 6 seconds.**

---

#### The Breakthrough: Competitor Analysis

**Research Method:**
```
Downloaded competitor apps
    ↓
Vidure app, Lingdu app
    ↓
Decompiled APKs
    ↓
Reverse-engineered code
```

**What I Found:**

| App | Player Used    | Latency |
|-----|----------------|---------|
| **Vidure** | IJKPlayer      | ~1 second |
| **Lingdu** | IJKPlayer      | ~1 second |
| **My App (media_kit)** | exoPlayer type | ~6 seconds |

---

#### IJKPlayer Discovery

**What is IJKPlayer?**
- Android native video player
- Based on FFmpeg, heavily optimized
- Used by: Bilibili, TikTok, and major Chinese streaming apps
- Purpose-built for **low-latency live streaming**
- Achieves **~1 second latency**

---

#### Future Plan: Android Native POC with IJKPlayer

**Phase 1: Pure Android Native POC (still in checking process)**
```
Create Android native app
    ↓
Integrate IJKPlayer
    ↓
Test with F9 dashcam
    ↓
Verify latency improvement
```

**Phase 2: Flutter Integration (Future)**
```
After POC success
    ↓
Create Flutter plugin
    ↓
Use Platform Channel to bridge native IJKPlayer
    ↓
Use IJKPlayer for live streaming only
    ↓
Keep media_kit for video playback (files)
```

---

#### Hybrid Architecture Plan

```
Flutter App
    ↓
├── Live Stream Screen
│   └── IJKPlayer (via Platform Channel) → ~1 sec latency
│
└── Video Playback Screen
    └── media_kit (libmpVLC) → Works fine for recorded files
```

**Why hybrid:**
- media_kit works perfectly for recorded file playback
- Only live streaming needs low latency

---

#### IJKPlayer Configuration (From Reverse Engineering)

```java
IjkMediaPlayer ijkPlayer = new IjkMediaPlayer();

// Low latency settings
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "fflags", "nobuffer");
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flags", "low_delay");
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0);
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 1);
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30);
ijkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
```

---

**Current Status:** 🔄 In Progress - Planning IJKPlayer POC

---

### Failure Story 5: Thumbnail Loading Delay

#### The Observation
Thumbnails load successfully, but there's a delay before they appear.

**Delay is proportional to video count:**
| Video Count | Load Time |
|-------------|-----------|
| 20 videos | 3-4 seconds |
| 100 videos | 10-12 seconds |

---

#### Root Cause: Eager Loading Architecture

**Current Implementation:**
```dart
// Called BEFORE showing UI
await _cacheAllThumbnails();  // Blocks until ALL downloaded

Future<void> _cacheAllThumbnails() async {
  // Downloads ALL thumbnails synchronously
  for (var video in frontVideoArray) {
    await _downloadAndSaveThumbnail(thumbnailUrl, video.name);
  }
  // NOW show UI with cached thumbnails
}
```

**Why So Slow:**
- Each thumbnail = 1 separate API call to dashcam
- Sequential downloads (await each one)
- 100 thumbnails × 100ms each = 10+ seconds
- No progressive loading - all-or-nothing

---

#### Current Caching Strategy

**Two-Level Cache:**
```
1. Local File Cache (Persistent)
   - Location: /thumbnail_images/
   - Survives app restart
   - Checked first before API call

2. In-Memory Map (Session)
   - Map<String, String> localThumbnailPathMap
   - Key: video name, Value: local file path
```

**Benefit:** Subsequent opens are instant.

---

#### What Was Attempted

| Optimization Attempt | Status |
|---------------------|--------|
| Lazy loading (load as scroll) | ❌ Not tried yet |
| Concurrent/parallel downloads | ❌ Not tried yet |
| FutureBuilder implementation | ❌ Not tried yet |

**Result:** No optimizations attempted - accepted current behavior.

---

#### Current User Experience

```
Open file browser
    ↓
[Loading spinner/indicator]
    ↓
Wait 3-12 seconds (based on video count)
    ↓
UI appears with all thumbnails loaded
```

**User Feedback:** Not yet deployed to production users.

---

#### Potential Optimizations (Not Yet Implemented)

**Option 1: Lazy Loading**
```dart
ListView.builder(
  itemBuilder: (context, index) {
    return FutureBuilder(
      future: getThumbnail(files[index]),
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          return Image.file(snapshot.data);
        }
        return CircularProgressIndicator();
      },
    );
  },
)
```

**Option 2: Parallel Downloads**
```dart
final futures = videos.map((v) => downloadThumbnail(v));
await Future.wait(futures);
```

**Option 3: Progressive Loading**
```
Show file list immediately
    ↓
Load thumbnails in background
    ↓
Update UI as each thumbnail arrives
```

---


### Failure Story 6: Video Playback from SD Card

#### Initial Attempt (FAILED)

```dart
// Tried to play directly without prerequisites
final rtspUrl = 'rtsp://192.168.169.1:554/mnt/card/video_front/20260311_170927_f.ts';
player.open(Media(rtspUrl));
```

**Result:** ❌ **"No videos found"**

---

#### Discovery: Playback Mode Required

**From Documentation:**
```
Enter Playback Mode: GET /app/playback?param=enter
Exit Playback Mode: GET /app/playback?param=exit
Note: Required before opening RTSP file playback
```

---

#### Root Cause: Wrong Dashcam Mode

```
Dashcam Modes:

┌─────────────────┐
│  LIVE MODE      │ ← Default state
│  (Recording)    │   Live streaming works
│                 │   File playback DOESN'T work
└────────┬────────┘
         │
         │ /app/playback?param=enter
         ▼
┌─────────────────┐
│  PLAYBACK MODE  │ ← Required for files
│  (File Playback)│   File playback works
│                 │   Live streaming disrupted
└────────┬────────┘
         │
         │ /app/playback?param=exit
         ▼
┌─────────────────┐
│  LIVE MODE      │ ← Back to normal
│  (Recording)    │   Everything works
└─────────────────┘
```

---

#### Working Implementation

**Step 1: Enter Playback Mode**
```dart
Future<void> _enterPlaybackMode() async {
  final response = await http.get(
    Uri.parse('http://192.168.169.1/app/playback?param=enter')
  );

  if (response.statusCode == 200) {
    print('[Playback] Entered playback mode');
  } else {
    throw Exception('Failed to enter playback mode');
  }
}
```

**Step 2: Load File List with Pagination**
```dart
Future<void> _loadFileList() async {
  final response = await http.get(
    Uri.parse('http://192.168.169.1/app/getfilelist?folder=loop&start=1&end=160')
  );

  final fileList = FileListResponse.fromJson('loop', jsonDecode(response.body));
  _videos = fileList.files;
}
```

**Step 3: Build RTSP URL**
```dart
String buildPlaybackUrl(String filePath) {
  return 'rtsp://192.168.169.1:554$filePath';
}

// Example
// rtsp://192.168.169.1:554/mnt/card/video_front/20260311_170927_f.ts
```

**Step 4: Play Video**
```dart
void _playVideo(String filePath) {
  final rtspUrl = buildPlaybackUrl(filePath);
  player.open(Media(rtspUrl));
}
```

**Step 5: Exit Playback Mode (Critical!)**
```dart
@override
void dispose() {
  _exitPlaybackMode();
  super.dispose();
}

Future<void> _exitPlaybackMode() async {
  await http.get(
    Uri.parse('http://192.168.169.1/app/playback?param=exit')
  );
}
```

---

#### Why Exit Playback Mode Is Critical

**What Happens If You Forget:**
```
User watches video
    ↓
Closes screen (no exit call)
    ↓
Switches to Live Stream tab
    ↓
❌ Live stream INTERRUPTED
    ↓
Dashcam still stuck in PLAYBACK MODE
```

**Why:** Dashcam can only be in ONE mode at a time.

---

#### App Architecture

```
File Browser Screen
    ↓ User taps video
    ↓
Video Player Screen (NEW SCREEN)
    ↓ initState()
    ↓ 1. _enterPlaybackMode()
    ↓ 2. _playVideo(filePath)
    ↓ [User watches video]
    ↓ User closes video
    ↓ dispose()
    ↓ 3. _exitPlaybackMode()
    ↓
Return to File Browser
    ↓ User switches to Live Stream
    ↓ LiveStreamScreen reinitializes fresh
```

---


#### Expected Implementation (Planned)

```dart
class RtspService {
  Timer? _heartbeatTimer;

  void _startHeartbeat() {
    _heartbeatTimer = Timer.periodic(Duration(seconds: 5), (_) async {
      await _sendHeartbeat();
    });
  }

  Future<void> _sendHeartbeat() async {
    try {
      final response = await http.get(
        Uri.parse('http://192.168.169.1/app/getparamvalue?param=rec')
      );

      if (response.statusCode == 200) {
        // Success
      } else {
        // Handle error
        _status = StreamConnectionStatus.error;
      }
    } catch (e) {
      _status = StreamConnectionStatus.error;
    }
  }

  void _stopHeartbeat() {
    _heartbeatTimer?.cancel();
    _heartbeatTimer = null;
  }
}
```

---

#### What Needs Testing

**Test 1: Without Heartbeat**
```
1. Connect to live stream
2. Don't send heartbeat
3. Wait and observe
4. What happens? When?

Result --> The live stream fails to play with stream not connected
```

**Test 2: With Heartbeat (5 seconds)**
```
1. Connect to live stream
2. Send heartbeat every 5 seconds
3. Wait for 10+ minutes
4. Does connection stay alive?

Result --> The live stream able to play 
```

**Status:** ⏳ Not fully tested yet.

---

### Failure Story 7: Timeline Screen Implementation

#### Initial Requirements (March 19, 2026)

**User Request:**
- Create timeline for loop videos only
- Show start/end times for videos (e.g., 10:00-10:05)
- Display green segments for available videos, grey for gaps
- Handle scrubbing to grey areas by jumping to next video

---

#### Critical Failure 1: Preloading Videos Caused Dashcam Crashes

**What Was Attempted:**
```dart
// WRONG APPROACH
Future<void> _preloadAdjacentVideos() async {
  for (int i = 1; i <= 2; i++) {
    _preloadedPlayers[nextClip.id] = Player();
    await _preloadedPlayers[nextClip.id]!.open(Media(...));
  }
}
```

**Why It Failed:**
1. Multiple RTSP connections overwhelmed dashcam
2. Dashcam crashed and restarted
3. Parallel operations violated dashcam limitations

**User Feedback:**
> "remember that dont try to make multiple rtsp call or api call to dashcam it may be crash the dashcam"

**Fix Applied:**
```dart
// CORRECT APPROACH
// Load ONE video at a time
await _player!.stop();
await _player!.dispose();
_player = null;
// Then load next video
```

---

#### Critical Failure 2: Seeking Within Video Didn't Work

**What Was Attempted:**
```dart
// WRONG APPROACH
Future<void> _onScrub(Duration targetPosition) async {
  await _player!.seek(targetPosition);  // Doesn't work!
  await _player!.play();
}
```

**Why It Failed:**
1. RTSP protocol can't seek to arbitrary position
2. Dashcam unable to load data from mid-point
3. Video failed to play or got stuck

**User Feedback:**
> "i had noticed that if i try to play the video from certain point the dashcam unable to load the data"

**Fix Applied:**
```dart
// CORRECT APPROACH
// Always play from START of video
Future<void> _onScrub(DateTime targetTime) async {
  // 1. Find clip covering this time
  F9TimelineClip? targetClip = _findClipForTime(targetTime);

  // 2. Close current completely
  await _player!.stop();
  await _player!.dispose();
  _player = null;

  // 3. Open FROM START
  await _player!.open(Media(targetClip.getRtspUrl()), play: true);

  // Note: User scrubs to 11:02 → Video starts from 11:00
}
```

---

#### Critical Failure 3: Incomplete Camera Switch Caused Issues

**What Was Attempted:**
```dart
// WRONG APPROACH
void _onCameraChanged() {
  setState(() => _currentCameraIndex = _tabController.index);
  _filterClipsByCamera();
  // Continue using same player - PROBLEM!
}
```

**Why It Failed:**
1. Same player used for both cameras
2. RTSP connection not properly closed
3. Could cause crashes or stuck states

**User Feedback:**
> "single timeline use the design like in the playback screen if the user switches from front to rear the timeline and all other feature should load for rear to avoid unwanted delay,crash and stuck"

**Fix Applied:**
```dart
Future<void> _onCameraChanged() async {
  // 1. STOP current playback
  await _player!.stop();

  // 2. DISPOSE player completely
  await _player!.dispose();
  _player = null;

  // 3. Clear front camera clips
  _currentClips.clear();

  // 4. Update camera index
  _currentCameraIndex = _tabController.index;

  // 5. Load rear camera clips
  _updateCurrentClips();

  // 6. Create FRESH player
  _player = Player(configuration: PlayerConfiguration(...));

  // 7. Open first rear video
  await _player!.open(Media(_currentClips.first.getRtspUrl()), play: true);
}
```

---

#### Critical Failure 4: Design Mismatch

**What Was Attempted:**
- Minimalist timeline with no border radius
- Sharp edges everywhere
- No decoration
- Plain flat colors

**User Feedback:**
> "the color and ui design should be similer to the existing code"

**Fix Applied:**
Launched Explore agent to analyze existing code:
- Found primary color: `#C62828` (red)
- Found border radius: 12-16px
- Found shadow usage: alpha 0.05-0.1
- Found spacing patterns: 4.w horizontal, 1.5.h vertical

---

#### Success 1: Gap-Skipping Algorithm

**Problem:** What happens when user scrubs to grey area (no video)?

**Solution:**
```dart
Future<void> _onScrub(DateTime targetTime) async {
  F9TimelineClip? targetClip = _findClipForTime(targetTime);

  if (targetClip == null) {
    // Scrubbed to grey area - find NEXT available video
    F9TimelineClip? nextClip = _findNextVideoAfter(targetTime);

    if (nextClip != null) {
      await _loadVideoFromStart(nextClip);
      _showJumpedMessage(nextClip.startTime);
    }
  }
}
```

**Behavior:**
```
Timeline: 10:00-10:05 [green] | 10:05-10:10 [green] | 10:10-10:15 [grey] | 10:15-10:20 [green]

User scrubs to 10:12 (grey):
    ↓
Auto-jump to 10:15
    ↓
Play from 10:15 (START)
```

---

#### Success 2: Mixed Timeline (Loop + Emergency + Parking)

**User's Evolution:**
- Started with loop-only requirement
- Later requested: "lets also fetch emergency and parking and add it to the timeline as well"

**Solution:**
```dart
Map<String, List<F9DashCamTsFile>> videosMap = {
  'loop': await getFileList(folder: 'loop'),
  'emr': await getFileList(folder: 'emr'),
  'park': await getFileList(folder: 'park'),
};

// Merge all clips by start time
allClips.sort((a, b) => a.startTime.compareTo(b.startTime));
```

**Color Coding:**
- 🟢 Green = Loop
- 🔴 Red = Emergency
- 🟡 Yellow = Parking
- ⚪ Grey = Gaps

---

#### Success 3: Date + Time Selection

```dart
Future<void> _onDateSelected(DateTime selectedDate) async {
  await _player!.dispose();
  _selectedDate = selectedDate;
  _updateCurrentClips();
  _timelineSegments = F9TimelineHelper.buildTimelineWithGaps(
    _currentClips,
    _selectedDate,
  );
  await _player!.open(Media(_currentClips.first.getRtspUrl()), play: true);
}
```

**Behavior:**
- Select "Yesterday" → Timeline shows yesterday's clips
- Scrub to 14:30 on yesterday → Plays yesterday's video (or nearest)

---

#### Success 4: Time Ruler with Markers

**Problem:** Time markers started at wrong time (hour instead of first video)

**Fix:**
```dart
// Start from actual timeline start
DateTime currentTime = timelineStart;  // e.g., 14:10

// Add first marker at exact start time
markers.add(_TimeMarkerWithSeconds(
  label: '14:10',
  secondsFromStart: 0,
  isHourMarker: true,
));

// Generate markers every 5 minutes
currentTime = currentTime.add(Duration(minutes: 5));
```

---

#### Timeline Implementation Summary

**Files Created:**
1. `lib/screens/dash_cam/f9/f9_dash_cam_timeline_screen.dart` - Main screen
2. `lib/utils/f9_timeline_helper.dart` - Helper functions

---

#### Key Constraints Discovered

| Constraint | Impact |
|------------|--------|
| ❌ No seeking within videos | Must play from start |
| ❌ No preloading/caching | Load one at a time |
| ❌ Single RTSP connection only | No parallel connections |
| ❌ Play from start only | Scrub = jump to video start |
| ❌ Complete reload on camera switch | Full dispose and reload |

---

## 6. Implementation Successes

### Success: Proper File List Parsing (Vidure API)

#### The Challenge
Generic F9 API documentation showed different JSON structure than actual Vidure implementation.

#### Expected vs Actual

**Expected (from docs):**
```json
{
  "result": 0,
  "files": [
    {
      "name": "REC_20240115_143026.mov",
      "duration": 300,
      "size": 150000,
      "type": "video"
    }
  ]
}
```

**Actual (Vidure):**
```json
{
  "result": 0,
  "info": [
    {
      "folder": "loop",
      "count": 20,
      "files": [...]  // ← Nested at info[0]['files']
    }
  ]
}
```

#### Solution: Defensive Parser with Fallbacks

```dart
factory FileListResponse.fromJson(String folder, Map<String, dynamic> json) {
  List<dynamic>? filesList;

  // Try 1: Vidure format (nested)
  final info = json['info'];
  if (info is List && info.isNotEmpty) {
    final firstInfo = info[0];
    if (firstInfo is Map<String, dynamic>) {
      filesList = firstInfo['files'] as List<dynamic>?;
    }
  }

  // Try 2: Generic format (direct files array)
  if (filesList == null) {
    filesList = json['files'] as List<dynamic>?;
  }

  // Try 3: Info as direct array
  if (filesList == null && info is List) {
    filesList = info;
  }

  // Parse files...
}
```

#### Field Variations Handled

```dart
factory F9File.fromJson(Map<String, dynamic> json) {
  // Handle time field variations
  final timeStr = json['createtimestr'] ?? json['time'] ?? '';

  // Handle type variations (int vs string)
  final typeInt = json['type'] as int?;
  FileType? fileType;
  if (typeInt != null) {
    fileType = typeInt == 2 ? FileType.video : FileType.picture;
  }

  // Handle negative duration
  final durationVal = json['duration'] as int? ?? 0;
  final finalDuration = durationVal < 0 ? 0 : durationVal;

  return F9File(
    name: json['name'] as String? ?? '',
    size: json['size'] as int? ?? 0,
    time: timeStr,
    duration: finalDuration,
    type: fileType,
  );
}
```

---

### Success: IndexedStack Navigation

**Pattern:** Using `IndexedStack` to preserve screen state across tab switches.

```dart
body: IndexedStack(
  index: _selectedIndex,
  children: const [
    LiveStreamScreen(),
    PlaybackListScreen(),
    SettingsScreen(),
  ],
)
```

**Benefit:** All screens stay in memory - video stream doesn't restart when switching tabs.

---

### Success: Two-Level Thumbnail Caching

**Strategy:**
```
1. Local File Cache (Persistent)
   - Location: /thumbnail_images/
   - Survives app restart

2. In-Memory Map (Session)
   - Map<String, String>
   - Faster access
```

**Benefit:** Second load is instant (thumbnails already cached).

---

## 6.1 Performance & UX Improvements (April 2026)

### Timeline Screen Improvements

#### Playhead Positioning Fix

**Problem:** Playhead pointer pointed to grey gap area instead of green video segment, showing "No videos available" incorrectly.

**Root Cause:** Buffer width calculation mismatch - code used `screenWidth * 0.72` but comment said "50% of screen width".

**Solution:** Changed buffer width to `screenWidth * 0.51` (line 857 in `f9_dash_cam_timeline_screen.dart`)

```dart
// Before:
double bufferWidth = screenWidth * 0.72;

// After:
double bufferWidth = screenWidth * 0.51;
```

**Result:** Playhead now correctly points to green video segment.

---

#### Video Seeking Implementation

**Problem:** Seeking beyond video duration caused stuck/infinite buffering; player crashed without proper initialization. Multiple API calls to dashcam crashed it, making seek impossible.

**Solution:** Implemented `_loadVideoWithSeek()` with safety measures:

```dart
Future<void> _loadVideoWithSeek(F9TimelineClip clip, Duration position) async {
  // 1. Cancel old subscriptions FIRST
  await _positionSubscription?.cancel();
  await _playingSubscription?.cancel();

  // 2. Stop and dispose old player
  await _player?.stop();
  await _player?.dispose();
  await Future.delayed(Duration(milliseconds: 100));

  // 3. Create fresh player
  _player = Player(configuration: PlayerConfiguration(title: 'F9 Timeline'));

  // 4. CRITICAL: Clamp position to valid range
  int videoDurationSeconds = clip.duration.inSeconds;
  int safePositionSeconds = position.inSeconds.clamp(0, videoDurationSeconds - 1);
  Duration safePosition = Duration(seconds: safePositionSeconds);

  // 5. Open video WITHOUT auto-play
  await _player!.open(Media(clip.getRtspUrl()), play: false);

  // 6. Wait for RTSP stream to buffer (2 seconds)
  await Future.delayed(Duration(milliseconds: 2000));

  // 7. Seek to clamped safe position
  await _player!.seek(safePosition);

  // 8. Additional buffer after seek (500ms)
  await Future.delayed(Duration(milliseconds: 500));

  // 9. Start playback
  await _player!.play();
}
```

**Key Safety Features:**
- Position clamping prevents seeking beyond video content
- 2-second buffer time before seeking for RTSP stability
- 500ms buffer after seek before playback
- Full player disposal and re-initialization

---

#### Rewind/Forward 10 Seconds

**Smart Boundary Handling:**
- If >10s into video → seek back/forward 10s
- If <10s into video → load previous/next video

```dart
Future<void> _rewind10Seconds() async {
  if (currentPosition > Duration(seconds: 10)) {
    await _player!.seek(currentPosition - Duration(seconds: 10));
  } else if (_currentClipIndex > 0) {
    await _loadVideoFromStart(_currentClipArray[_currentClipIndex - 1]);
  }
}
```

---

#### Smart Auto-Advance

**Problem:** Video would auto-advance when user was scrubbing or had paused.

**Solution:** Only auto-advance when video is ACTUALLY PLAYING and reaches the end:

```dart
void _checkAutoAdvance(Duration position) {
  if (_isSwitchingVideo) return;
  if (_isDraggingSlider) return;

  // Only advance if ACTUALLY PLAYING and near end
  bool isPlayingAndNearEnd = _isPlaying && position.inSeconds >= currentClip.duration.inSeconds - 2;

  if (isPlayingAndNearEnd && _currentClipIndex < _currentClipArray.length - 1) {
    _loadVideoFromStart(_currentClipArray[_currentClipIndex + 1]);
  }
}
```

---

### Live Screen Simplification

**Problem:** User frustration with ~12 second wait before seeing the stream.

**Cause:**
- 5-second countdown (5...4...3...2...1...)
- 6-second buffer delay after stream starts

**Solution:** Removed countdown and buffer delay entirely.

| Metric | Before | After |
|--------|--------|-------|
| Total wait | ~12 seconds | ~2 seconds |
| Countdown | 5 seconds | Removed |
| Buffer delay | 6 seconds | Removed |

**Removed Code:**
- `_isShowingCountdown` state variable
- `_countdownSeconds` state variable
- `_buildCountdownIndicator()` method
- Countdown for-loop in `_onInit()`
- 6-second `Future.delayed()` after stream start

---

### Playback Screen Performance

#### Problem 1: Dashcam Unresponsive with Parallel Downloads

**Symptom:** Loading indicator stuck forever, dashcam became unresponsive.

**Root Cause:** 5 parallel thumbnail downloads overwhelmed the dashcam's limited HTTP handling capacity.

**Solution:** Reduced to 2 parallel downloads with 100ms delay between batches:

```dart
Future<void> _cacheThumbnailsForVideos(List<F9DashCamTsFile> videoArray) async {
  List<Future<Map<String, String?>>> futures = [];

  for (var video in videoArray) {
    if (_isDisposed) break;

    futures.add(_downloadThumbnailForResult(...));

    // Download only 2 thumbnails in parallel
    if (futures.length >= 2) {
      await Future.wait(futures);
      futures.clear();
      await Future.delayed(Duration(milliseconds: 100));
    }
  }
}
```

| Configuration | Before | After |
|---------------|--------|-------|
| Parallel downloads | 5 | 2 |
| Batch delay | None | 100ms |
| Dashcam stability | Crashes | Stable |

---

#### Problem 2: App Froze/Crashed When Navigating Away

**Symptom:**
- Live screen couldn't connect after leaving playback
- App froze or crashed

**Root Cause:** Background loading continued after user left screen, blocking the dashcam's single-connection limit.

**Solution:** Added `_isDisposed` flag to cancel background operations:

```dart
class _F9DashCamPlaybackListScreenState extends Widget {
  bool _isDisposed = false;

  @override
  void dispose() {
    _isDisposed = true;  // Signal background loading to stop
    f9DashCamApiManager.exitPlayBack();  // Exit playback mode immediately
    super.dispose();
  }

  Future<void> _loadRemainingVideosInBackground() async {
    while (!_isDisposed) {  // Check if widget is still alive
      List<F9DashCamTsFile> batch = await f9DashCamApiManager.getFileList(...);

      if (_isDisposed) break;  // Stop if disposed during API call
      // ... process batch
    }
  }
}
```

---

## 6.2 Trials & Errors Summary

| Issue | Symptom | Root Cause | Solution |
|-------|---------|------------|----------|
| Playhead misalignment | "No videos" shown incorrectly | Buffer width 72% vs 50% comment | Use 51% buffer width |
| Long live screen load | 12+ second wait | Countdown + buffer delay | Remove both delays |
| Dashcam unresponsive | Thumbnails stuck loading | 5 parallel downloads | Reduce to 2 parallel |
| Navigation freeze | Live screen blocked | Background tasks continued | Add _isDisposed flag |
| Seek stuck playback | Infinite buffering | Seek beyond video duration | Clamp position to safe range |
| Player crash | No playback after seek | Improper initialization | Full player disposal + recreation |

---

## 7. Code Architecture

### Project Structure - F9 Dashcam

```
lib/
├── models/                                      # F9 Dashcam Data Models
│   ├── f9_dash_cam_dict.dart                   # API response wrapper
│   ├── f9_dash_cam_dict.g.dart                 # Generated JSON serialization
│   ├── f9_dash_cam_menu.dart                   # Settings menu model
│   ├── f9_dash_cam_menu.g.dart                 # Generated JSON serialization
│   ├── f9_dash_cam_ts_file.dart                # Video file metadata
│   ├── f9_dash_cam_ts_file.g.dart              # Generated JSON serialization
│   └── f9_timeline_clip.dart                   # Timeline clip model
│
├── screens/dash_cam/
│   ├── dash_cam_home_screen.dart               # Dashcam type selector
│   └── f9/                                     # F9 Dashcam Screens
│       ├── f9_dashcam_widget.dart              # WiFi detection & entry
│       ├── f9_dash_cam_live_screen.dart        # Live RTSP streaming
│       ├── f9_dash_cam_playback_list_screen.dart  # Video browser
│       ├── f9_dash_cam_timeline_screen.dart    # Timeline with seeking
│       ├── f9_dash_cam_video_player_screen.dart   # Video playback
│       ├── f9_dash_cam_photo_list_screen.dart  # Photo gallery
│       ├── f9_dash_cam_setting_screen.dart     # Device settings
│       └── f9_image_view_screen.dart           # Photo viewer
│
├── utils/                                       # F9 Utilities
│   ├── f9_dash_cam_api_manager.dart            # HTTP API client
│   └── f9_timeline_helper.dart                 # Timeline processing
│
├── widgets/                                     # Shared Widgets
│   ├── dash_cam_list_tile.dart                 # Navigation item
│   ├── dash_cam_date_chip.dart                 # Date selector
│   ├── dash_cam_playback_grid_widget.dart      # Video grid
│   └── dash_cam_playback_grid_widget_2.dart    # Video grid with thumbnails
│
├── deprecated/
│   └── f9_dash_cam_timeline_screen_old.dart    # Old timeline version
│
└── my_constants.dart                            # F9 constants (baseUrl, ports, colors)

assets/images/
├── F9_dashcam.png                               # Device image
└── F9_dashcam_1.png                             # Device image (alt)

docs/
├── f9_dashcam_complete_learning.md              # This document
├── f9_timeline_detailed_plan.md                 # Timeline plan
└── f9_ylt_code_review_violations.md             # Code review log
```

---

### F9 Screen Details

| Screen | File | Purpose | Lines |
|--------|------|---------|-------|
| Live | `f9_dash_cam_live_screen.dart` | RTSP streaming, camera switch | ~530 |
| Playback | `f9_dash_cam_playback_list_screen.dart` | Video browser, thumbnails | ~540 |
| Timeline | `f9_dash_cam_timeline_screen.dart` | Visual timeline, seeking | ~1600 |
| Video Player | `f9_dash_cam_video_player_screen.dart` | Single video playback | ~650 |
| Photo List | `f9_dash_cam_photo_list_screen.dart` | Photo gallery | ~280 |
| Settings | `f9_dash_cam_setting_screen.dart` | Device settings | ~500 |
| Image View | `f9_image_view_screen.dart` | Photo zoom/pan | ~115 |
| Entry | `f9_dashcam_widget.dart` | WiFi detection | ~250 |

---

### F9 Model Details

| Model | File | Fields |
|-------|------|--------|
| F9DashCamDict | `f9_dash_cam_dict.dart` | folder, count, fileArray |
| F9DashCamMenu | `f9_dash_cam_menu.dart` | paramId, paramValue, paramName |
| F9DashCamTsFile | `f9_dash_cam_ts_file.dart` | name, duration, createtimestr, type |
| F9TimelineClip | `f9_timeline_clip.dart` | filePath, startTime, endTime, duration, type, isFrontCamera, displayColor |

---

### F9 API Manager Methods

```dart
// Device
getDeviceInfo(), getMediaInfo(), getSdInfo(), getBatteryInfo()

// Camera
initializeLiveStream(), switchCamera(index), setRecording(value)

// Files
getFileList(folder, start, end), getPlayBackDocs(), getImageArray()
getThumbnail(filePath), deleteFile(filePath)

// Modes
enterPlayBack(), exitPlayBack(), enterRecorder(), exitRecorder()

// Settings
getParamItems(), getCurrentValueForAllMenuItems(), setParamValue(param, value)

// SD Card
formatSDCard()

// Timeline
getMixedTimelineVideos(), getLoopVideosForDate()
```

---

### F9 Timeline Helper Methods

```dart
buildTimelineClips(videosByFolder)     // Convert API data to clips
groupClipsByDate(clipArray)            // Group by Today/Yesterday/date
buildTimelineWithGaps(clipArray, date) // Create segments with grey gaps
filterByCamera(clipArray, isFront)     // Front/rear filtering
findClipAtTime(clipArray, globalTime)  // Find clip at timestamp
getClipTypeColor(type)                 // Green/Red/Yellow for loop/emr/park
formatDateChip(date)                   // "Today", "Yesterday", "MMM dd"
```

---

### Service Layer: RtspService

**Responsibilities:**
- Manage RTSP player lifecycle
- Handle HTTP API communication
- Track connection state
- Send periodic heartbeats
- Provide debug logging

**Configuration:**
```dart
class RtspConfig {
  static const String baseUrl = 'http://192.168.169.1';
  static const String defaultRtspUrl = 'rtsp://192.168.169.1:554';
  static const int width = 960;
  static const int height = 540;
  static const int fps = 25;
  static const List<String> cameraChannels = ['0', '1'];
  static const List<String> cameraNames = ['Front', 'Rear'];
  static const Duration heartbeatInterval = Duration(seconds: 5);
}
```

**Connection States:**
```dart
enum StreamConnectionStatus {
  disconnected,    // No active connection
  connecting,      // Connection in progress
  connected,       // Successfully streaming
  error,           // Connection failed
}
```

---

### Navigation Architecture

```
Bottom Navigation (IndexedStack)
    ↓
├── Tab 0: Live Stream
│   └── LiveStreamScreen
│       ├── RTSP player (media_kit)
│       ├── Camera selector
│       └── Transport selector
│
├── Tab 1: Playback
│   └── PlaybackListScreen
│       ├── Folder tabs (Loop, EMR, Event, Park)
│       ├── List/Grid view toggle
│       └── File items with thumbnails
│
└── Tab 2: Settings
    └── SettingsScreen
        └── Volume control (0-4 levels)
```

---

## 8. Key Learnings

### 1. API Documentation vs Reality

Generic F9 API documentation showed different structure than actual Vidure implementation.

**Lesson:** Always test with real device responses.

---

### 2. Defensive Programming

Use multiple fallback strategies for parsing:
```dart
// Try format 1
if (condition1) { return value1; }
// Try format 2
if (condition2) { return value2; }
// Try format 3
if (condition3) { return value3; }
// Fallback
return defaultValue;
```

---

### 3. Dashcam Has Modes

Dashcam can only be in ONE mode at a time:
- **LIVE MODE** - Live streaming works
- **PLAYBACK MODE** - File playback works

Must switch modes via API.

---

### 4. Single RTSP Connection Limit

**Critical:** Dashcam CANNOT handle multiple RTSP connections simultaneously.

**What causes crashes:**
- Multiple RTSP connections
- Parallel API calls
- Preloading multiple videos

**Safe approach:**
- ONE RTSP connection at a time
- Dispose before opening new
- Sequential operations only

---

### 5. No Seeking in RTSP

RTSP protocol cannot seek to arbitrary position within video.

**Workaround:** Always play from START of video.
- User scrubs to 11:02
- Video starts from 11:00

---

### 6. Player Choice Matters for Latency

| Player | Latency | Use Case |
|--------|---------|----------|
| media_kit (libmpVLC) | ~6 seconds | File playback |
| IJKPlayer | ~1 second | Live streaming (planned) |

---

### 7. Design Consistency

Use Explore agent to analyze existing code patterns:
- Colors
- Border radius
- Spacing
- Shadows

---

## 9. Troubleshooting Guide

### Issue: Stream won't connect

**Symptoms:** RTSP connection fails or times out

**Solutions:**
1. Check device is powered on
2. Verify WiFi connection to `192.168.169.1`
3. Try calling `/app/enterrecorder` first
4. Switch transport protocol (UDP ↔ TCP)
5. Check debug logs

---

### Issue: Files not showing

**Symptoms:** "No files found" in file browser

**Solutions:**
1. Check SD card is inserted
2. Check API response in debug log
3. Verify JSON parsing (nested structure)
4. Check folder parameter (`loop`, `emr`, etc.)

---

### Issue: Video won't play

**Symptoms:** Video playback fails

**Solutions:**
1. **Check playback mode** - Must call `/app/playback?param=enter` first
2. Verify RTSP URL format: `rtsp://192.168.169.1:554{filepath}`
3. Check file exists on SD card
4. Verify player is properly initialized

---

### Issue: Camera switch doesn't work

**Symptoms:** Camera won't switch from front to rear

**Solutions:**
1. Check API call: `/app/setparamvalue?param=switchcam&value=1`
2. Disconnect current stream first
3. Wait 500ms after API call
4. Reconnect to same URL (camera changed on dashcam side)

---

### Issue: Live stream interrupted after playback

**Symptoms:** Live stream won't work after playing video

**Solution:**
```dart
// CRITICAL: Always exit playback mode
@override
void dispose() {
  _exitPlaybackMode();  // Don't forget!
  super.dispose();
}
```

---

### Issue: 6-second delay in live stream

**Symptoms:** Live stream has significant latency

**Current Status:** UNSOLVED

**Attempted (all failed):**
- Switch to UDP (dashcam forces TCP)
- Disable audio
- Reduce buffer size
- Low-delay flags

**Future Plan:** Implement IJKPlayer for ~1 second latency

---

## 9.5. Reusable Patterns (April 2026 Refinements)

### Pattern 1: Rate-Limiting for Embedded Devices

**Problem:** Dashcam became unresponsive when app made 5 parallel thumbnail downloads.

**Solution:**
- Limit concurrent requests to 2 maximum
- Add 100ms delay between batches
- Embedded devices (dashcams) have limited processing power

**Use when:** Making multiple API calls to embedded devices

---

### Pattern 2: Safe Dispose for Async Operations

**Problem:** Background video loading continued after user navigated away, blocking dashcam from accepting new connections.

**Solution:**
- Add `_isDisposed` boolean flag
- Set to true in dispose() method
- Check in all background loops: `while (!_isDisposed)`
- Call API exit method to cancel pending operations

**Use when:** Widget has background async operations that must stop on navigate

---

### Pattern 3: RTSP Seeking Best Practices

**Problem:** Seeking beyond video duration caused infinite buffering.

**Solution:**
- Clamp position to safe range: `0 to duration - 1` (prevents seeking beyond content)
- Buffer 2 seconds before seeking (RTSP needs time to establish stream)
- Buffer 500ms after seek before playback

**Use when:** Implementing seek functionality for RTSP live streams

---

### Pattern 4: Smart Auto-Advance Logic

**Problem:** Videos auto-advanced even when user was scrubbing or paused at end.

**Solution:**
- Only advance when ALL conditions met: playing, not dragging slider, near end
- Check `_isDraggingSlider` to prevent advance during scrubbing
- Check `_isPlaying` to prevent advance when paused

**Use when:** Auto-advancing videos in a playlist/timeline

---

## 10. Quick Reference

### Common RTSP URLs

```
# Live streams (single URL, camera switched via API)
rtsp://192.168.169.1:554

# File playback
rtsp://192.168.169.1:554/mnt/card/video_front/20260311_170927_f.ts
```

### Common API Endpoints

| Action         | Endpoint                                               |
|----------------|--------------------------------------------------------|
| Enter recorder | `GET /app/enterrecorder`                               |
| Exit recorder  | `GET /app/exitrecorder`                                |
| Switch camera  | `GET /app/setparamvalue?param=switchcam&value={0,1}` |
| Get file list  | `GET /app/getfilelist?folder={type}&start={n}&end={n}` |
| Get thumbnail  | `GET /app/getthumbnail?file={path}` |
| Enter playback | `GET /app/playback?param=enter`     |
| Exit playback  | `GET /app/playback?param=exit`      |
| Get heartbeat  | `GET /app/getparamvalue?param=rec`  |

### Folder Types

| Display   | API Value |
|-----------|-----------|
| Loop      | `loop`    |
| Emergency | `emr`     |
| Event     | `event`   |
| Parking   | `park`    |

### Camera Values

| Camera | Value |
|--------|-------|
| Front  | `0`   |
| Rear   | `1`   |

### Volume Levels

| Level     | Value |
|-----------|-------|
| Off       | `0`   |
| Low       | `1`   |
| Medium    | `2`   |
| High      | `3`   |
| Very High | `4`   |

---

## Conclusion

This F9 Dashcam implementation journey involved:

- **9 major failure stories** with detailed solutions
- **6 significant successes** and working implementations
- **Complete API reference** for all dashcam operations
- **Network architecture** understanding
- **Code architecture** and patterns

### Key Takeaways

1. **Test with real hardware** - Documentation differs from reality
2. **Defensive programming** - Multiple fallbacks make robust code
3. **Single connection limit** - Dashcam crashes with multiple RTSP
4. **Mode switching required** - Live vs Playback modes
5. **Design consistency** - Match existing app patterns



---

**Document Version:** 1.2
**Last Updated:** April 2, 2026
