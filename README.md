# PetRobot

[![CI](https://github.com/taki-codes/pet-robot-portfolio/actions/workflows/android-ci.yml/badge.svg)](https://github.com/taki-codes/pet-robot-portfolio/actions/workflows/android-ci.yml)

[設計・コードの読みどころと検証結果](docs/portfolio.md)

Xiaomi Pad 7 Proを、表情と触れ合いのある卓上ペット端末として使うAndroidアプリです。KotlinとJetpack Composeで実装し、端末内でセンシングし、長時間稼働を評価するための記録を保存します。

## 主な機能

- センサーの状態に応じたペットの表情、タップ・なで操作への反応
- 来訪時のあいさつ、同梱音源による鳴き声・甘える声
- CameraXのフレーム差分・ML Kitの顔数・音量の特徴量を使った在席状態の推定
- 在席と活動量から、休憩・声かけタイミングの候補を判定
- フォアグラウンドサービスのハートビート、熱状態に応じた解析負荷調整
- Roomによる構造化ログ、日次・7日集計、閾値を変えたログ再生
- 「早い・ちょうどよい・遅い」の3ボタンでタイミングを手動評価

「ミー」への反応にはAndroidのオンデバイス音声認識を限定利用します。対応する認識サービスのある端末が必要です。一般的な会話AIやクラウドへの音声送信は実装範囲に含めていません。

## 技術構成

| 領域 | 技術 |
| --- | --- |
| Android UI | Kotlin、Jetpack Compose、Material 3 |
| カメラ・特徴量 | CameraX、ML Kit Face Detection、AudioRecord |
| 保存 | Room |
| ビルド・品質確認 | Gradle Wrapper、JUnit、Android Lint、GitHub Actions |
| Android要件 | Android 14以降（minSdk 34）、compile / target SDK 37 |

## ローカルで動かす

Android Studio、Android SDK API 37、JDK 25を用意します。Gradle Wrapperのバージョンは9.5.0です。

```powershell
git clone https://github.com/taki-codes/pet-robot-portfolio.git
cd pet-robot-portfolio
.\gradlew.bat assembleDebug
```

Android Studioでリポジトリを開き、端末を接続して `app` を実行します。センシングにはカメラ・マイク、稼働通知には通知の権限を許可してください。想定する実機はXiaomi Pad 7 Proです。

生成APKは `app/build/outputs/apk/debug/app-debug.apk` に出力されます。

## モジュールと設計

| モジュール | 責務 |
| --- | --- |
| `:app` | Androidライフサイクル、Compose UI、権限、フォアグラウンドサービス |
| `:sensing` | センサーアダプター、特徴量、状態機械 |
| `:storage` | 構造化ログと集計。アプリ層へ依存しない保存処理 |

検出・状態遷移のロジックはタイムスタンプ付きの値を受け取り、AndroidのContextや実時間に依存せずJVMテストで確認できるようにしています。音声再生に伴う自己反応は、再生ガードや反応の間隔制御で抑えます。

プライバシー上の境界も実装の一部です。`INTERNET` 権限をAPKから除き、生の画像・音声・文字起こし・個人識別用データを保存しない方針で、数値化した観測と状態を記録します。顔数の検出は個人識別には使用しません。

## 検証

Windowsでは次のコマンドを実行します。

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\scripts\verify-v0-guardrails.ps1
```

GitHub ActionsもPRとmainへのpushで同じ種類の確認を行います。ガードレールスクリプトは、組み立て済みAPKのネットワーク権限と、sensing層の直接保存APIを検査します。

長時間稼働、発熱、権限、カメラ・マイクの実挙動は実機で確認する項目です。手順は[テスト計画](docs/test-plan.md)と[24時間センシング試験](docs/24-hour-sensing-test.md)に分けて記載しています。

## 開発中に行った端末確認

元の開発版では、Xiaomi Pad 7 Proで画面を表示したままスリープを防げることと、ADBで稼働状態のチェックポイントを取得できることを確認しました。この確認をもとに、開始・12時間・24時間の同じ手順で記録を取る試験を用意しています。[24時間試験の手順](docs/24-hour-sensing-test.md)

なで操作の音は、端末での聞こえ方についてのフィードバックを受けて音量を調整しました。操作の継続時間、一度のなで操作で一回だけ鳴る条件、再生音による自己反応を抑えるガードは、単体テストで確認しています。公開版の音源と出典は別途記録しています。[なで操作のテスト](docs/stroke-purr-test.md) · [公開版の音源](docs/audio-sources.md)

## 現在の位置付け・制約

- センシングと稼働信頼性を測るv0基盤に、ペットの表情・触れ合いUIを追加した実験段階です。
- 任意のAndroid端末での長時間稼働や、各センサーによる判定精度を保証するものではありません。
- 自律会話、一般向けSTT / TTS / LLM、個人認識、サーバー連携は対象外です。
- 歌声候補の検出は明示的に有効化するデバッグ用の実験です。通常機能や検証済みの分類器としては扱いません。

## 関連資料

- [v0の目的と範囲](docs/v0-spec.md)
- [テスト計画と信頼性ゲート](docs/test-plan.md)
- [在席状態の評価](docs/presence-state-test.md)
- [構造化ログ](docs/structured-room-log-test.md)
- [熱状態への適応](docs/thermal-adaptation-test.md)
- [同梱音声の出典](docs/audio-sources.md)


## この公開用ソースについて

[元のコードとの差分・含める範囲](PORTFOLIO-SOURCE.md)を参照してください。個別の運用情報と元のGit履歴を含まない公開用ソースです。
