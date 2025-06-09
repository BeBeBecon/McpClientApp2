# 🔑 オプショナルAPIキー設定ガイド

## 📋 概要
APIキーを選択的に設定し、利用可能なLLMプロバイダーのみを表示する仕組みです。

## 🎛️ 動的UI制御の特徴
- ✅ **利用可能なAPIキーのみ表示**: 設定されていないプロバイダーは非表示
- ✅ **自動デフォルト選択**: 利用可能な最初のプロバイダーを自動選択
- ✅ **エラー防止**: 未設定APIキーでのエラーを回避

## 🔧 設定パターン

### **パターン1: Geminiのみ使用（現在の設定）**
```properties
# local.properties
# ANTHROPIC_API_KEY=  ← コメントアウト
GEMINI_API_KEY=your-gemini-api-key-here
```

**結果:**
- 🌟 Geminiボタンのみ表示
- 🚫 Anthropicボタンは非表示
- 🎯 Geminiが自動選択される

### **パターン2: 両方利用**
```properties
# local.properties
ANTHROPIC_API_KEY=sk-ant-api03-xxxxxxxxxxxx
GEMINI_API_KEY=AIzaSyxxxxxxxxxxxxxxxxx
```

**結果:**
- 🎛️ 両方のボタンが表示
- 🌟 Geminiが優先的に選択される

### **パターン3: Anthropicのみ**
```properties
# local.properties
ANTHROPIC_API_KEY=sk-ant-api03-xxxxxxxxxxxx
# GEMINI_API_KEY=  ← コメントアウト
```

**結果:**
- 🏛️ Anthropicボタンのみ表示
- 🎯 Anthropicが自動選択される

## 🔧 BuildConfigフラグ

アプリ内でAPIキーの有無を確認：
```kotlin
// APIキーの有無をチェック
if (BuildConfig.HAS_ANTHROPIC_API_KEY) {
    // Anthropic利用可能
}

if (BuildConfig.HAS_GEMINI_API_KEY) {
    // Gemini利用可能
}
```

## 🚀 将来のAnthropic追加手順

Anthropic APIキーを取得したら：

1. **local.propertiesを編集**:
```properties
ANTHROPIC_API_KEY=your-actual-anthropic-key
GEMINI_API_KEY=your-gemini-api-key-here
```

2. **アプリを再ビルド**:
```bash
./gradlew clean assembleDebug
```

3. **結果**: 両方のLLMプロバイダーが利用可能に！

## 📱 UI動作

### **ステータス表示**
- 起動時: "Available LLMs: Gemini"
- 両方利用可能時: "Available LLMs: Anthropic, Gemini"

### **エラーハンドリング**
- 未設定プロバイダー選択時: 警告メッセージ表示
- APIキーなし: ボタン自体が非表示

## 🎯 メリット
- 🔒 **セキュリティ**: 不要なAPIキー要求なし
- 🎨 **UX向上**: 利用できない機能は非表示
- 🔧 **開発効率**: 段階的な機能追加が可能
- 🚀 **拡張性**: 新しいプロバイダー追加が容易