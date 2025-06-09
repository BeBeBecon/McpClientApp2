# 🔑 セキュアなAPIキー設定ガイド

## 📋 概要
APIキーを`local.properties`ファイルで管理し、BuildConfigを通じて安全にアクセスする仕組みです。

## 🛡️ セキュリティの特徴
- ✅ **Gitリポジトリに含まれない**: `local.properties`は`.gitignore`に含まれる
- ✅ **ソースコードに直接記載しない**: BuildConfigを介してアクセス
- ✅ **ビルド時に埋め込み**: 実行時には暗号化された形で存在

## 🔧 設定手順

### **1. APIキーの設定**
`local.properties`ファイルを編集：
```properties
# 🔑 APIキー設定
ANTHROPIC_API_KEY=xxxxxxxxxxxx
GEMINI_API_KEY=xxxxxxxxxxxxxxxxx
```

### **2. ビルド実行**
```bash
./gradlew assembleDebug
```

### **3. 使用方法**
Kotlinコードで以下のように使用：
```kotlin
// 🔑 安全なAPIキー取得
val anthropicKey = BuildConfig.ANTHROPIC_API_KEY
val geminiKey = BuildConfig.GEMINI_API_KEY
```

## ⚠️ 注意事項

### **セキュリティ**
- `local.properties`をGitにコミットしない
- 本番環境では追加のセキュリティ対策を実施
- APIキーの定期的な更新を推奨

### **チーム開発**
- 各開発者が独自の`local.properties`を作成
- APIキーの共有は安全な方法で実施
- テスト用とプロダクション用のキーを分離

## 🚀 メリット
- 📁 **ファイル分離**: 設定とコードの分離
- 🔒 **版管理安全**: APIキーがリポジトリに残らない
- 🛠️ **開発効率**: 環境ごとの設定切り替えが容易
- 🔧 **保守性**: APIキー変更時のコード修正不要