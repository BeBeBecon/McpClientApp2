# API 24サポートが必要な場合の代替案

## 🗑️ アダプティブアイコンファイルを削除

以下のコマンドでアダプティブアイコンファイルを削除：

```bash
rm /Users/taku/install/McpClientApp2/app/src/main/res/mipmap-anydpi/ic_launcher.xml
rm /Users/taku/install/McpClientApp2/app/src/main/res/mipmap-anydpi/ic_launcher_round.xml
```

その後、build.gradle.ktsでminSdk = 24に戻す：

```kotlin
defaultConfig {
    minSdk = 24  // Android 7.0
}
```

## ⚠️ 注意点
- アダプティブアイコンの恩恵を受けられない
- 現代的なUI/UXから逸脱する
- 保守性が低下する

## 📊 推奨
**minSdk = 26**への変更が現実的で適切な解決策です。