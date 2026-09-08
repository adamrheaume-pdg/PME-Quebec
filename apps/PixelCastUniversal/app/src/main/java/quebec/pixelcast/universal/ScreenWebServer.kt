package quebec.pixelcast.universal

import fi.iki.elonen.NanoHTTPD
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

class ScreenWebServer(port: Int = 8080) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response = if (session.uri == "/stream.mjpeg") stream() else page()
    private fun page(): Response {
        val html = """<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Pixel Cast Universal</title><style>html,body{margin:0;background:#000;width:100%;height:100%;overflow:hidden}img{width:100%;height:100%;object-fit:contain}</style></head><body><img src=\"/stream.mjpeg\"></body></html>"""
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }
    private fun stream(): Response {
        val input = PipedInputStream(512 * 1024)
        val output = PipedOutputStream(input)
        thread(isDaemon = true) {
            var last = -1L
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val (seq, frame) = FrameStore.waitNext(last)
                    if (frame != null && seq != last) {
                        last = seq
                        output.write("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n".toByteArray())
                        output.write(frame); output.write("\r\n".toByteArray()); output.flush()
                    }
                }
            } catch (_: Exception) {} finally { try { output.close() } catch (_: Exception) {} }
        }
        return newChunkedResponse(Response.Status.OK, "multipart/x-mixed-replace; boundary=frame", input)
    }
}
