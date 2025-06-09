package com.example.mcpclientapp2

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// 🤖 LLMプロバイダーの抽象基底クラス（AnthropicとGeminiで共通インターフェース）
sealed class LLMProvider {
    abstract suspend fun generateResponse(message: String, tools: List<Any> = emptyList()): String
}

// 🏛️ Anthropic Claude用のプロバイダー実装
class AnthropicProvider(private val apiKey: String) : LLMProvider() {
    private val client = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    override suspend fun generateResponse(message: String, tools: List<Any>): String {
        return withContext(Dispatchers.IO) {  // 🧵 バックグラウンドスレッドで実行
            try {
                // 🔒 APIキーの有効性チェック
                if (apiKey.isEmpty() || apiKey == "your-anthropic-api-key-here") {
                    return@withContext "❌ Anthropic API key is not properly configured. Please set your API key in local.properties."
                }

                // 📤 Claude APIにリクエスト送信（通常会話）
                val response = client.messages().create(
                    MessageCreateParams.builder()
                        .model(Model.CLAUDE_3_5_SONNET_20241022)  // 🧠 使用モデル指定
                        .maxTokens(1024)                          // 📏 最大トークン数制限
                        .messages(
                            listOf(
                                MessageParam.builder()
                                    .role(MessageParam.Role.USER)
                                    .content(buildConversationPrompt(message, tools))
                                    .build()
                            )
                        )
                        .build()
                )

                // 📥 レスポンスからテキスト抽出（改良版）
                val contentList = response.content()
                if (contentList.isNotEmpty()) {
                    val contentBlock = contentList[0]
                    if (contentBlock.isText()) {
                        val textBlock = contentBlock.text().orElse(null)
                        textBlock?.text() ?: "🤔 I received your message but couldn't generate a response."
                    } else {
                        "📝 I received a non-text response. Please try rephrasing your question."
                    }
                } else {
                    "🔇 No content received from Claude. Please try again."
                }
            } catch (e: Exception) {
                "❌ Claude API Error: ${e.message ?: "Unknown error occurred"}"
            }
        }
    }

    // 💬 通常会話用のプロンプト構築（日本語対応版）
    private fun buildConversationPrompt(userMessage: String, tools: List<Any>): String {
        val toolInfo = if (tools.isNotEmpty()) {
            "\n\nNote: I have access to ${tools.size} MCP tools if needed for specific tasks."
        } else {
            ""
        }

        // 🇯🇵 日本語判定
        val isJapanese = containsJapanese(userMessage)

        return if (isJapanese) {
            """
            あなたは親しみやすいAIアシスタントです。ユーザーのメッセージに日本語で自然に返答してください。
            
            ユーザーメッセージ: $userMessage$toolInfo
            
            会話調で親しみやすく、役立つ情報を含んだ日本語の回答をお願いします。必要に応じて詳しい説明も含めてください。
            """.trimIndent()
        } else {
            """
            You are a helpful AI assistant. Please respond naturally to the user's message.
            
            User message: $userMessage$toolInfo
            
            Respond in a conversational, helpful manner. If the user is asking about general topics, provide informative and engaging answers. Keep responses concise but thorough.
            """.trimIndent()
        }
    }

    // 🇯🇵 日本語文字が含まれているかチェック
    private fun containsJapanese(text: String): Boolean {
        return text.any { char ->
            // ひらがな、カタカナ、漢字のUnicode範囲をチェック
            char.code in 0x3040..0x309F ||  // ひらがな
            char.code in 0x30A0..0x30FF ||  // カタカナ  
            char.code in 0x4E00..0x9FAF ||  // CJK統合漢字
            char.code in 0x3400..0x4DBF     // CJK拡張A
        }
    }
}

// 🌟 Google Gemini用のプロバイダー実装（Gemini-1.5-Flash対応版）
class GeminiProvider(private val apiKey: String) : LLMProvider() {
    // 🌐 HTTP通信用のOkHttpクライアント
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // 📄 JSON解析設定
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 💭 会話履歴を保存（マルチターン対応）
    private val conversationHistory = mutableListOf<ConversationTurn>()
    
