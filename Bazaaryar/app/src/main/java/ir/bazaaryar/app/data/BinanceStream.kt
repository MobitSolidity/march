package ir.bazaaryar.app.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Tick-level prices (~1 s) from Binance's public market-data-only WebSocket.
 * Subscribes only to the symbols we actually need, to keep mobile data usage sane.
 * Emits batches keyed by base symbol (e.g. "BTC"), priced in USDT.
 */
object BinanceStream {
    private const val URL = "wss://data-stream.binance.vision/ws"

    fun ticks(symbols: Set<String>): Flow<Map<String, LiveQuote>> = callbackFlow {
        val streams = symbols.map { it.lowercase() + "usdt@miniTicker" }
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                streams.chunked(200).forEachIndexed { i, chunk ->
                    val msg = JSONObject()
                        .put("method", "SUBSCRIBE")
                        .put("params", JSONArray(chunk))
                        .put("id", i + 1)
                    webSocket.send(msg.toString())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val batch = runCatching { parse(text) }.getOrNull()
                if (!batch.isNullOrEmpty()) this@callbackFlow.trySend(batch)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@callbackFlow.close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@callbackFlow.close(IOException(t.message ?: "websocket failure", t))
            }
        }
        val ws = Net.client.newWebSocket(Request.Builder().url(URL).build(), listener)
        awaitClose { ws.cancel() }
    }.buffer(256)

    private fun parse(text: String): Map<String, LiveQuote> {
        val t = text.trimStart()
        val now = System.currentTimeMillis()
        val out = HashMap<String, LiveQuote>()
        if (t.startsWith("[")) {
            val arr = JSONArray(t)
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(it, now, out) }
        } else {
            add(JSONObject(t), now, out)
        }
        return out
    }

    private fun add(o: JSONObject, now: Long, out: MutableMap<String, LiveQuote>) {
        if (o.optString("e") != "24hrMiniTicker") return
        val s = o.optString("s")
        if (!s.endsWith("USDT")) return
        val close = o.optString("c").toDoubleOrNull() ?: return
        val open = o.optString("o").toDoubleOrNull() ?: 0.0
        val change = if (open > 0) (close - open) / open * 100 else 0.0
        out[s.removeSuffix("USDT")] = LiveQuote(close, change, now)
    }
}
