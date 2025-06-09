package com.example.mcpclientapp2.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// 📤 MCP Serverに送信するリクエストの基本構造
@Serializable
data class MCPRequest(
    val jsonrpc: String = "2.0",  // JSON-RPC 2.0プロトコル
    val id: String,               // リクエストID（レスポンスとの対応付け用）
    val method: String,           // 呼び出すメソッド名
    val params: Map<String, JsonElement> = emptyMap()  // 🔧 シリアライゼーション対応
)

// 🔧 MCPツール呼び出し用のデータクラス
@Serializable
data class MCPToolCall(
    val name: String,                    // ツール名
    val arguments: Map<String, JsonElement>      // ツールの引数
)

// 📡 MCP Server接続確認用のPingリクエスト
@Serializable
data class MCPPingRequest(
    val jsonrpc: String = "2.0",
    val id: String = "ping",
    val method: String = "ping"
)