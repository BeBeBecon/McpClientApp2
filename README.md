# MCP Client App2

MCP（Model Context Protocol）サーバーと連携するAIチャットクライアントアプリです。

## 🚀 セットアップ

### API キー設定
`local.properties` に API キーを追加:
```properties
GEMINI_API_KEY="your_gemini_api_key_here"
ANTHROPIC_API_KEY="your_anthropic_api_key_here"  # オプション
```

API キー取得:
- **Gemini**: [Google AI Studio](https://makersuite.google.com/app/apikey)
- **Anthropic**: [Anthropic Console](https://console.anthropic.com/)

### インストール
```bash
git clone [リポジトリURL]
cd McpClientApp2
# API キーを local.properties に設定
./gradlew installDebug
```

## 📱 使用方法

1. アプリを起動
2. Server URL を設定（例: `localhost:5010`）
3. AI Provider を選択（Gemini推奨）
4. **Ping** で接続確認
5. 自然言語でチャット開始

### 対応サーバー
| サーバー | ポート | キーワード例 |
|---------|--------|-------------|
| Weather | 5010 | 天気、weather、気温 |
| Calendar | 5011 | 予定、スケジュール、会議 |
| Maps | 5012 | 地図、場所、レストラン |

### 使用例
```
今日の千葉の天気を教えて
明日14時に会議をスケジュール
東京駅への行き方を教えて
```

## 🔧 MCP Server追加方法

### 1. サーバー設定追加
`assets/mcp_servers.json` に新しいサーバーを追加:
```json
{
  "id": "your_service",
  "name": "Your Server", 
  "baseUrl": "http://localhost:XXXX",
  "enabled": true,
  "toolKeywords": ["キーワード1", "keyword2"]
}
```

### 2. 必要な手順
1. 新しいMCPサーバーアプリを起動
2. 上記JSONを `mcp_servers.json` に追加
3. `toolKeywords` でキーワードマッチングを設定
4. アプリを再起動

### 3. サーバー要件
- `GET /{service_id}/tools` - 利用可能ツール一覧
- `GET /{service_id}/call/{tool_name}` - ツール実行

## 🐛 トラブルシューティング

- **接続エラー**: MCPサーバーの起動状況を確認
- **AI応答なし**: API キー設定とネット接続を確認  
- **ツール未実行**: キーワードマッチングとLogcatを確認

## 📋 要件

- Android 8.0 (API 26) 以上
- インターネット権限
- Gemini または Anthropic API キー