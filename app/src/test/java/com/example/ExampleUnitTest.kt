package com.example

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
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

