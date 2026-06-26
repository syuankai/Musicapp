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

    var player: ExoPlayer? = null
        private set

    private var progressJob: Job? = null
    private var lastPosition: Long = 0L

    fun initPlayer(context: Context) {
        if (player != null) return
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

        val newPlayer = ExoPlayer.Builder(context.applicationContext, renderersFactory).build().apply {
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
                } else {
                    stopProgressTracker()
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

        // Reload the current item if we have one and restore position
        _currentMediaItem.value?.let { item ->
            val mediaItem = if (item.isLocal && item.localUri != null) {
                MediaItem.fromUri(item.localUri)
            } else {
                MediaItem.fromUri(item.url)
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

        // Save progress to restore
        player?.let { p ->
            lastPosition = p.currentPosition
            p.release()
        }
        player = null
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

        if (mediaItem.id.startsWith("youtube_")) {
            _isResolvingYoutube.value = true
            _youtubeResolveError.value = null
            viewModelScope.launch {
                val videoId = mediaItem.id.removePrefix("youtube_")
                val isVideoMode = mediaItem.isVideo
                val extracted = YoutubeExtractor.fetchStream(videoId, isVideoMode)
                _isResolvingYoutube.value = false
                if (extracted != null) {
                    val resolvedItem = mediaItem.copy(url = extracted.streamUrl)
                    _currentMediaItem.value = resolvedItem
                    player?.let { p ->
                        p.setMediaItem(MediaItem.fromUri(extracted.streamUrl))
                        p.prepare()
                        p.play()
                    }
                } else {
                    _youtubeResolveError.value = "解析 YouTube 失敗，請重試"
                }
            }
        } else {
            _isResolvingYoutube.value = false
            _youtubeResolveError.value = null
            player?.let { p ->
                val m3Item = if (mediaItem.isLocal && mediaItem.localUri != null) {
                    MediaItem.fromUri(mediaItem.localUri)
                } else {
                    MediaItem.fromUri(mediaItem.url)
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

                // Play the newly added item
                playItem(newItem, updatedList.size - 1)
                onSuccess()
            } else {
                _youtubeResolveError.value = "解析失敗，請檢查網路或更換連結"
                onError("解析失敗，請檢查網路或更換連結")
            }
        }
    }

    fun playPause() {
        player?.let { p ->
            if (p.isPlaying) {
                p.pause()
            } else {
                p.prepare()
                p.play()
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
    }

    fun toggleMute() {
        val isCurrentlyMuted = _isMuted.value
        _isMuted.value = !isCurrentlyMuted
        player?.volume = if (!isCurrentlyMuted) 0.0f else 1.0f
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

        // Immediately play the added file
        playItem(newLocalItem, updatedList.size - 1)
    }

    fun removePlaylistItem(index: Int) {
        val updatedList = _playlist.value.toMutableList()
        if (index in updatedList.indices) {
            updatedList.removeAt(index)
            _playlist.value = updatedList

            // If we deleted the active item, adjust index or stop
            if (index == _currentIndex.value) {
                if (updatedList.isNotEmpty()) {
                    val nextIdx = index.coerceAtMost(updatedList.size - 1)
                    playItem(updatedList[nextIdx], nextIdx)
                } else {
                    _currentMediaItem.value = null
                    player?.stop()
                    _isPlaying.value = false
                }
            } else if (index < _currentIndex.value) {
                _currentIndex.value -= 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
        stopProgressTracker()
    }
}
