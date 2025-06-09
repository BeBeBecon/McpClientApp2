package com.example.mcpclientapp2

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mcpclientapp2.databinding.ActivityMainBinding
import com.example.mcpclientapp2.models.MCPTool
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // 🎨 ViewBinding（型安全なView参照）
    private lateinit var binding: ActivityMainBinding
    // 🌐 MCP通信クライアント
    private lateinit var mcpClient: MCPClient
    // 🤖 現在選択中のLLMプロバイダー
    private lateinit var currentLLMProvider: LLMProvider
    // ⚙️ MCPツール実行エンジン
    private lateinit var mcpToolExecutor: MCPToolExecutor
    // 📁 MCP設定管理
    private lateinit var mcpConfigManager: MCPConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 📱 クライアントとプロバイダーの初期化
        mcpClient = MCPClient()
        mcpToolExecutor = MCPToolExecutor(mcpClient)
        mcpConfigManager = MCPConfigManager(this)
        
        // 🔧 MCP設定を読み込み
        mcpConfigManager.applyConfigToRegistry()

        // 🔧 利用可能なLLMプロバイダーを設定
        setupAvailableLLMProviders()
        setupClickListeners()
        
        // 🎉 ウェルカムメッセージ
        addWelcomeMessage()
    }

    // 🎉 ウェルカムメッセージの表示
    private fun addWelcomeMessage() {
        val availableServers = MCPServerRegistry.getEnabledServers()
        val serverList = availableServers.joinToString(", ") { it.name }
        
        val welcomeMessage = """
            👋 Welcome to MCP Client!
            
            You can:
            • Chat normally with AI
            • Use MCP tools automatically
            • Switch between AI providers
            
            🛠️ Available MCP Services:
            $serverList
            
            Try saying:
            • "今日の予定を教えて" (Calendar - Port 5011)
            • "明日14時に会議をスケジュール" (Calendar - Port 5011)
            • "今日の千葉の天気を教えて" (Weather - Port 5010)
            • "東京駅への行き方を教えて" (Maps - Port 5012)
            • "近くのレストランを探して" (Maps - Port 5012)
        """.trimIndent()
        
        addMessageToChat("System", welcomeMessage, false)
    }

    // 🔧 利用可能なLLMプロバイダーの設定とUI制御
    private fun setupAvailableLLMProviders() {
        // 🔍 APIキーの有無をチェック
        val hasAnthropic = BuildConfig.HAS_ANTHROPIC_API_KEY
        val hasGemini = BuildConfig.HAS_GEMINI_API_KEY

        // 🎛️ UI要素の表示/非表示制御
        binding.anthropicButton.visibility = if (hasAnthropic) View.VISIBLE else View.GONE
        binding.geminiButton.visibility = if (hasGemini) View.VISIBLE else View.GONE

        // 🔧 デフォルトプロバイダーの設定
        currentLLMProvider = when {
            hasGemini -> {
                // 🌟 Geminiが利用可能な場合、Geminiをデフォルトに設定
                binding.llmToggleGroup.check(binding.geminiButton.id)
                GeminiProvider(BuildConfig.GEMINI_API_KEY)
            }
            hasAnthropic -> {
                // 🏛️ Anthropicが利用可能な場合
                binding.llmToggleGroup.check(binding.anthropicButton.id)
                AnthropicProvider(BuildConfig.ANTHROPIC_API_KEY)
            }
            else -> {
                // ❌ どちらも利用できない場合（エラー防止用）
                updateStatus("⚠️ No API keys configured")
                GeminiProvider("")  // ダミープロバイダー
            }
        }

        // 📊 利用可能なプロバイダー情報を表示
        val availableProviders = mutableListOf<String>()
        if (hasAnthropic) availableProviders.add("Anthropic")
        if (hasGemini) availableProviders.add("Gemini")
        
        updateStatus("🚀 Ready - Available: ${availableProviders.joinToString(", ")}")
    }

    // 👆 各ボタンのクリックイベント設定
    private fun setupClickListeners() {
        // 📡 Pingボタン：MCP Server接続テスト
        binding.pingButton.setOnClickListener {
            pingServer()
        }

        // 📤 送信ボタン：メッセージ送信
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        // ⌨️ Enterキーでの送信対応
        binding.messageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }

        // 🔄 LLMプロバイダー切り替え（利用可能なもののみ）
        binding.llmToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentLLMProvider = when (checkedId) {
                    binding.anthropicButton.id -> {
                        // 🔧 Anthropicが利用可能かチェック
                        if (BuildConfig.HAS_ANTHROPIC_API_KEY) {
                            updateStatus("🏛️ Switched to Anthropic")
                            AnthropicProvider(BuildConfig.ANTHROPIC_API_KEY)
                        } else {
                            updateStatus("❌ Anthropic API key not configured")
                            return@addOnButtonCheckedListener
                        }
                    }
                    binding.geminiButton.id -> {
                        // 🔧 Geminiが利用可能かチェック
                        if (BuildConfig.HAS_GEMINI_API_KEY) {
                            updateStatus("🌟 Switched to Gemini")
                            GeminiProvider(BuildConfig.GEMINI_API_KEY)
                        } else {
                            updateStatus("❌ Gemini API key not configured")
                            return@addOnButtonCheckedListener
                        }
                    }
                    else -> currentLLMProvider
                }
            }
        }
    }

    // 📡 MCP ServerにPing送信して接続確認
    private fun pingServer() {
        val serverUrl = binding.serverUrlInput.text.toString().trim()
        if (serverUrl.isEmpty()) {
            updateStatus("⚠️ Please enter server URL")
            return
        }

        // ⏳ 非同期でPing実行（UIをブロックしない）
        lifecycleScope.launch {
            updateStatus("📡 Pinging server...")
            val success = mcpClient.pingServer(serverUrl)
            
            if (success) {
                updateStatus("✅ ${getString(R.string.ping_success)}")
                addMessageToChat("System", "🔗 Connected to MCP Server: $serverUrl", false)
                
                // ✅ Ping成功時：SSEでリアルタイム接続開始
                mcpClient.connectToServer(serverUrl) { message ->
                    runOnUiThread {  // 🧵 メインスレッドでUI更新
                        updateStatus("📨 $message")
                    }
                }
            } else {
                updateStatus("❌ ${getString(R.string.ping_failed)}")
                addMessageToChat("System", "❌ Failed to connect to MCP Server", false)
            }
        }
    }

    // 💬 ユーザーメッセージの送信と処理
    private fun sendMessage() {
        val message = binding.messageInput.text.toString().trim()
        if (message.isEmpty()) return

        // 📝 ユーザーメッセージをチャット画面に追加
        addMessageToChat("You", message, true)
        binding.messageInput.text?.clear()

        // ⏳ MCPツール統合処理を実行
        lifecycleScope.launch {
            updateStatus("🧠 Analyzing message...")

            try {
                // 🔧 MCPツール自動実行エンジンでメッセージを処理
                val response = mcpToolExecutor.analyzeAndExecute(message, currentLLMProvider)

                // 🤖 応答をチャット画面に追加
                addMessageToChat("Assistant", response, false)
                updateStatus("✅ Response completed")
                
            } catch (e: Exception) {
                val errorMessage = "Sorry, I encountered an error: ${e.message}"
                addMessageToChat("Assistant", errorMessage, false)
                updateStatus("❌ Error occurred")
            }
        }
    }

    // 💬 チャット画面にメッセージカードを追加（改良版）
    private fun addMessageToChat(sender: String, message: String, isUser: Boolean) {
        val messageView = createMessageView(sender, message, isUser)
        binding.chatContainer.addView(messageView)

        // 📜 新しいメッセージまでスムーズスクロール
        binding.chatScrollView.post {
            binding.chatScrollView.smoothScrollTo(0, binding.chatContainer.height)
        }
    }

    // 🎨 メッセージ表示用のカードビューを作成（スタイリッシュ版）
    private fun createMessageView(sender: String, message: String, isUser: Boolean): View {
        val cardView = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    if (isUser) 48 else 0,  // ユーザーメッセージは右寄せ風
                    8,
                    if (isUser) 0 else 48,  // アシスタントメッセージは左寄せ風
                    8
                )
            }
            
            // 🎨 カードスタイル
            setCardBackgroundColor(
                getColor(
                    when {
                        sender == "System" -> R.color.surface_secondary
                        isUser -> R.color.user_message_bg
                        else -> R.color.assistant_message_bg
                    }
                )
            )
            cardElevation = 2f
            radius = 16f
        }

        val textView = TextView(this).apply {
            // 📝 テキストスタイル
            text = if (sender == "System") message else "$sender: $message"
            textSize = 14f
            setPadding(16, 12, 16, 12)
            setTextColor(getColor(R.color.text_primary))
            setLineSpacing(4f, 1f)  // 🔧 修正: 正しいメソッド呼び出し
            
            // 🎯 システムメッセージは中央揃え
            if (sender == "System") {
                gravity = android.view.Gravity.CENTER
                setTextColor(getColor(R.color.text_secondary))
                textSize = 12f
            }
        }

        cardView.addView(textView)
        return cardView
    }

    // 📊 ステータスメッセージの更新（絵文字付き）
    private fun updateStatus(status: String) {
        binding.statusText.text = status
    }

    // 🧹 アプリ終了時にMCP接続をクリーンアップ
    override fun onDestroy() {
        super.onDestroy()
        mcpClient.disconnect()
    }
}