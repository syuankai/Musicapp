package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AeroPlayer", appName)
  }

  @Test
  fun testYoutubeExtractor() = runBlocking {
    println("=== TESTING YOUTUBE EXTRACTOR ===")
    val videoId = "dQw4w9WgXcQ" // Rick Astley
    val stream = YoutubeExtractor.fetchStream(videoId, isVideoMode = false)
    println("Extracted YouTube Stream: $stream")
    if (stream != null) {
      println("Title: ${stream.title}")
      println("Artist: ${stream.artist}")
      println("Stream URL: ${stream.streamUrl}")
    } else {
      println("YouTube Extractor failed to fetch stream!")
    }
  }

  @Test
  fun testSoundCloudExtractor() = runBlocking {
    println("=== TESTING SOUNDCLOUD EXTRACTOR ===")
    val url = "https://soundcloud.com/postmalone/congratulations"
    val tracks = SoundCloudExtractor.resolve(url)
    println("Resolved SoundCloud Tracks size: ${tracks.size}")
    if (tracks.isNotEmpty()) {
      val track = tracks.first()
      println("Title: ${track.title}")
      println("Artist: ${track.artist}")
      println("Stream URL: ${track.streamUrl}")
      val playableUrl = SoundCloudExtractor.fetchPlayableUrl(track.streamUrl)
      println("Playable Stream URL: $playableUrl")
    } else {
      println("SoundCloud Extractor failed to resolve tracks!")
    }
  }
}

