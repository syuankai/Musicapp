package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.sin
import android.view.TextureView
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                GlassPlayerApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassPlayerApp(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    
    // Initialize player on startup
    LaunchedEffect(Unit) {
        viewModel.initPlayer(context)
    }

    // Load states from ViewModel
    val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val preferDecoderType by viewModel.preferDecoderType.collectAsState()
    val activeVideoDecoder by viewModel.activeVideoDecoder.collectAsState()
    val activeAudioDecoder by viewModel.activeAudioDecoder.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val backgroundType by viewModel.backgroundType.collectAsState()
    val selectedWallpaper by viewModel.selectedWallpaper.collectAsState()

    var showCodecListSheet by remember { mutableStateOf(false) }
    var videoScaleMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showSettingsPage by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var selectedPlaylistTab by remember { mutableStateOf("all") }

    // Media picking launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            // Guess file format from name
            val isVideo = fileName.endsWith(".mp4", ignoreCase = true) || 
                          fileName.endsWith(".mkv", ignoreCase = true) || 
                          fileName.endsWith(".mov", ignoreCase = true)
            viewModel.addLocalFile(it, fileName, isVideo)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Root Wallpaper (Supporting both gradient and simple solid dark mode)
        if (backgroundType == "simple") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F111A))
            )
        } else {
            val bgResource = when (selectedWallpaper) {
                "cyberpunk" -> R.drawable.wp_cyberpunk_1782527624166
                "cosmic" -> R.drawable.wp_cosmic_1782527636903
                "retro" -> R.drawable.wp_retro_1782527649980
                else -> R.drawable.img_glass_background_1782480821595
            }
            Image(
                painter = painterResource(id = bgResource),
                contentDescription = "Background Gradient",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Semi-transparent overlay to ensure premium readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (backgroundType == "simple") 0.15f else 0.45f))
        )

        // Main Scaffold layout
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Spacer matching system navigation bar height for safe paddings
                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(8.dp)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Panel
                HeaderRow(
                    onImportClick = { filePickerLauncher.launch("*/*") },
                    onSettingsClick = { showSettingsPage = !showSettingsPage }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable main body
                if (showSettingsPage) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Settings Back Navigation and Info card
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showSettingsPage = false },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "系統設定 Settings",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "設定解碼分配、視覺主題與探索解碼晶片",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // 2. Decoder Preference Configuration
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "解碼並用設定 (Codec Engine)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "變更解碼晶片調配，優先調用所選的硬體加速或軟體解碼晶片",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                ) {
                                    val options = listOf("default" to "系統預設", "hardware" to "硬體加速", "software" to "軟體解碼")
                                    options.forEach { (type, label) ->
                                        val isSelected = preferDecoderType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                                )
                                                .clickable { viewModel.setDecoderPreference(context, type) }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.testTag("decoder_pref_$type")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2.5 Active Decoders Info Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperMode,
                                        contentDescription = "Decoder Tech",
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "使用中解碼晶片 (Active MediaCodec)",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "此為系統核心在播放目前影音串流時，實際調用並激活的解碼硬體或軟體模組",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp, top = 2.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    val audioDec = activeAudioDecoder?.let { DecoderHelper.getFriendlyName(it) } ?: if (playbackState == Player.STATE_IDLE) "無" else "載入中..."
                                    val videoDec = activeVideoDecoder?.let { DecoderHelper.getFriendlyName(it) } ?: if (currentMediaItem?.isVideo == true) "載入中..." else "無 (純音訊檔)"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("音訊解碼:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Text(audioDec, color = Color(0xFFEFB8C8), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("active_audio_decoder"))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("視訊解碼:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Text(videoDec, color = Color(0xFFD0BCFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("active_video_decoder"))
                                    }
                                }
                            }
                        }

                        // 3. Background Theme Style Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "背景視覺風格 (Background Style)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "選擇您偏好的播放器背景，兩者皆支援磨砂毛玻璃(Blur)動態色彩模糊質感",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                ) {
                                    val bgOptions = listOf("gradient" to "炫彩漸層", "simple" to "極簡深灰")
                                    bgOptions.forEach { (type, label) ->
                                        val isSelected = backgroundType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                                )
                                                .clickable { viewModel.setBackgroundType(type) }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3.5 Wallpaper Selector Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "自訂桌布壁紙 (Custom Wallpapers)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "選擇您喜愛的極致視覺桌布（將套用於炫彩漸層背景模式下）",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val wallpapers = listOf(
                                        "default" to "預設流光",
                                        "cyberpunk" to "賽博朋克霓虹",
                                        "cosmic" to "浩瀚星際星雲",
                                        "retro" to "復古霓虹合成波"
                                    )
                                    wallpapers.forEach { (wpKey, wpLabel) ->
                                        val isWpSelected = selectedWallpaper == wpKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    BorderStroke(1.dp, if (isWpSelected) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.12f)),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isWpSelected) Color(0xFFD0BCFF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.02f))
                                                .clickable { viewModel.setSelectedWallpaper(wpKey) }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Movie,
                                                    contentDescription = wpLabel,
                                                    tint = if (isWpSelected) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = wpLabel,
                                                    color = if (isWpSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isWpSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                            if (isWpSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFFD0BCFF), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Tech codec database card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "核心解碼晶片庫 (Decoder Library)",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "查看此 Android 裝置核心所支援的全部 MediaCodec 硬體與軟體解碼晶片清單",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Button(
                                    onClick = { showCodecListSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hardware,
                                        contentDescription = "Codecs",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "開啟本機解碼晶片清單",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 5. About App Card
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "關於 Aero Glass Player",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "版本：v1.2.0\n獨特磨砂毛玻璃質感的多媒體播放器，專為 Android 打造。搭載本機高畫質解碼優先級管理，並內建 YouTube 及 SoundCloud 免下載高速雲端串流解析引擎。",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    
                    // Main Media Screen (Audio Visualizer or Video Surface with cinematic ambient mode)
                    item {
                        val activeAmbientColor by viewModel.activeAmbientColor.collectAsState()

                        // Build a beautiful flowing gradient composable for ambient glow backdrop
                        val ambientColorCompose = Color(
                            red = activeAmbientColor.r / 255f,
                            green = activeAmbientColor.g / 255f,
                            blue = activeAmbientColor.b / 255f,
                            alpha = 0.45f
                        )

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            if (currentMediaItem?.isVideo == true && viewModel.player != null) {
                                // MP4 Video Renderer Area with dynamic backing glow
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (isFullScreen) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "全螢幕播放中...",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        // 1. Cinematic Background glow matching active video pixels
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(16.dp)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(ambientColorCompose, Color.Transparent),
                                                        radius = 350f
                                                    )
                                                )
                                        )

                                        var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

                                        // Real-time video frame pixel analysis
                                        LaunchedEffect(isPlaying, currentMediaItem) {
                                            while (isPlaying) {
                                                delay(180) // Analysis interval
                                                textureViewRef?.let { tv ->
                                                    try {
                                                        val bitmap = tv.getBitmap(8, 8)
                                                        if (bitmap != null) {
                                                            var rSum = 0L
                                                            var gSum = 0L
                                                            var bSum = 0L
                                                            val pixels = IntArray(64)
                                                            bitmap.getPixels(pixels, 0, 8, 0, 0, 8, 8)
                                                            for (pixel in pixels) {
                                                                rSum += (pixel shr 16) and 0xFF
                                                                gSum += (pixel shr 8) and 0xFF
                                                                bSum += pixel and 0xFF
                                                            }
                                                            val avgR = (rSum / 64).toInt().coerceIn(0, 255)
                                                            val avgG = (gSum / 64).toInt().coerceIn(0, 255)
                                                            val avgB = (bSum / 64).toInt().coerceIn(0, 255)
                                                            viewModel.updateAmbientColor(avgR, avgG, avgB)
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }

                                        AndroidView(
                                            factory = { ctx ->
                                                TextureView(ctx).apply {
                                                    viewModel.player?.setVideoTextureView(this)
                                                    textureViewRef = this
                                                }
                                            },
                                            update = { textureView ->
                                                viewModel.player?.setVideoTextureView(textureView)
                                                textureViewRef = textureView
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { isFullScreen = true }
                                        )
                                        
                                        // Video Resize Toggle Overlay
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                                .clickable {
                                                    videoScaleMode = if (videoScaleMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                    } else {
                                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Aspect Ratio",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (videoScaleMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) "適應" else "填滿",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Full Screen Toggle Overlay
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                                .clickable { isFullScreen = true },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = "Full Screen",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "全螢幕",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                // MP3 Audio Vinyl with smooth hue breathing color cycling
                                LaunchedEffect(isPlaying, currentMediaItem) {
                                    if (isPlaying) {
                                        var hue = 0f
                                        while (isPlaying) {
                                            delay(160)
                                            hue = (hue + 3f) % 360f
                                            val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.85f))
                                            val r = (colorInt shr 16) and 0xFF
                                            val g = (colorInt shr 8) and 0xFF
                                            val b = colorInt and 0xFF
                                            viewModel.updateAmbientColor(r, g, b)
                                        }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    // Music visual backglow
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(16.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(ambientColorCompose, Color.Transparent),
                                                    radius = 320f
                                                )
                                            )
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val rotation = rememberInfiniteTransition(label = "Vinyl rotation")
                                        val angle by rotation.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(durationMillis = 8000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "Angle"
                                        )

                                        Box(
                                            modifier = Modifier.size(110.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Vinyl Base Ring
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .rotate(if (isPlaying) angle else 0f),
                                                shape = CircleShape,
                                                color = Color(0xFF111111),
                                                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    // Decorative Vinyl Grooves
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2.5f)
                                                        drawCircle(Color.White.copy(alpha = 0.03f), radius = size.minDimension / 3.5f)
                                                    }
                                                    // Center Music Node Icon
                                                    Icon(
                                                        imageVector = Icons.Default.Audiotrack,
                                                        contentDescription = "Audio track",
                                                        tint = Color(0xFFEFB8C8),
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Dynamic Fluid Equalizer Bars Drawing
                                        AnimatedEqualizer(isPlaying = isPlaying)
                                    }
                                }
                            }
                        }
                    }

                    // Metadata Panel
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentMediaItem?.title ?: "未在播放",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("current_track_title")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentMediaItem?.artist ?: "未知演出者",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("current_track_artist")
                                )
                            }
                        }
                    }



                    // Music Controls Area
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            // Audio seeking track
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = currentTime.toFloat(),
                                    onValueChange = { viewModel.seekTo(it.toLong()) },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFFEFB8C8),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("media_progress_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentTime),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = formatTime(duration),
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Playback controls row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.playPrevious() },
                                    modifier = Modifier.testTag("prev_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Track",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.skipBackward() },
                                    modifier = Modifier.testTag("rewind_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Play Pause main glass button
                                Surface(
                                    onClick = { viewModel.playPause() },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .testTag("play_pause_button"),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.skipForward() },
                                    modifier = Modifier.testTag("forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.playNext() },
                                    modifier = Modifier.testTag("next_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Track",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Speed & Mute Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { viewModel.toggleMute() }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = "Mute Toggle",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isMuted) "靜音" else "音量開啟",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }

                                // Speed selection
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("播放速度: ", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                                    speeds.forEach { speed ->
                                        val isCurrentSpeed = playbackSpeed == speed
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .background(
                                                    if (isCurrentSpeed) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.setSpeed(speed) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${speed}x",
                                                 color = if (isCurrentSpeed) Color.White else Color.White.copy(alpha = 0.5f),
                                                 fontSize = 11.sp,
                                                 fontWeight = if (isCurrentSpeed) FontWeight.Bold else FontWeight.Normal
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     }

                     // Unified Cloud Streaming Panel (YouTube & SoundCloud)
                    item {
                        var streamingPlatform by remember { mutableStateOf("youtube") } // "youtube" or "soundcloud"
                        
                        var youtubeUrlOrId by remember { mutableStateOf("") }
                        var isVideoMode by remember { mutableStateOf(false) } // false = audio only, true = video
                        var searchMode by remember { mutableStateOf(0) } // 0 = 關鍵字搜尋, 1 = 貼上連結
                        var searchQuery by remember { mutableStateOf("") }
                        var soundcloudUrl by remember { mutableStateOf("") }

                        val isResolvingYoutube by viewModel.isResolvingYoutube.collectAsState()
                        val youtubeResolveError by viewModel.youtubeResolveError.collectAsState()

                        val youtubeSearchResults by viewModel.youtubeSearchResults.collectAsState()
                        val isSearchingYoutube by viewModel.isSearchingYoutube.collectAsState()
                        val youtubeSearchError by viewModel.youtubeSearchError.collectAsState()

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            // Sliding custom segmented toggle between YouTube and SoundCloud
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                                    .border(
                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                listOf("YouTube", "SoundCloud").forEach { platform ->
                                    val isSelected = (platform == "YouTube" && streamingPlatform == "youtube") ||
                                                     (platform == "SoundCloud" && streamingPlatform == "soundcloud")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) {
                                                    if (platform == "YouTube") Color(0xFFFF0000).copy(alpha = 0.22f)
                                                    else Color(0xFFFF5500).copy(alpha = 0.22f)
                                                } else Color.Transparent
                                            )
                                            .clickable { 
                                                streamingPlatform = if (platform == "YouTube") "youtube" else "soundcloud"
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (platform == "YouTube") Icons.Default.PlayArrow else Icons.Default.Audiotrack,
                                                contentDescription = platform,
                                                tint = if (isSelected) {
                                                    if (platform == "YouTube") Color(0xFFFF4D4D) else Color(0xFFFF7733)
                                                } else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = platform,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (streamingPlatform == "youtube") {
                                // YouTube Mode content
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "YouTube Icon",
                                            tint = Color(0xFFFF0000),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "YouTube 影音探索",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "搜尋喜愛的 YouTube 影片或貼上連結，免下載即刻串流播放與自動產生動態背景色彩！",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                // Glass Segmented Tab Bar for YouTube modes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .border(
                                            BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                ) {
                                    listOf("關鍵字搜尋", "貼上網址/ID").forEachIndexed { index, label ->
                                        val isSelected = searchMode == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                                )
                                                .clickable { searchMode = index }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                if (searchMode == 0) {
                                    // Keyword Search Mode
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            label = { Text("輸入關鍵字搜尋影片", color = Color.White.copy(alpha = 0.6f)) },
                                            placeholder = { Text("例如：周杰倫, lo-fi beats...", color = Color.White.copy(alpha = 0.3f)) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("youtube_search_input"),
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = Color.White.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        )

                                        Button(
                                            onClick = {
                                                if (searchQuery.isNotBlank()) {
                                                    viewModel.searchYoutube(searchQuery)
                                                }
                                            },
                                            enabled = searchQuery.isNotBlank() && !isSearchingYoutube,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD0BCFF),
                                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            if (isSearchingYoutube) {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    color = Color.Black,
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    youtubeSearchError?.let { err ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = err,
                                            color = Color(0xFFFFB3B3),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }

                                    if (youtubeSearchResults.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "搜尋結果 (點擊直接加入播放或聽歌)：",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 280.dp)
                                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                itemsIndexed(youtubeSearchResults) { _, result ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                                            .padding(8.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        AsyncImage(
                                                            model = result.thumbnailUrl,
                                                            contentDescription = "Thumbnail",
                                                            modifier = Modifier
                                                                .size(width = 80.dp, height = 45.dp)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = result.title,
                                                                color = Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = result.uploaderName,
                                                                    color = Color.White.copy(alpha = 0.5f),
                                                                    fontSize = 10.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Text(
                                                                    text = result.durationText,
                                                                    color = Color(0xFFEFB8C8),
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }

                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color(0xFFEFB8C8).copy(alpha = 0.15f), CircleShape)
                                                                    .clickable {
                                                                        viewModel.addYoutubeItem(
                                                                            urlOrId = result.videoId,
                                                                            isVideoMode = false,
                                                                            onSuccess = {},
                                                                            onError = {}
                                                                        )
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("音訊", color = Color(0xFFEFB8C8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }

                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color(0xFFD0BCFF).copy(alpha = 0.15f), CircleShape)
                                                                    .clickable {
                                                                        viewModel.addYoutubeItem(
                                                                            urlOrId = result.videoId,
                                                                            isVideoMode = true,
                                                                            onSuccess = {},
                                                                            onError = {}
                                                                        )
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("影片", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Paste URL Mode
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.OutlinedTextField(
                                            value = youtubeUrlOrId,
                                            onValueChange = { youtubeUrlOrId = it },
                                            label = { Text("貼上網址或 11 位影片 ID", color = Color.White.copy(alpha = 0.6f)) },
                                            placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.White.copy(alpha = 0.3f)) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                            enabled = !isResolvingYoutube,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("youtube_url_input"),
                                            trailingIcon = {
                                                if (youtubeUrlOrId.isNotEmpty()) {
                                                    IconButton(onClick = { youtubeUrlOrId = "" }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = Color.White.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (!isVideoMode) Color(0xFFEFB8C8) else Color.White.copy(alpha = 0.15f)
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .background(
                                                    if (!isVideoMode) Color(0xFFEFB8C8).copy(alpha = 0.1f) else Color.Transparent
                                                )
                                                .clickable { isVideoMode = false }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Audiotrack,
                                                contentDescription = "Audio only",
                                                tint = if (!isVideoMode) Color(0xFFEFB8C8) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "純音樂模式",
                                                color = if (!isVideoMode) Color(0xFFEFB8C8) else Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (isVideoMode) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.15f)
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .background(
                                                    if (isVideoMode) Color(0xFFD0BCFF).copy(alpha = 0.1f) else Color.Transparent
                                                )
                                                .clickable { isVideoMode = true }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Movie,
                                                contentDescription = "Video mode",
                                                tint = if (isVideoMode) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "影片模式",
                                                color = if (isVideoMode) Color(0xFFD0BCFF) else Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            if (youtubeUrlOrId.isNotBlank()) {
                                                viewModel.addYoutubeItem(
                                                    urlOrId = youtubeUrlOrId,
                                                    isVideoMode = isVideoMode,
                                                    onSuccess = {
                                                        youtubeUrlOrId = ""
                                                    },
                                                    onError = {}
                                                )
                                            }
                                        },
                                        enabled = youtubeUrlOrId.isNotBlank() && !isResolvingYoutube,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isVideoMode) Color(0xFFD0BCFF) else Color(0xFFEFB8C8),
                                            disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("youtube_add_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isResolvingYoutube) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                color = Color.Black,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("串流解析中...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Load Stream",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("載入 YouTube 串流播放", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    youtubeResolveError?.let { err ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = err,
                                            color = Color(0xFFFFB3B3),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            } else {
                                // SoundCloud Mode content
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Audiotrack,
                                            contentDescription = "SoundCloud Icon",
                                            tint = Color(0xFFFF5500),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SoundCloud 音樂匯入",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "貼上 SoundCloud 的單首歌曲或播放清單(Sets)網址，即可一鍵載入待播清單串流播放！",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.OutlinedTextField(
                                        value = soundcloudUrl,
                                        onValueChange = { soundcloudUrl = it },
                                        label = { Text("貼上 SoundCloud 網址", color = Color.White.copy(alpha = 0.6f)) },
                                        placeholder = { Text("https://soundcloud.com/...", color = Color.White.copy(alpha = 0.3f)) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                        enabled = !isResolvingYoutube,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("soundcloud_url_input"),
                                        trailingIcon = {
                                            if (soundcloudUrl.isNotEmpty()) {
                                                IconButton(onClick = { soundcloudUrl = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        tint = Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (soundcloudUrl.isNotBlank()) {
                                            viewModel.addSoundCloudItem(
                                                url = soundcloudUrl,
                                                onSuccess = {
                                                    soundcloudUrl = ""
                                                },
                                                onError = {}
                                            )
                                        }
                                    },
                                    enabled = soundcloudUrl.isNotBlank() && !isResolvingYoutube,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF5500),
                                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("soundcloud_add_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isResolvingYoutube && soundcloudUrl.isNotEmpty()) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("串流解析中...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Load Stream",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("載入 SoundCloud 串流", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                youtubeResolveError?.let { err ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = err,
                                        color = Color(0xFFFFB3B3),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Playlist Queue
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "待播清單 Queue (${playlist.size})",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .clickable { filePickerLauncher.launch("*/*") }
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Track",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("匯入 MP3/4", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Dynamic tab layout based on presence of cloud items
                            val hasCloudItems = playlist.any { !it.isLocal }
                            if (hasCloudItems) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                        .border(
                                            BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                ) {
                                    val tabs = listOf(
                                        "all" to "全部待播",
                                        "local" to "本機音樂",
                                        "cloud" to "雲端串流"
                                    )
                                    tabs.forEach { (tabKey, tabLabel) ->
                                        val isTabSelected = selectedPlaylistTab == tabKey
                                        val count = when (tabKey) {
                                            "all" -> playlist.size
                                            "local" -> playlist.count { it.isLocal }
                                            else -> playlist.count { !it.isLocal }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isTabSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                                )
                                                .clickable { selectedPlaylistTab = tabKey }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$tabLabel ($count)",
                                                color = if (isTabSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (selectedPlaylistTab == "cloud") {
                                    selectedPlaylistTab = "all"
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filteredPlaylist = playlist.mapIndexed { idx, item -> idx to item }.filter { (_, item) ->
                                    if (hasCloudItems) {
                                        when (selectedPlaylistTab) {
                                            "local" -> item.isLocal
                                            "cloud" -> !item.isLocal
                                            else -> true
                                        }
                                    } else {
                                        true
                                    }
                                }

                                if (filteredPlaylist.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "此分頁暫無歌曲",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    filteredPlaylist.forEach { (idx, item) ->
                                        val isSelected = currentIndex == idx
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (isSelected) Color(0xFFEFB8C8).copy(alpha = 0.4f) else Color.Transparent
                                                    ),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .background(
                                                    if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { viewModel.playItem(item, idx) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Dynamic Type Icon
                                            Icon(
                                                imageVector = if (item.isVideo) Icons.Default.Movie else Icons.Default.Audiotrack,
                                                contentDescription = if (item.isVideo) "Video File" else "Audio File",
                                                tint = if (isSelected) Color(0xFFEFB8C8) else Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val subtitleText = when {
                                                    item.id.startsWith("youtube_") -> "YouTube • ${item.artist}"
                                                    item.id.startsWith("soundcloud_") -> "SoundCloud • ${item.artist}"
                                                    item.isLocal -> "本機 • ${item.artist}"
                                                    else -> "串流 • ${item.artist}"
                                                }
                                                Text(
                                                    text = subtitleText,
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Delete from playlist button
                                            IconButton(
                                                onClick = { viewModel.removePlaylistItem(idx) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Track",
                                                    tint = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Full-Screen Video Over-Everything Layer
    if (isFullScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { /* prevent clicks passing through */ },
            contentAlignment = Alignment.Center
        ) {
            var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        viewModel.player?.setVideoTextureView(this)
                        textureViewRef = this
                    }
                },
                update = { textureView ->
                    viewModel.player?.setVideoTextureView(textureView)
                    textureViewRef = textureView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Auto-hiding full screen controls
            var showControls by remember { mutableStateOf(true) }
            LaunchedEffect(showControls) {
                if (showControls) {
                    delay(4000)
                    showControls = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showControls = !showControls }
            ) {
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        // Title / Metadata info at Top Start
                        Text(
                            text = currentMediaItem?.title ?: "",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(24.dp)
                                .statusBarsPadding()
                        )

                        // Close/Exit Full Screen Button at Top End
                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .statusBarsPadding()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Full Screen",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Play/Pause Center Action
                        IconButton(
                            onClick = { viewModel.playPause() },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        // Bottom Media Control Bar with Slider
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(24.dp)
                                .navigationBarsPadding()
                        ) {
                            Slider(
                                value = currentTime.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toLong()) },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFFEFB8C8),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatTime(currentTime),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet listing all device decoders (可以並用解碼器探索面板)
    if (showCodecListSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val codecList = remember { DecoderHelper.getDecoderList() }

        ModalBottomSheet(
            onDismissRequest = { showCodecListSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E24),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "裝置解碼晶片庫",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "列出您手機內建支援 MP3/MP4 的所有 MediaCodec 解碼器",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { showCodecListSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(codecList) { _, codec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = codec.friendlyName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = codec.name,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))

                                // Chip indicating Hardware acceleration vs Software
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (codec.isHardwareAccelerated) Color(0x33A8FFB2) else Color(0x33FFB3B3),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (codec.isHardwareAccelerated) "硬體晶片" else "軟體解碼",
                                        color = if (codec.isHardwareAccelerated) Color(0xFFA8FFB2) else Color(0xFFFFB3B3),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "支援格式類型: ${codec.supportedTypes.joinToString(", ")}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

// ---------------- Reusable Glass Components ----------------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun HeaderRow(
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = "Logo",
                    tint = Color(0xFFEFB8C8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aero Glass Player",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "毛玻璃硬/軟解並用播放器",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "System Settings",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onImportClick,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                    .testTag("import_file_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Import local media files",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AnimatedEqualizer(isPlaying: Boolean) {
    // 頻段數量，設定為 16 個頻段，使佈局極其精緻
    val barCount = 16
    
    // 記錄每個頻段當前的高度（百分比 0.0f 到 1.0f）
    val heights = remember { FloatArray(barCount) { 0.1f } }
    // 記錄每個頻段的峰值位置（0.0f 到 1.0f）
    val peaks = remember { FloatArray(barCount) { 0.1f } }
    // 記錄每個峰值的下墜速度
    val peakWeights = remember { FloatArray(barCount) { 0f } }

    // 使用 withFrameNanos 產生平滑且幀率同步的物理動力學動畫
    var tick by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isPlaying) {
        val frequencies = FloatArray(barCount) { i ->
            0.6f + (i * 0.08f)
        }
        val phases = FloatArray(barCount) { i ->
            (i * 0.4f)
        }

        var lastTimeNanos = System.nanoTime()
        while (true) {
            androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f // 秒
                lastTimeNanos = frameTimeNanos
                
                if (isPlaying) {
                    tick += dt * 4.5f // 控制跳動的基礎速度
                    for (i in 0 until barCount) {
                        // 使用多重正弦波、餘弦波和隨機小噪聲混合，模擬出極其逼真的音樂頻率能量分佈
                        val baseWave = kotlin.math.sin(tick * frequencies[i] + phases[i])
                        val subWave = kotlin.math.cos(tick * frequencies[i] * 1.8f + phases[i] * 1.2f)
                        val noise = (kotlin.math.sin(tick * 12f + i) * 0.12f)
                        
                        // 低頻（左側）振幅較大，高頻（右側）振幅較精緻
                        val positionWeight = 1.0f - (i.toFloat() / barCount) * 0.35f
                        
                        // 計算目標高度百分比
                        val targetHeight = ((baseWave * 0.45f + subWave * 0.25f + noise + 0.5f) * positionWeight).coerceIn(0.08f, 0.92f)
                        
                        // 稍微平滑插值 (Lerp) 讓跳動更圓滑，避免突變
                        heights[i] = heights[i] + (targetHeight - heights[i]) * 0.22f
                    }
                } else {
                    // 暫停時，所有頻譜柱平滑地落回極小值
                    for (i in 0 until barCount) {
                        heights[i] = heights[i] + (0.05f - heights[i]) * 0.12f
                    }
                }

                // 物理重力模擬：更新峰值亮點
                for (i in 0 until barCount) {
                    val currentH = heights[i]
                    if (currentH >= peaks[i]) {
                        peaks[i] = currentH
                        peakWeights[i] = 0f // 歸零重力下墜速度
                    } else {
                        // 隨著時間，重力加速度讓峰值下掉
                        peakWeights[i] = peakWeights[i] + 1.0f * dt // 加速度
                        peaks[i] = (peaks[i] - peakWeights[i] * dt).coerceIn(0.05f, 1.0f)
                        // 限制如果掉到比當前高度還低，則對齊
                        if (peaks[i] < currentH) {
                            peaks[i] = currentH
                            peakWeights[i] = 0f
                        }
                    }
                }
            }
        }
    }

    // 繪製頻譜
    Canvas(
        modifier = Modifier
            .width(220.dp)
            .height(80.dp)
            .padding(horizontal = 8.dp)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.02f))
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        for (i in 0 until barCount) {
            val barHeight = heights[i] * height
            val x = i * (barWidth + spacing)
            val y = height - barHeight

            // 玻璃擬態霓虹漸變色筆刷
            val brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEFB8C8), // 亮霓虹粉
                    Color(0xFFD0BCFF), // 電光紫
                    Color(0xFFD0BCFF).copy(alpha = 0.15f) // 半透明底部，打造空氣懸浮感
                )
            )

            // 繪製頻譜柱
            drawRoundRect(
                brush = brush,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // 繪製頂部浮動亮點（Peak Float Point）
            val peakY = height - (peaks[i] * height) - 3.dp.toPx()
            if (peakY < height - 5.dp.toPx()) {
                drawCircle(
                    color = Color(0xFFEFB8C8),
                    radius = (barWidth / 2f).coerceAtMost(2.5f.dp.toPx()),
                    center = androidx.compose.ui.geometry.Offset(x + barWidth / 2f, peakY)
                )
            }
        }
    }
}

// Helper to format track times
fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// Resolver for open document paths
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Imported Media"
}
