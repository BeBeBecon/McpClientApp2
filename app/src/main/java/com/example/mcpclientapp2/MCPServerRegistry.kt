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

    // --- ↓↓↓ IDを修正しました！ ↓↓↓ ---
    private val presetServers = listOf(
        MCPServerConfig(
            id = "weather_service",
            name = "Weather Service", 
            description = "US天気情報を提供するMCPサーバー",
            baseUrl = "http://localhost:5010",
            capabilities = listOf("weather", "forecast", "alerts"),
            toolKeywords = listOf("天気", "weather", "温度", "雨", "晴れ", "曇り", "forecast", "気温", "湿度", "警報", "アラート")
        ),
        MCPServerConfig(
            id = "google_calendar_service",
            name = "Google Calendar MCP",
            description = "Google Calendar統合 - イベント作成・管理・検索",
            baseUrl = "http://localhost:5011",
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
            id = "googlemaps_service",
            name = "Google Maps Service",
            description = "地図・場所検索・ルート案内を提供",
            baseUrl = "http://localhost:5012",
            capabilities = listOf("maps", "directions", "places", "geocoding"),
            toolKeywords = listOf("地図", "場所", "住所", "ルート", "道順", "map", "location", "address", "directions", "navigate", "位置", "geocode", "駅", "から", "まで", "行き方", "案内", "経路", "電車", "バス", "車", "歩き", "最寄り")
        )
    )
    // --- ↑↑↑ 修正完了 ↑↑↑ ---

    private var customServers = mutableListOf<MCPServerConfig>()

    fun getAllServers(): List<MCPServerConfig> {
        return presetServers + customServers
    }

    fun getEnabledServers(): List<MCPServerConfig> {
        return getAllServers().filter { it.enabled }
    }

    fun findMatchingServer(message: String): MCPServerRegistry.MCPServerConfig? {
        val lowerMessage = message.lowercase()
        
        // 優先順位付きでマッチング（より具体的なキーワードを先に判定）
        val enabledServers = getEnabledServers()
        
        // 1. 経路・道順関連（マップ優先）
        val routeKeywords = listOf("から", "まで", "行き方", "道順", "ルート", "経路", "案内", "directions", "navigate")
        if (routeKeywords.any { lowerMessage.contains(it) }) {
            enabledServers.find { it.id == "googlemaps_service" }?.let { return it }
        }
        
        // 2. 予定・カレンダー関連
        val calendarKeywords = listOf("予定", "スケジュール", "会議", "ミーティング", "calendar", "event")
        if (calendarKeywords.any { lowerMessage.contains(it) }) {
            enabledServers.find { it.id == "google_calendar_service" }?.let { return it }
        }
        
        // 3. 天気関連
        val weatherKeywords = listOf("天気", "weather", "気温", "雨", "晴れ", "曇り", "forecast", "予報")
        if (weatherKeywords.any { lowerMessage.contains(it) }) {
            enabledServers.find { it.id == "weather_service" }?.let { return it }
        }
        
        // 4. 従来のキーワードマッチング（フォールバック）
        return enabledServers.find { server ->
            server.toolKeywords.any { keyword ->
                lowerMessage.contains(keyword.lowercase())
            }
        }
    }

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
