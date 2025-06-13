package com.example.mcpclientapp2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * ⚙️ MCP Tool実行エンジン
 * ユーザーメッセージを分析してMCPツールを自動実行
 */
class MCPToolExecutor(private val mcpClient: MCPClient) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 🧠 メッセージを分析してMCPツールを実行
     */
    suspend fun analyzeAndExecute(message: String, llmProvider: LLMProvider): String {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 DEBUG: Analyzing message: $message")

                // 1️⃣ メッセージから適切なMCPサーバーを判定
                val matchingServer = MCPServerRegistry.findMatchingServer(message)
                println("🔍 DEBUG: Matching server: ${matchingServer?.name}")

                if (matchingServer == null) {
                    // 🤖 通常のLLM応答
                    return@withContext llmProvider.generateResponse(message, emptyList())
                }

                // 2️⃣ MCPサーバーに接続してツール一覧を取得
                val tools = getAvailableTools(matchingServer)

                if (tools.isEmpty()) {
                    return@withContext "❌ ${matchingServer.name}への接続に失敗しました。通常の応答をします。\n\n" +
                            llmProvider.generateResponse(message, emptyList())
                }

                // 3️⃣ LLMにツール使用判定を依頼
                val toolDecision = decideTool(message, matchingServer, tools, llmProvider)

                if (toolDecision.useTools) {
                    // 4️⃣ ツールを実行
                    val toolResult = executeTools(toolDecision.toolCalls)

                    // 5️⃣ ツール結果をLLMで整形
                    return@withContext formatToolResult(message, toolResult, llmProvider)
                } else {
                    // 🤖 ツール不要の場合は通常応答
                    return@withContext llmProvider.generateResponse(message, emptyList())
                }

            } catch (e: Exception) {
                println("🔍 DEBUG: Error in analyzeAndExecute: ${e.message}")
                // ❌ エラー時は通常のLLM応答にフォールバック
                return@withContext "⚠️ ツール実行中にエラーが発生しました: ${e.message}\n\n" +
                        llmProvider.generateResponse(message, emptyList())
            }
        }
    }

    /**
     * 🔧 MCPサーバーから利用可能ツールを取得
     */
    private suspend fun getAvailableTools(server: MCPServerRegistry.MCPServerConfig): List<String> {
        return try {
            println("🔍 DEBUG: Connecting to ${server.baseUrl} for server ${server.id}")
            val isConnected = mcpClient.pingServer(server.baseUrl)
            println("🔍 DEBUG: Ping result for ${server.baseUrl}: $isConnected")
            if (!isConnected) return emptyList()

            val tools = mcpClient.getToolsFromServer(server.baseUrl, server.id)
            println("🔍 DEBUG: Tools received from ${server.id}: $tools")
            tools
        } catch (e: Exception) {
            println("🔍 DEBUG: Error getting tools from ${server.id}: ${e.message}")
            emptyList()
        }
    }

    /**
     * 🧠 LLMにツール使用判定を依頼
     */
    private suspend fun decideTool(
        message: String,
        server: MCPServerRegistry.MCPServerConfig,
        tools: List<String>,
        llmProvider: LLMProvider
    ): ToolDecision {
        val decisionPrompt = buildToolDecisionPrompt(message, server, tools)
        val llmResponse = llmProvider.generateResponse(decisionPrompt, emptyList())
        println("🔍 DEBUG: LLM decision response:\n$llmResponse")
        return parseToolDecision(llmResponse, server)
    }

    /**
     * 📝 ツール判定用プロンプト構築
     */
    private fun buildToolDecisionPrompt(
        message: String,
        server: MCPServerRegistry.MCPServerConfig,
        tools: List<String>
    ): String {
        val toolsWithDetails = when (server.id) {
            "weather_service" -> """
                • get_forecast - 天気予報を取得（引数: latitude, longitude）
                • get_alerts - 気象警報を取得（引数: state）
            """.trimIndent()
            "googlemaps_service" -> """
                • search_places - 場所検索（引数: query, location）
                • get_directions - ルート案内（引数: origin, destination）
                • geocode_address - 住所から座標を取得（引数: address）
            """.trimIndent()
            "google_calendar_service" -> """
                • list_events - イベント一覧取得
                • create_event - イベント作成（引数: title, startTime, endTime）
                • search_events - イベント検索（引数: query）
            """.trimIndent()
            else -> tools.joinToString("\n") { "• $it" }
        }
        
        return """
            Analyze the user's message and determine if a specific MCP tool should be used.
            Respond strictly in the format below. Do not add any other text or formatting.

            User Message: "$message"
            Available MCP Server: ${server.name} (${server.description})
            Available Tools:
            $toolsWithDetails

            Important notes:
            - For weather queries, you need latitude and longitude. Use common coordinates for Japanese cities (Tokyo: 35.6762, 139.6503)
            - For location queries, extract the place names properly
            - For calendar queries, determine if listing or creating events

            Provide your response using these exact keys:
            USE_TOOLS: YES or NO
            TOOL_NAME: [Tool name to use (only if USE_TOOLS is YES)]
            ARGUMENTS: [Arguments for the tool in JSON format (only if USE_TOOLS is YES)]
            REASON: [Brief reason for your decision]
        """.trimIndent()
    }

    /**
     * 🔍 LLM応答からツール実行判定を解析 (ここが重要)
     */
    private fun parseToolDecision(llmResponse: String, server: MCPServerRegistry.MCPServerConfig): ToolDecision {
        return try {
            val lines = llmResponse.lines()

            // 修正点: "YES"が含まれているか、より柔軟にチェック
            val useToolsLine = lines.find { it.trim().startsWith("USE_TOOLS:", ignoreCase = true) }
            val useTools = useToolsLine?.substringAfter(":")?.trim().equals("YES", ignoreCase = true)

            if (useTools) {
                // 修正点: 正規表現から、より安全な行単位の検索に変更
                val toolName = lines.find { it.trim().startsWith("TOOL_NAME:", ignoreCase = true) }
                    ?.substringAfter(":")?.trim()
                val argumentsText = lines.find { it.trim().startsWith("ARGUMENTS:", ignoreCase = true) }
                    ?.substringAfter(":")?.trim()

                println("🔍 DEBUG: Parsing decision: useTools=$useTools, toolName=$toolName, argumentsText=$argumentsText")

                if (toolName != null && argumentsText != null) {
                    val toolCall = MCPToolCall(
                        serverUrl = server.baseUrl,
                        serverName = server.id,
                        toolName = toolName,
                        arguments = parseArguments(argumentsText)
                    )
                    println("🔍 DEBUG: Successfully created tool call: $toolCall")
                    ToolDecision(true, listOf(toolCall))
                } else {
                    println("🔍 DEBUG: Parsing failed: TOOL_NAME or ARGUMENTS not found despite USE_TOOLS was YES.")
                    ToolDecision(false, emptyList())
                }
            } else {
                println("🔍 DEBUG: Parsing decision: USE_TOOLS is NO or not found.")
                ToolDecision(false, emptyList())
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: Error parsing tool decision: ${e.message}")
            ToolDecision(false, emptyList())
        }
    }

    /**
     * 📦 引数文字列をMapに変換
     */
    private fun parseArguments(argumentsText: String): Map<String, Any> {
        return try {
            val jsonElement = json.parseToJsonElement(argumentsText)
            jsonElement.jsonObject.mapValues { (_, value) ->
                (value as? JsonPrimitive)?.content ?: value.toString()
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: Error parsing arguments: '$argumentsText' - ${e.message}")
            emptyMap()
        }
    }

    /**
     * ⚙️ MCPツールを実際に実行
     */
    private suspend fun executeTools(toolCalls: List<MCPToolCall>): String {
        val results = mutableListOf<String>()

        for (toolCall in toolCalls) {
            try {
                println("🔍 DEBUG: Executing tool ${toolCall.toolName} on ${toolCall.serverUrl} with args: ${toolCall.arguments}")
                val result = mcpClient.callTool(
                    toolCall.serverUrl,
                    toolCall.serverName,
                    toolCall.toolName,
                    toolCall.arguments
                )
                println("🔍 DEBUG: Tool result: $result")
                results.add("✅ ${toolCall.toolName}: $result")
            } catch (e: Exception) {
                println("🔍 DEBUG: Tool execution error: ${e.message}")
                results.add("❌ ${toolCall.toolName}: エラー - ${e.message}")
            }
        }
        return results.joinToString("\n")
    }

    /**
     * 📝 ツール実行結果をLLMで整形
     */
    private suspend fun formatToolResult(
        originalMessage: String,
        toolResult: String,
        llmProvider: LLMProvider
    ): String {
        val formatPrompt = """
            ユーザーからの質問に対してMCPツールを実行しました。
            結果を分かりやすく整形してユーザーに回答してください。

            元の質問: "$originalMessage"
            ツール実行結果:
            $toolResult

            ユーザーフレンドリーな形で、結果を要約・整形して回答してください。
        """.trimIndent()
        return llmProvider.generateResponse(formatPrompt, emptyList())
    }

    // 📋 データクラス定義
    data class ToolDecision(
        val useTools: Boolean,
        val toolCalls: List<MCPToolCall>
    )

    data class MCPToolCall(
        val serverUrl: String,
        val serverName: String,
        val toolName: String,
        val arguments: Map<String, Any>
    )
}
