package com.example.mcpclientapp2

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * 🌐 MCP Server管理レジストリ
 * 外部MCPサーバーの設定と管理を行う
 */
object MCPServerRegistry {

    @Serializable
    data class MCPServerConfig(
        val id: String,
        val name: String,
        val description: String,
        val baseUrl: String,
        val enabled: Boolean = true,
        val capabilities: List<String>,
        val toolKeywords: List<String>
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // --- ↓↓↓ ポート番号を修正しました！ ↓↓↓ ---
    private val presetServers = listOf(
        MCPServerConfig(
            id = "weather",
            name = "Weather Service",
            description = "US天気情報を提供するMCPサーバー",
            baseUrl = "http://localhost:5010", // 5002 -> 5010
            capabilities = listOf("weather", "forecast", "alerts"),
            toolKeywords = listOf("天気", "weather", "温度", "雨", "晴れ", "曇り", "forecast", "気温", "湿度", "警報", "アラート")
        ),
        MCPServerConfig(
            id = "google_calendar",
            name = "Google Calendar MCP",
            description = "Google Calendar統合 - イベント作成・管理・検索",
            baseUrl = "http://localhost:5011", // 5002 -> 5011
            capabilities = listOf("calendar", "events", "scheduling", "reminders"),
            toolKeywords = listOf(
                "カレンダー", "calendar", "予定", "スケジュール", "schedule", "event", "イベント",
                "会議", "meeting", "ミーティング", "打ち合わせ", "予約", "booking",
                "リマインダー", "reminder", "通知", "notification", "アラーム", "alarm",
                "今日の予定", "明日の予定", "今週", "来週", "来月", "次回", "空いている時間",
                "free time", "busy", "空き時間", "available", "空きスロット"
            )
        ),
        MCPServerConfig(
            id = "googlemaps",
            name = "Google Maps Service",
            description = "地図・場所検索・ルート案内を提供",
            baseUrl = "http://localhost:5012", // 5002 -> 5012
            capabilities = listOf("maps", "directions", "places", "geocoding"),
            toolKeywords = listOf("地図", "場所", "住所", "ルート", "道順", "map", "location", "address", "directions", "navigate", "位置", "geocode")
        ),
        // --- 以下のサーバーはまだ実装されていないため、一旦コメントアウトまたは削除を推奨します ---
        /*
        MCPServerConfig(
            id = "slack",
            name = "Slack MCP",
            description = "Slack統合 - メッセージ送信・チャンネル管理",
            baseUrl = "http://localhost:5013", // 将来のポート
            capabilities = listOf("messaging", "channels", "users", "notifications"),
            toolKeywords = listOf(...)
        ),
        MCPServerConfig(
            id = "playwright",
            name = "Web Automation Service",
            description = "Webブラウザ自動化・スクレイピング",
            baseUrl = "http://localhost:5014", // 将来のポート
            capabilities = listOf("browser", "scraping", "automation", "screenshot"),
            toolKeywords = listOf(...)
        )
        */
    )
    // --- ↑↑↑ 修正完了 ↑↑↑ ---

    private var customServers = mutableListOf<MCPServerConfig>()

    fun getAllServers(): List<MCPServerConfig> {
        return presetServers + customServers
    }

    fun getEnabledServers(): List<MCPServerConfig> {
        return getAllServers().filter { it.enabled }
    }

    fun findMatchingServer(message: String): MCPServerConfig? {
        val lowerMessage = message.lowercase()
        return getEnabledServers().find { server ->
            server.toolKeywords.any { keyword ->
                lowerMessage.contains(keyword.lowercase())
            }
        }
    }
    // ... (以下の関数は変更なし)
    fun getServerById(id: String): MCPServerConfig? {
        return getAllServers().find { it.id == id }
    }

    fun addCustomServer(server: MCPServerConfig) {
        customServers.add(server)
    }

    fun removeCustomServer(id: String) {
        customServers.removeAll { it.id == id }
    }

    fun exportToJson(): String {
        return json.encodeToString(customServers)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val importedServers = json.decodeFromString<List<MCPServerConfig>>(jsonString)
            customServers.clear()
            customServers.addAll(importedServers)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun generateSampleConfig(): String {
        val sampleServer = MCPServerConfig(
            id = "sample_api",
            name = "Sample API Server",
            description = "サンプルAPIサーバーの説明",
            baseUrl = "https://your-api-server.com",
            capabilities = listOf("search", "data"),
            toolKeywords = listOf("検索", "データ", "search", "info")
        )

        return json.encodeToString(listOf(sampleServer))
    }
}
