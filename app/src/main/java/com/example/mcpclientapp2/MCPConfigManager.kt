package com.example.mcpclientapp2

import android.content.Context
import java.io.IOException

/**
 * 📁 MCP設定ファイル管理クラス
 * assetsフォルダとローカルストレージでMCP Server設定を管理
 */
class MCPConfigManager(private val context: Context) {
    
    private val CONFIG_FILE = "mcp_servers.json"
    
    /**
     * 📥 assetsからMCP Server設定を読み込み
     */
    fun loadConfigFromAssets(): String? {
        return try {
            context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            null
        }
    }
    
    /**
     * 💾 ローカルストレージに設定を保存
     */
    fun saveConfig(jsonConfig: String): Boolean {
        return try {
            context.openFileOutput(CONFIG_FILE, Context.MODE_PRIVATE).use { output ->
                output.write(jsonConfig.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 📂 ローカルストレージから設定を読み込み
     */
    fun loadConfig(): String? {
        return try {
            context.openFileInput(CONFIG_FILE).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // ローカルファイルがない場合はassetsから読み込み
            loadConfigFromAssets()
        }
    }
    
    /**
     * 🔄 設定をMCPServerRegistryに適用
     */
    fun applyConfigToRegistry(): Boolean {
        val config = loadConfig()
        return if (config != null) {
            MCPServerRegistry.importFromJson(config)
        } else {
            false
        }
    }
    
    /**
     * 📝 現在の設定をローカルに保存
     */
    fun saveCurrentConfig(): Boolean {
        val config = MCPServerRegistry.exportToJson()
        return saveConfig(config)
    }
    
    /**
     * 🆔 使用方法のガイド文字列を生成
     */
    fun getUsageGuide(): String {
        return """
            📁 MCP Server 設定ガイド
            
            🔧 設定ファイル場所:
            app/src/main/assets/mcp_servers.json
            
            📝 設定例:
            ${MCPServerRegistry.generateSampleConfig()}
            
            🛠️ 追加手順:
            1. 上記のJSON形式で新しいサーバーを定義
            2. assets/mcp_servers.json に追加
            3. アプリを再起動
            
            🔑 重要なフィールド:
            • id: 一意のサーバーID
            • baseUrl: MCPサーバーのURL
            • toolKeywords: 自動判定用キーワード
            • enabled: 有効/無効の切り替え
        """.trimIndent()
    }
}
