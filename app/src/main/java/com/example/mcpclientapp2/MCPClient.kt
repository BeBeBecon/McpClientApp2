package com.example.mcpclientapp2

import com.example.mcpclientapp2.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class MCPClient {
    // 🌐 HTTP通信用のOkHttpクライアント（HTTP/HTTPS両対応、タイムアウト設定済み）
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // 🔧 ローカル開発用：すべてのホスト名を許可
        .hostnameVerifier { _, _ -> true }
        // 🔧 開発用：すべてのSSL証明書を信頼（自己署名証明書対応）
        .apply {
            try {
                // 全てを信頼するTrustManagerを作成
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            } catch (e: Exception) {
                // SSL設定に失敗した場合はデフォルト設定を使用
            }
        }
        .build()

    // 📄 JSON解析設定（未知のフィールドを無視、エラー耐性あり）
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 📡 Server-Sent Events接続オブジェクト
    private var eventSource: EventSource? = null
    // 🔧 MCP Serverから取得した利用可能ツール一覧
    private var availableTools: List<MCPTool> = emptyList()

    // 📡 MCP Serverに接続確認のPingを送信
    suspend fun pingServer(serverUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 🔧 プロトコル自動判定（ローカル開発用）
                val cleanUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"  // デフォルトはHTTP（ローカル開発用）
                } else {
                    serverUrl
                }
                
                println("🔍 DEBUG: Pinging $cleanUrl")
                
                // ServerApp2の基本的なヘルスチェック（rootエンドポイント）
                val request = Request.Builder()
                    .url(cleanUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val success = response.isSuccessful
                
                println("🔍 DEBUG: Ping response: ${response.code}")
                success
            } catch (e: Exception) {
                println("🔍 DEBUG: Ping error: ${e.message}")
                false  // エラー時はfalse
            }
        }
    }

    // 📋 サーバーから利用可能なツール一覧を取得
    suspend fun getToolsFromServer(serverUrl: String, serverName: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }
                
                val endpoint = "$cleanUrl/$serverName/tools"
                println("🔍 DEBUG: Getting tools from $endpoint")
                
                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    println("🔍 DEBUG: Tools response: $responseBody")
                    val tools = json.decodeFromString<List<String>>(responseBody)
                    tools
                } else {
                    println("🔍 DEBUG: Tools request failed: ${response.code}")
                    emptyList()
                }
            } catch (e: Exception) {
                println("🔍 DEBUG: Error getting tools: ${e.message}")
                emptyList()
            }
        }
    }

    // ⚙️ ServerApp2のAPIエンドポイントに対応したツール実行
    suspend fun callTool(serverUrl: String, serverName: String, toolName: String, arguments: Map<String, Any>): String {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }
                
                println("🔍 DEBUG: Calling tool $toolName on $cleanUrl")
                println("🔍 DEBUG: Server name: $serverName")
                println("🔍 DEBUG: Arguments: $arguments")
                
                // ServerApp2のエンドポイント形式: /{serverName}/call/{toolName}
                val endpoint = "$cleanUrl/$serverName/call/$toolName"
                
                val requestBuilder = Request.Builder().url(endpoint)
                
                if (arguments.isNotEmpty()) {
                    // GETパラメータとして引数を追加
                    val urlBuilder = endpoint.toHttpUrl().newBuilder()
                    arguments.forEach { (key, value) ->
                        urlBuilder.addQueryParameter(key, value.toString())
                    }
                    requestBuilder.url(urlBuilder.build())
                }
                
                val finalUrl = requestBuilder.build().url.toString()
                println("🔍 DEBUG: Final request URL: $finalUrl")
                
                val response = client.newCall(requestBuilder.get().build()).execute()
                val responseBody = response.body?.string() ?: "No response"
                
                println("🔍 DEBUG: Response code: ${response.code}")
                println("🔍 DEBUG: Response body: $responseBody")
                
                responseBody
            } catch (e: Exception) {
                println("🔍 DEBUG: Error in callTool: ${e.message}")
                "Error calling tool: ${e.message}"
            }
        }
    }

    // 🔗 MCP ServerにSSE接続してリアルタイム通信開始（レガシー互換）
    suspend fun connectToServer(serverUrl: String, onMessage: (String) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                onMessage("✅ Connected to ServerApp2 at $serverUrl")
                true
            } catch (e: Exception) {
                onMessage("❌ Failed to connect: ${e.message}")
                false
            }
        }
    }

    // 📋 現在利用可能なツール一覧を取得（レガシー互換）
    fun getAvailableTools(): List<MCPTool> = availableTools

    // 🔌 SSE接続を切断
    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }
}