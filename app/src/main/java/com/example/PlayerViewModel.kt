package com.example

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RgbColor(val r: Int, val g: Int, val b: Int)

data class MediaItemModel(
    val id: String,
    val title: String,
    val artist: String,
    val url: String,
    val isVideo: Boolean,
    val isLocal: Boolean,
    val localUri: Uri? = null
)

val DEMO_PLAYLIST = listOf(
    MediaItemModel(
        id = "audio_1",
        title = "Neon Horizon (Synthwave)",
        artist = "SoundHelix Collective",
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        isVideo = false,
        isLocal = false
    ),
    MediaItemModel(
        id = "audio_2",
        title = "Retro Lofi Beats (Chillhop)",
        artist = "SoundHelix Collective",
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        isVideo = false,
        isLocal = false
    ),
    MediaItemModel(
        id = "video_1",
        title = "Big Buck Bunny (MP4 H.264)",
        artist = "Blender Open Source",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        isVideo = true,
        isLocal = false
    ),
    MediaItemModel(
        id = "video_2",
        title = "Elephants Dream (MP4 Video)",
        artist = "Blender Project",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        isVideo = true,
        isLocal = false
    ),
    MediaItemModel(
        id = "video_3",
        title = "Sintel Cinematic HD",
        artist = "Blender Foundation",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        isVideo = true,
        isLocal = false
    )
)

@OptIn(UnstableApi::class)
class PlayerViewModel : ViewModel() {

    companion object {
        var activeSession: androidx.media3.session.MediaSession? = null
    }

    private val _playlist = MutableStateFlow<List<MediaItemModel>>(DEMO_PLAYLIST)
    val playlist: StateFlow<List<MediaItemModel>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItemModel?>(DEMO_PLAYLIST[0])
    val currentMediaItem: StateFlow<MediaItemModel?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Decoders Config: "default", "hardware", "software"
    private val _preferDecoderType = MutableStateFlow("default")
    val preferDecoderType: StateFlow<String> = _preferDecoderType.asStateFlow()

    private val _activeVideoDecoder = MutableStateFlow<String?>(null)
    val activeVideoDecoder: StateFlow<String?> = _activeVideoDecoder.asStateFlow()

    private val _activeAudioDecoder = MutableStateFlow<String?>(null)
    val activeAudioDecoder: StateFlow<String?> = _activeAudioDecoder.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // YouTube Search States
    private val _youtubeSearchResults = MutableStateFlow<List<YoutubeSearchResult>>(emptyList())
    val youtubeSearchResults: StateFlow<List<YoutubeSearchResult>> = _youtubeSearchResults.asStateFlow()

    private val _isSearchingYoutube = MutableStateFlow(false)
    val isSearchingYoutube: StateFlow<Boolean> = _isSearchingYoutube.asStateFlow()

    private val _youtubeSearchError = MutableStateFlow<String?>(null)
    val youtubeSearchError: StateFlow<String?> = _youtubeSearchError.asStateFlow()

    private val _activeAmbientColor = MutableStateFlow(RgbColor(30, 30, 45))
    val activeAmbientColor: StateFlow<RgbColor> = _activeAmbientColor.asStateFlow()

    private val _isResolvingYoutube = MutableStateFlow(false)
    val isResolvingYoutube: StateFlow<Boolean> = _isResolvingYoutube.asStateFlow()

    private val _youtubeResolveError = MutableStateFlow<String?>(null)
    val youtubeResolveError: StateFlow<String?> = _youtubeResolveError.asStateFlow()

    private val _backgroundType = MutableStateFlow("gradient") // "gradient" or "simple"
    val backgroundType: StateFlow<String> = _backgroundType.asStateFlow()

    private var appContext: Context? = null

    // Cache for YouTube stream URL: videoId_isVideoMode -> (streamUrl, timestamp)
    private val youtubeUrlCache = mutableMapOf<String, Pair<String, Long>>()
    // Cache for SoundCloud stream URL: transcodingUrl -> (playableUrl, timestamp)
    private val soundcloudUrlCache = mutableMapOf<String, Pair<String, Long>>()