    // 📝 会話ターンのデータクラス
    data class ConversationTurn(
        val role: String,  // "user" or "assistant"
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    override suspend fun generateResponse(message: String, tools: List<Any>): String {
        return withContext(Dispatchers.IO) {
            try {
                // 🔒 APIキーの有効性チェック
                if (apiKey.isEmpty() || apiKey == "your-gemini-api-key-here") {
                    return@withContext "❌ Gemini API key is not properly configured. Please set your API key in local.properties."
                }

                // 📝 ユーザーメッセージを履歴に追加
                conversationHistory.add(ConversationTurn("user", message))

                // 🧠 実際のGemini-1.5-Flash APIを呼び出し
                val response = callGeminiAPI(message, tools)

                // 📝 アシスタントレスポンスを履歴に追加
                conversationHistory.add(ConversationTurn("assistant", response))

                // 🗑️ 履歴が長くなりすぎた場合は古いものを削除（最新10ターンを保持）
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                    conversationHistory.removeAt(0)  // user-assistant ペアで削除
                }

                response
                
            } catch (e: Exception) {
                "❌ Gemini Error: ${e.message ?: "Unknown error occurred"}"
            }
        }
    }

    // 🌟 Gemini-1.5-Flash APIを実際に呼び出す
    private suspend fun callGeminiAPI(message: String, tools: List<Any>): String {
        try {
            // 🔗 Gemini API エンドポイント（Gemini-1.5-Flash指定）
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            // 📚 会話履歴を含むコンテキスト構築
            val contextualPrompt = buildContextualPrompt(message, tools)

            // 📦 リクエストボディ構築とJSON変換
            val jsonBody = """
            {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": "${contextualPrompt.replace("\"", "\\\"").replace("\n", "\\n")}"
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.7,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 1024
                },
                "safetySettings": [
                    {
                        "category": "HARM_CATEGORY_HARASSMENT",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    },
                    {
                        "category": "HARM_CATEGORY_HATE_SPEECH",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    },
                    {
                        "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    },
                    {
                        "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
                        "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                    }
                ]
            }
            """.trimIndent()

            // 🌐 HTTP リクエスト送信
            val request = Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(
                    "application/json".toMediaType(),
                    jsonBody
                ))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return "❌ Gemini API request failed: ${response.code} ${response.message}"
            }

            // 📥 レスポンス解析
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject
            val candidates = jsonResponse["candidates"]?.jsonArray
            
            if (candidates?.isNotEmpty() == true) {
                val content = candidates[0].jsonObject["content"]?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                
                if (parts?.isNotEmpty() == true) {
                    val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                    return text ?: "🤔 Gemini responded but no text content was found."
                }
            }

            return "🔇 No valid response from Gemini-1.5-Flash. Please try again."

        } catch (e: Exception) {
            return "❌ API call failed: ${e.message}"
        }
    }

    // 📚 コンテキストを含むプロンプトを構築
    private fun buildContextualPrompt(currentMessage: String, tools: List<Any>): String {
        val toolContext = if (tools.isNotEmpty()) {
            "\n\nNote: I have access to ${tools.size} MCP tools if needed for specific tasks."
        } else {
            ""
        }

        // 📝 最近の会話履歴（最新4ターン）
        val recentHistory = if (conversationHistory.size > 1) {
            val recent = conversationHistory.takeLast(6).dropLast(1) // 現在のメッセージは除く
            val historyText = recent.joinToString("\n") { "${it.role}: ${it.message}" }
            "\n\nRecent conversation:\n$historyText\n"
        } else {
            ""
        }

        // 🌏 日本語判定とプロンプト生成
        val isJapanese = containsJapanese(currentMessage)
        
        return if (isJapanese) {
            """
            あなたはGemini-1.5-Flash、Googleの親しみやすいAIアシスタントです。ユーザーのメッセージに日本語で自然に会話してください。
            
            ${if (recentHistory.isNotEmpty()) "これまでの会話の流れ:$recentHistory" else ""}
            
            現在のユーザーメッセージ: $currentMessage$toolContext
            
            親しみやすく、役立つ情報を含んだ日本語の回答をお願いします。会話調で自然に答えてください。
            """.trimIndent()
        } else {
            """
            You are Gemini-1.5-Flash, a helpful AI assistant. Please respond naturally and conversationally to the user's message.
            
            ${if (recentHistory.isNotEmpty()) "Context from our conversation:$recentHistory" else ""}
            
            Current user message: $currentMessage$toolContext
            
            Please provide a helpful, engaging, and informative response. Keep it conversational but informative.
            """.trimIndent()
        }
    }

    // 🇯🇵 日本語文字が含まれているかチェック
    private fun containsJapanese(text: String): Boolean {
        return text.any { char ->
            // ひらがな、カタカナ、漢字のUnicode範囲をチェック
            char.code in 0x3040..0x309F ||  // ひらがな
            char.code in 0x30A0..0x30FF ||  // カタカナ  
            char.code in 0x4E00..0x9FAF ||  // CJK統合漢字
            char.code in 0x3400..0x4DBF     // CJK拡張A
        }
    }


}