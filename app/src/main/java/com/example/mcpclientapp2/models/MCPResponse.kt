package com.example.mcpclientapp2.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// 📥 MCP Serverからのレスポンスの基本構造
@Serializable
data class MCPResponse(
    val jsonrpc: String,          // JSON-RPC 2.0プロトコル
    val id: String?,              // リクエストIDの対応
    val result: MCPResult? = null,    // 成功時の結果
    val error: MCPError? = null       // エラー時の詳細
)

// ✅ MCP Serverからの成功レスポンス内容
@Serializable
data class MCPResult(
    val tools: List<MCPTool>? = null,       // 利用可能ツール一覧
    val content: List<MCPContent>? = null   // ツール実行結果
)

// 🔧 MCPツールの定義情報
@Serializable
data class MCPTool(
    val name: String,                    // ツール名
    val description: String?,            // ツールの説明
    val inputSchema: Map<String, JsonElement>    // 🔧 JsonElementを直接使用（シリアライザー指定不要）
)

// 📄 MCPコンテンツ（ツール実行結果など）
@Serializable
data class MCPContent(
    val type: String,     // コンテンツタイプ（text, imageなど）
    val text: String?     // テキストコンテンツ
)

// ❌ MCP Serverからのエラーレスポンス
@Serializable
data class MCPError(
    val code: Int,        // エラーコード
    val message: String   // エラーメッセージ
)