    private val CACHE_EXPIRY_MS = 3600_000L // 1 hour cache expiry

    private fun saveSetting(key: String, value: Any) {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
        }
        editor.apply()
    }

    private fun savePlaylistToPrefs() {
        val context = appContext ?: return
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val array = org.json.JSONArray()
        for (item in _playlist.value) {
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("artist", item.artist)
                put("url", item.url)
                put("isVideo", item.isVideo)
                put("isLocal", item.isLocal)
                if (item.localUri != null) {
                    put("localUri", item.localUri.toString())
                }
            }
            array.put(obj)
        }
        prefs.edit().putString("playlist", array.toString()).apply()
    }

    private fun loadSettingsFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        _preferDecoderType.value = prefs.getString("decoder_type", "default") ?: "default"
        _playbackSpeed.value = prefs.getFloat("playback_speed", 1.0f)
        _isMuted.value = prefs.getBoolean("is_muted", false)
        _backgroundType.value = prefs.getString("background_type", "gradient") ?: "gradient"
        _selectedWallpaper.value = prefs.getString("selected_wallpaper", "default") ?: "default"

        // Load playlist
        val jsonStr = prefs.getString("playlist", null)
        if (jsonStr != null) {
            try {
                val array = org.json.JSONArray(jsonStr)
                val list = mutableListOf<MediaItemModel>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val title = obj.getString("title")
                    val artist = obj.getString("artist")
                    val url = obj.getString("url")
                    val isVideo = obj.getBoolean("isVideo")
                    val isLocal = obj.getBoolean("isLocal")
                    val localUriStr = obj.optString("localUri", null)
                    val localUri = if (localUriStr != null) Uri.parse(localUriStr) else null
                    list.add(MediaItemModel(id, title, artist, url, isVideo, isLocal, localUri))
                }
                _playlist.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load index
        val savedIndex = prefs.getInt("current_index", 0)
        if (savedIndex in _playlist.value.indices) {
            _currentIndex.value = savedIndex
            _currentMediaItem.value = _playlist.value[savedIndex]
        } else if (_playlist.value.isNotEmpty()) {
            _currentIndex.value = 0
            _currentMediaItem.value = _playlist.value[0]
        }
    }

    fun setBackgroundType(type: String) {
        _backgroundType.value = type
        saveSetting("background_type", type)
    }

    private val _selectedWallpaper = MutableStateFlow<String>("default")
    val selectedWallpaper: StateFlow<String> = _selectedWallpaper.asStateFlow()

    fun setSelectedWallpaper(wallpaper: String) {
        _selectedWallpaper.value = wallpaper
        saveSetting("selected_wallpaper", wallpaper)
    }

    var player: ExoPlayer? = null
        private set

    var mediaSession: androidx.media3.session.MediaSession? = null
        private set

    private fun createMediaItem(model: MediaItemModel, url: String): MediaItem {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(model.title)
            .setArtist(model.artist)
            .build()
        
        val isHls = url.contains(".m3u8") || 
                     url.contains("/hls") || 
                     (model.id.startsWith("soundcloud_") && (_playlist.value.find { it.id == model.id }?.url?.contains("/hls") == true))
        
        val builder = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            
        if (isHls) {
            builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }
        
        return builder.build()
    }

    private var progressJob: Job? = null
    private var lastPosition: Long = 0L

    fun initPlayer(context: Context) {
        appContext = context.applicationContext
        if (player != null) return
        loadSettingsFromPrefs(context.applicationContext)
        setupPlayerInstance(context)
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayerInstance(context: Context) {
        val type = _preferDecoderType.value

        // Setup custom selector for renderers factory to select decoders
        val renderersFactory = DefaultRenderersFactory(context.applicationContext).apply {
            setMediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                val defaultList = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)
                when (type) {
                    "hardware" -> {
                        defaultList.sortedWith(compareByDescending { info ->
                            info.hardwareAccelerated && !info.softwareOnly
                        })
                    }
                    "software" -> {
                        defaultList.sortedWith(compareByDescending { info ->
                            info.softwareOnly
                        })
                    }
                    else -> defaultList
                }
            }
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30000, // minBufferMs
                60000, // maxBufferMs
                1500,  // bufferForPlaybackMs
                3000   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            context.applicationContext,
            httpDataSourceFactory
        )

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
            dataSourceFactory
        )

        val newPlayer = ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = if (_isMuted.value) 0.0f else 1.0f
                setPlaybackSpeed(_playbackSpeed.value)
            }

        // Add Listeners
        newPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                    activeSession = mediaSession
                    appContext?.let { ctx ->
                        try {
                            val serviceIntent = android.content.Intent(ctx, MediaPlaybackService::class.java).apply {
                                action = "START"
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                ctx.startForegroundService(serviceIntent)
                            } else {
                                ctx.startService(serviceIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    stopProgressTracker()
                    appContext?.let { ctx ->
                        try {
                            val serviceIntent = android.content.Intent(ctx, MediaPlaybackService::class.java).apply {
                                action = "START"
                            }
                            ctx.startService(serviceIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = state
                if (state == Player.STATE_READY) {
                    _duration.value = newPlayer.duration.coerceAtLeast(0L)
                } else if (state == Player.STATE_ENDED) {
                    playNext()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentItem = _currentMediaItem.value ?: return
                val currentPos = player?.currentPosition ?: 0L

                if (currentItem.id.startsWith("youtube_") || currentItem.id.startsWith("soundcloud_")) {
                    if (currentItem.id.startsWith("youtube_")) {
                        val videoId = currentItem.id.removePrefix("youtube_")
                        val isVideoMode = currentItem.isVideo
                        youtubeUrlCache.remove("${videoId}_$isVideoMode")
                    } else {
                        soundcloudUrlCache.remove(currentItem.url)
                    }

                    _youtubeResolveError.value = "偵測到串流失效，正在自動重試解析新位址..."
                    _isResolvingYoutube.value = true

                    viewModelScope.launch {
                        if (currentItem.id.startsWith("youtube_")) {
                            val videoId = currentItem.id.removePrefix("youtube_")
                            val isVideoMode = currentItem.isVideo
                            val extracted = YoutubeExtractor.fetchStream(videoId, isVideoMode)
                            _isResolvingYoutube.value = false
                            if (extracted != null) {
                                youtubeUrlCache["${videoId}_$isVideoMode"] = Pair(extracted.streamUrl, System.currentTimeMillis())
                                val resolvedItem = currentItem.copy(url = extracted.streamUrl)
                                _currentMediaItem.value = resolvedItem
                                player?.let { p ->
                                    p.setMediaItem(createMediaItem(resolvedItem, extracted.streamUrl), currentPos)
                                    p.prepare()
                                    p.play()
                                }
                            } else {
                                _youtubeResolveError.value = "自動重試解析 YouTube 失敗"
                            }
                        } else if (currentItem.id.startsWith("soundcloud_")) {
                            val originalItem = _playlist.value.find { it.id == currentItem.id }
                            val transcodingUrl = originalItem?.url ?: currentItem.url
                            val playableUrl = SoundCloudExtractor.fetchPlayableUrl(transcodingUrl)
                            _isResolvingYoutube.value = false
                            if (playableUrl != null) {
                                soundcloudUrlCache[transcodingUrl] = Pair(playableUrl, System.currentTimeMillis())
                                val resolvedItem = currentItem.copy(url = playableUrl)
                                _currentMediaItem.value = resolvedItem
                                player?.let { p ->
                                    p.setMediaItem(createMediaItem(resolvedItem, playableUrl), currentPos)
                                    p.prepare()
                                    p.play()
                                }
                            } else {
                                _youtubeResolveError.value = "自動重試解析 SoundCloud 失敗"
                            }
                        }
                    }
                }
            }
        })

        // Add Analytics listener to extract exact decoder names in real-time
        newPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedMs: Long,
                initializationDurationMs: Long
            ) {
                _activeVideoDecoder.value = decoderName
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedMs: Long,
                initializationDurationMs: Long
            ) {
                _activeAudioDecoder.value = decoderName
            }
        })

        player = newPlayer

        try {
            mediaSession?.release()
            val session = androidx.media3.session.MediaSession.Builder(context.applicationContext, newPlayer).build()
            mediaSession = session
            activeSession = session
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Reload the current item if we have one and restore position
        _currentMediaItem.value?.let { item ->
            val mediaItem = if (item.isLocal && item.localUri != null) {
                createMediaItem(item, item.localUri.toString())
            } else {
                createMediaItem(item, item.url)
            }
            newPlayer.setMediaItem(mediaItem, lastPosition)
            newPlayer.prepare()
            if (_isPlaying.value) {
                newPlayer.play()
            }
        }
    }

    fun setDecoderPreference(context: Context, type: String) {
        if (_preferDecoderType.value == type) return
        _preferDecoderType.value = type
        saveSetting("decoder_type", type)

        // Save progress to restore
        player?.let { p ->
            lastPosition = p.currentPosition
            p.release()
        }
        player = null
        mediaSession?.release()
        mediaSession = null
        _activeVideoDecoder.value = null
        _activeAudioDecoder.value = null

        // Recreate with new codec preference
        setupPlayerInstance(context)
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = viewModelScope.launch {
            while (true) {
                player?.let { p ->
                    _currentTime.value = p.currentPosition
                    _duration.value = p.duration.coerceAtLeast(0L)
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playItem(mediaItem: MediaItemModel, index: Int) {
        _currentIndex.value = index
        _currentMediaItem.value = mediaItem
        _activeVideoDecoder.value = null
        _activeAudioDecoder.value = null
        lastPosition = 0L
        saveSetting("current_index", index)

        if (mediaItem.id.startsWith("youtube_")) {
            _isResolvingYoutube.value = true
            _youtubeResolveError.value = null
            viewModelScope.launch {
                val videoId = mediaItem.id.removePrefix("youtube_")
                val isVideoMode = mediaItem.isVideo
                val cacheKey = "${videoId}_$isVideoMode"
                val cached = youtubeUrlCache[cacheKey]
                val now = System.currentTimeMillis()

                val streamUrl = if (cached != null && (now - cached.second) < CACHE_EXPIRY_MS) {
                    cached.first
                } else {
                    val extracted = YoutubeExtractor.fetchStream(videoId, isVideoMode)
                    if (extracted != null) {
                        youtubeUrlCache[cacheKey] = Pair(extracted.streamUrl, now)
                        extracted.streamUrl
                    } else null
                }

                _isResolvingYoutube.value = false
                if (streamUrl != null) {
                    val resolvedItem = mediaItem.copy(url = streamUrl)
                    _currentMediaItem.value = resolvedItem
                    player?.let { p ->
                        p.setMediaItem(createMediaItem(resolvedItem, streamUrl))
                        p.prepare()
                        p.play()
                    }
                } else {
                    _youtubeResolveError.value = "解析 YouTube 失敗，請重試"
                }
            }
        } else if (mediaItem.id.startsWith("soundcloud_")) {
            _isResolvingYoutube.value = true
            _youtubeResolveError.value = null
            viewModelScope.launch {
                val cached = soundcloudUrlCache[mediaItem.url]
                val now = System.currentTimeMillis()

                val playableUrl = if (cached != null && (now - cached.second) < CACHE_EXPIRY_MS) {
                    cached.first
                } else {
                    val resolvedUrl = SoundCloudExtractor.fetchPlayableUrl(mediaItem.url)
                    if (resolvedUrl != null) {
                        soundcloudUrlCache[mediaItem.url] = Pair(resolvedUrl, now)
                        resolvedUrl
                    } else null
                }

                _isResolvingYoutube.value = false
                if (playableUrl != null) {
                    val resolvedItem = mediaItem.copy(url = playableUrl)
                    _currentMediaItem.value = resolvedItem
                    player?.let { p ->
                        p.setMediaItem(createMediaItem(resolvedItem, playableUrl))
                        p.prepare()
                        p.play()
                    }
                } else {
                    _youtubeResolveError.value = "解析 SoundCloud 串流連結失敗"
                }
            }
        } else {
            _isResolvingYoutube.value = false
            _youtubeResolveError.value = null
            player?.let { p ->
                val m3Item = if (mediaItem.isLocal && mediaItem.localUri != null) {
                    createMediaItem(mediaItem, mediaItem.localUri.toString())
                } else {
                    createMediaItem(mediaItem, mediaItem.url)
                }
                p.setMediaItem(m3Item)
                p.prepare()
                p.play()
            }
        }
    }

    fun addYoutubeItem(
        urlOrId: String,
        isVideoMode: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val videoId = YoutubeExtractor.extractVideoId(urlOrId)
        if (videoId == null) {
            onError("無效的 YouTube 網址或 ID")
            return
        }

        _isResolvingYoutube.value = true
        _youtubeResolveError.value = null
        viewModelScope.launch {
            val extracted = YoutubeExtractor.fetchStream(videoId, isVideoMode)
            _isResolvingYoutube.value = false
            if (extracted != null) {
                val newItem = MediaItemModel(
                    id = "youtube_$videoId",
                    title = extracted.title,
                    artist = extracted.artist,
                    url = extracted.streamUrl,
                    isVideo = isVideoMode,
                    isLocal = false
                )
                val updatedList = _playlist.value.toMutableList()
                updatedList.add(newItem)
                _playlist.value = updatedList
                savePlaylistToPrefs()

                // Play the newly added item
                playItem(newItem, updatedList.size - 1)
                onSuccess()
            } else {
                _youtubeResolveError.value = "解析失敗，請檢查網路或更換連結"
                onError("解析失敗，請檢查網路或更換連結")
            }
        }
    }

    fun addSoundCloudItem(
        url: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!SoundCloudExtractor.isSoundCloudUrl(url)) {
            onError("無效的 SoundCloud 連結")
            return
        }
        _isResolvingYoutube.value = true
        _youtubeResolveError.value = null
        viewModelScope.launch {
            val tracks = SoundCloudExtractor.resolve(url)
            _isResolvingYoutube.value = false
            if (tracks.isNotEmpty()) {
                val updatedList = _playlist.value.toMutableList()
                var firstAddedIdx = -1
                tracks.forEach { track ->
                    val newItem = MediaItemModel(
                        id = track.id,
                        title = track.title,
                        artist = track.artist,
                        url = track.streamUrl,
                        isVideo = false,
                        isLocal = false
                    )
                    updatedList.add(newItem)
                    if (firstAddedIdx == -1) {
                        firstAddedIdx = updatedList.size - 1
                    }
                }
                _playlist.value = updatedList
                savePlaylistToPrefs()

                // Play the first newly added item
                if (firstAddedIdx != -1) {
                    playItem(updatedList[firstAddedIdx], firstAddedIdx)
                }
                onSuccess()
            } else {
                _youtubeResolveError.value = "解析 SoundCloud 失敗，請確認網址或網路連線"
                onError("解析 SoundCloud 失敗，請確認網址或網路連線")
            }
        }
    }

    fun playPause() {
        player?.let { p ->
            if (p.isPlaying) {
                p.pause()
            } else {
                val currentItem = _currentMediaItem.value
                if (currentItem != null && (currentItem.id.startsWith("youtube_") || currentItem.id.startsWith("soundcloud_"))) {
                    // Re-resolve and play to handle potentially expired streams on resume
                    playItem(currentItem, _currentIndex.value)
                } else {
                    p.prepare()
                    p.play()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.let { p ->
            p.seekTo(positionMs)
            _currentTime.value = positionMs
        }
    }

    fun skipForward() {
        player?.let { p ->
            val newPos = (p.currentPosition + 10000).coerceAtMost(p.duration)
            seekTo(newPos)
        }
    }

    fun skipBackward() {
        player?.let { p ->
            val newPos = (p.currentPosition - 10000).coerceAtLeast(0)
            seekTo(newPos)
        }
    }

    fun playNext() {
        val currentList = _playlist.value
        if (currentList.isEmpty()) return
        val nextIndex = (_currentIndex.value + 1) % currentList.size
        playItem(currentList[nextIndex], nextIndex)
    }

    fun playPrevious() {
        val currentList = _playlist.value
        if (currentList.isEmpty()) return
        var prevIndex = _currentIndex.value - 1
        if (prevIndex < 0) prevIndex = currentList.size - 1
        playItem(currentList[prevIndex], prevIndex)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player?.setPlaybackSpeed(speed)
        saveSetting("playback_speed", speed)
    }

    fun toggleMute() {
        val isCurrentlyMuted = _isMuted.value
        val newMute = !isCurrentlyMuted
        _isMuted.value = newMute
        player?.volume = if (newMute) 0.0f else 1.0f
        saveSetting("is_muted", newMute)
    }

    fun searchYoutube(query: String) {
        if (query.isBlank()) {
            _youtubeSearchResults.value = emptyList()
            return
        }
        _isSearchingYoutube.value = true
        _youtubeSearchError.value = null
        viewModelScope.launch {
            try {
                val results = YoutubeExtractor.search(query)
                _youtubeSearchResults.value = results
                if (results.isEmpty()) {
                    _youtubeSearchError.value = "查無搜尋結果"
                }
            } catch (e: Exception) {
                _youtubeSearchError.value = "搜尋出錯，請重試"
            } finally {
                _isSearchingYoutube.value = false
            }
        }
    }

    fun updateAmbientColor(r: Int, g: Int, b: Int) {
        _activeAmbientColor.value = RgbColor(r, g, b)
    }

    fun addLocalFile(uri: Uri, fileName: String, isVideo: Boolean) {
        val newLocalItem = MediaItemModel(
            id = "local_${System.currentTimeMillis()}",
            title = fileName,
            artist = "Local File",
            url = uri.toString(),
            isVideo = isVideo,
            isLocal = true,
            localUri = uri
        )
        val updatedList = _playlist.value.toMutableList()
        updatedList.add(newLocalItem)
        _playlist.value = updatedList
        savePlaylistToPrefs()

        // Immediately play the added file
        playItem(newLocalItem, updatedList.size - 1)
    }

    fun removePlaylistItem(index: Int) {
        val updatedList = _playlist.value.toMutableList()
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _playlist.value = updatedList
            savePlaylistToPrefs()

            // If we deleted the active item, adjust index or stop
            if (index == _currentIndex.value) {
                if (updatedList.isNotEmpty()) {
                    val nextIdx = index.coerceAtMost(updatedList.size - 1)
                    playItem(updatedList[nextIdx], nextIdx)
                } else {
                    _currentMediaItem.value = null
                    player?.stop()
                    _isPlaying.value = false
                    saveSetting("current_index", 0)
                }
            } else if (index < _currentIndex.value) {
                _currentIndex.value -= 1
                saveSetting("current_index", _currentIndex.value)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appContext?.let { ctx ->
            try {
                val serviceIntent = android.content.Intent(ctx, MediaPlaybackService::class.java).apply {
                    action = "STOP"
                }
                ctx.startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeSession = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        stopProgressTracker()
    }
}
