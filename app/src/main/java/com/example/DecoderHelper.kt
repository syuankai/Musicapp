package com.example

import android.media.MediaCodecList
import android.os.Build

data class CodecInfo(
    val name: String,
    val friendlyName: String,
    val isEncoder: Boolean,
    val isSoftwareOnly: Boolean,
    val isHardwareAccelerated: Boolean,
    val supportedTypes: List<String>
)

object DecoderHelper {
    fun getFriendlyName(codecName: String): String {
        val nameLower = codecName.lowercase()
        val isSoftware = nameLower.contains(".google.") || 
                         nameLower.contains("android.google") || 
                         nameLower.contains("c2.android.") || 
                         nameLower.contains(".sw.") || 
                         nameLower.contains("google.sw") ||
                         nameLower.contains("software")

        val brand = when {
            nameLower.contains("qcom") || nameLower.contains("qualcomm") -> "高通驍龍 (Qualcomm)"
            nameLower.contains("mtk") || nameLower.contains("mediatek") -> "聯發科 (MediaTek)"
            nameLower.contains("sec") || nameLower.contains("samsung") || nameLower.contains("exynos") -> "三星 (Samsung)"
            nameLower.contains("hisi") || nameLower.contains("kirin") -> "華為麒麟 (Kirin)"
            nameLower.contains("intel") -> "英特爾 (Intel)"
            nameLower.contains("nvidia") || nameLower.contains("tegra") -> "輝達 (NVIDIA)"
            nameLower.contains("amlogic") -> "晶晨 (Amlogic)"
            nameLower.contains("rk") || nameLower.contains("rockchip") -> "瑞芯微 (Rockchip)"
            nameLower.contains("sprd") || nameLower.contains("unisoc") -> "紫光展銳 (Unisoc)"
            nameLower.contains("broadcom") -> "博通 (Broadcom)"
            nameLower.contains("apple") -> "蘋果 (Apple)"
            isSoftware -> "Android 系統內建"
            else -> "硬體商通用"
        }

        val type = when {
            nameLower.contains("avc") || nameLower.contains("h264") || nameLower.contains("h.264") -> "AVC / H.264 影片"
            nameLower.contains("hevc") || nameLower.contains("h265") || nameLower.contains("h.265") -> "HEVC / H.265 影片"
            nameLower.contains("vp9") -> "VP9 影片"
            nameLower.contains("vp8") -> "VP8 影片"
            nameLower.contains("av1") -> "AV1 超高畫質影片"
            nameLower.contains("mp3") || nameLower.contains("mpeg3") -> "MP3 串流音訊"
            nameLower.contains("aac") -> "AAC 高規音訊"
            nameLower.contains("opus") -> "Opus 高解析音訊"
            nameLower.contains("flac") -> "FLAC 無損音訊"
            nameLower.contains("vorbis") -> "Vorbis 遊戲音訊"
            nameLower.contains("mpeg2") || nameLower.contains("h262") -> "MPEG-2 舊型影片"
            nameLower.contains("m4a") -> "M4A 音訊"
            else -> ""
        }

        val method = if (isSoftware) "軟體模擬解碼 (CPU)" else "硬體高效能解碼 (DSP/GPU)"
        val cleanName = codecName.removePrefix("OMX.").removePrefix("c2.")

        return if (type.isNotEmpty()) {
            "$brand $type • $method ($cleanName)"
        } else {
            "$brand 通用解碼 • $method ($cleanName)"
        }
    }

    fun getDecoderList(): List<CodecInfo> {
        val codecList = mutableListOf<CodecInfo>()
        try {
            val list = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in list.codecInfos) {
                if (info.isEncoder) continue // Only show decoders

                val types = info.supportedTypes.toList()
                if (types.isEmpty()) continue

                // Only show decoders that are relevant for audio or video playback
                val isRelevant = types.any { type ->
                    type.contains("audio", ignoreCase = true) || type.contains("video", ignoreCase = true)
                }
                if (!isRelevant) continue

                val isSoftwareOnly = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isSoftwareOnly
                } else {
                    // Fallback for API < 29
                    info.name.startsWith("OMX.google.", ignoreCase = true) || 
                    info.name.startsWith("c2.android.", ignoreCase = true)
                }

                val isHardwareAccelerated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    info.isHardwareAccelerated
                } else {
                    !isSoftwareOnly
                }

                codecList.add(
                    CodecInfo(
                        name = info.name,
                        friendlyName = getFriendlyName(info.name),
                        isEncoder = false,
                        isSoftwareOnly = isSoftwareOnly,
                        isHardwareAccelerated = isHardwareAccelerated,
                        supportedTypes = types
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sort decoders: hardware accelerated first, then by name
        return codecList.sortedWith(compareByDescending<CodecInfo> { it.isHardwareAccelerated }.thenBy { it.name })
    }
}
