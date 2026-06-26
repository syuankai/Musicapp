package com.example

import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HyperionTcpClient {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var currentIp: String = ""
    private var currentPort: Int = 19444

    suspend fun sendColor(ip: String, port: Int, r: Int, g: Int, b: Int) = withContext(Dispatchers.IO) {
        try {
            // Reconnect if IP or Port changed, or if socket is not active
            if (socket == null || socket?.isClosed == true || currentIp != ip || currentPort != port || outputStream == null) {
                close()
                currentIp = ip
                currentPort = port
                
                socket = Socket()
                socket?.connect(InetSocketAddress(ip, port), 1200) // 1.2s timeout
                socket?.tcpNoDelay = true
                outputStream = socket?.getOutputStream()
            }

            // Standard Hyperion JSON socket command for color
            val payload = "{\"command\":\"color\",\"priority\":50,\"color\":[$r,$g,$b],\"origin\":\"AeroPlayer\"}\n"
            outputStream?.write(payload.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            close() // Clear stale socket so we retry next time
        }
    }

    suspend fun clearColor() = withContext(Dispatchers.IO) {
        try {
            if (socket != null && socket?.isClosed == false && outputStream != null) {
                val payload = "{\"command\":\"clear\",\"priority\":50}\n"
                outputStream?.write(payload.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            close()
        }
    }

    fun close() {
        try {
            outputStream?.close()
        } catch (e: Exception) {}
        try {
            socket?.close()
        } catch (e: Exception) {}
        outputStream = null
        socket = null
    }
}
