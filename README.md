# 红果短剧网页版 App（Hongguo Web Shell）

> 把红果短剧**官方网页版**装成一个 Android App：自动隐藏「前往 App / 扫码下载」这类反复弹出的导流弹层，拦掉唤起 App 的跳转和 APK 下载。
>
> **免 root · 零权限 · 不破解 · 不注入无障碍 · 只申请联网**

一个约 200 行的 Java 文件，没有第三方库，没有 native 库，没有统计 SDK。
不喜欢用别人编译好的安装包？工程就在这儿，自己跑一遍 CI 就有一个你自己签名的 APK。

> ⚠️ 非官方项目，与红果短剧 / 字节跳动无任何关系。仅供个人学习与自用，请支持正版。

---

## 目录

- [为什么做这个](#为什么做这个)
- [它做什么 / 不做什么](#它做什么--不做什么)
- [功能](#功能)
- [安装](#安装)
- [使用](#使用)
- [工作原理](#工作原理)
- [权限说明](#权限说明)
- [常见问题](#常见问题)
- [自定义](#自定义)
- [已知限制](#已知限制)
- [目录结构](#目录结构)
- [参与](#参与)
- [免责声明](#免责声明)
- [License](#license)

---

## 为什么做这个

在手机上用浏览器打开红果短剧网页版（`hongguoduanju.com`），最烦的往往不是视频内容本身，而是页面一路推你去装 App：

- 中间的橙色大按钮「前往「红果短剧」免费看全集」；
- 顶部的下载条、底部的扫码条；
- 点到某些位置会被 `snssdk1128://` 之类的 scheme 直接拉去应用商店或唤起 App。

而直接装官方 App，又会带回来一堆你未必想要的东西：开屏广告、信息流、推送、各种权限。

这个项目走的是第三条路：**继续用网页版，但把"装 App"这套导流全部藏掉**，再用一个 WebView 外壳让它用起来像个正常 App（全屏、返回键、视频横屏）。

它不碰红果 App 本体，不读无障碍树，不改任何网络请求，也不破解签名——所以既不用担心反作弊检测，也不会影响银行类 App 的运行环境。

## 它做什么 / 不做什么

| | 内容 |
| --- | --- |
| ✅ 做 | 用系统 WebView 打开 `https://hongguoduanju.com/`；页面加载完注入一段 JS，按**可见文字**把导流块设为 `display:none`；用 `MutationObserver` 跟随单页应用的动态插入继续隐藏；拦掉所有非 `http(s)` 的 scheme 以及 `.apk` 下载；处理视频全屏、返回键、旋转 |
| ❌ 不做 | 不修改 App 安装包、不破解签名、不做中间人抓包、不拦截视频流、不申请无障碍/存储/通讯录/定位权限、不内置任何广告或统计 SDK |

一句话：**它只是"官方网页版 + 一层去干扰的壳"**，不是去广告神器，也不改变网页版本身的清晰度与剧集范围。

## 功能

- **零运行时权限**：`AndroidManifest.xml` 里只有 `INTERNET` 一条，安装后不会弹任何授权框。
- **按文字隐藏**：不依赖站点的 class 名，改版后只要文案不变就还能命中；文案变了改一行字符串即可。
- **双重拦截**：`shouldOverrideUrlLoading` 拦唤起 App 的 scheme，`setDownloadListener` 拦下载安装包，页内导流点不动。
- **跟随动态渲染**：`MutationObserver` 监听 DOM 变化，SPA 路由切换后新插入的弹层同样会被隐藏。
- **像 App 的体验**：无地址栏、无状态栏干扰、视频全屏走 `onShowCustomView`、返回键先退全屏再退网页最后才退出。
- **省电省流**：禁缩放、禁混合内容、不预加载、关掉多余的后台行为。

## 安装

### 方式一：直接下 APK（最省事）

1. 打开本仓库的 [Releases](../../releases/latest) 页面；
2. 下载最新的 `app-debug.apk`；
3. 传到手机（微信文件传输助手 / 数据线 / 网盘都行），点开安装；
4. 首次会提示「允许来自此来源的应用」，允许即可。

> 说明：Release 里的 APK 由本仓库的 GitHub Actions 自动编译产出，签名用的是 CI 自动生成的 debug 密钥。它只适合自用；如果你在意"这包到底有没有被改过"，请用方式二自己编。

### 方式二：自己编译（推荐给在意安全的人）

不需要装 Android Studio，用 GitHub Actions 就行：

1. 页面右上角 **Fork** / 或新建仓库后把本目录全部文件推上去（`.github/` 也要）；
2. 进仓库的 **Actions** 标签 → 左侧 `build-apk` → 右侧 **Run workflow**；
3. 3~5 分钟后，进这次运行页面，底部 **Artifacts** 下载 `hongguo-shell-apk.zip`，解压得到 `app-debug.apk`；
4. 传手机安装。

本机编译（需 JDK 17 + Android SDK，`ANDROID_HOME` 指向 SDK 目录）：

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

技术栈：AGP 8.5.2 / Gradle 8.7 / compileSdk 34 / minSdk 24（Android 7.0 及以上）。

## 使用

装好点开就是全屏的网页版，没有地址栏。

- 找剧：首页、分类、榜单、搜索都能用；
- 看剧：点进详情页 → 播放，视频全屏时横屏会自动铺满；
- 返回键：视频全屏 → 先退全屏；网页内 → 后退一页；已经在首页 → 退出 App。

## 工作原理

```
用户点击图标
   │
   ▼
MainActivity 创建 FrameLayout + WebView
   │  WebSettings：开启 JS/DOM Storage，去掉 UA 里的 "; wv"
   ▼
加载 https://hongguoduanju.com/
   │
   ├─ onPageFinished → evaluateJavascript(HIDE_JS)
   │      └─ 按 WORDS 里的文字匹配叶子节点 → display:none
   │      └─ MutationObserver 持续监听，新弹层同样隐藏
   │
   ├─ shouldOverrideUrlLoading → 非 http(s) 一律阻止（snssdk1128:// / intent:// / market:// …）
   ├─ shouldOverrideUrlLoading → 路径以 .apk 结尾一律阻止
   └─ DownloadListener → 任何下载都拦下并提示
```

关键代码都在 [`app/src/main/java/com/hongguo/shell/MainActivity.java`](app/src/main/java/com/hongguo/shell/MainActivity.java)，一共 4 个部分：`HIDE_JS`、`handleUrl`、全屏回调、返回键处理。

## 权限说明

| 权限 | 用途 |
| --- | --- |
| `android.permission.INTERNET` | 访问网页 |

就这一条。没有存储权限（不读不写你的文件）、没有无障碍（不模拟点击）、没有定位、没有读取应用列表、没有后台服务、没有开机自启。

安装后可以自己去「设置 → 应用 → 红果网页版 → 权限」核对。

## 常见问题

**Q：打开是白屏 / 一直转圈？**
A：先确认浏览器能正常打开 `hongguoduanju.com`（站点本身偶尔会抽风）。其次是系统 WebView 太旧：去应用商店更新「Android System WebView」或 Chrome。

**Q：橙色按钮还在 / 又弹出来了？**
A：说明它的文案不在 `WORDS` 里，或者结构特殊（比如是纯图标按钮）。把按钮上的完整文字提个 issue，加一行字符串就能修好。

**Q：为什么清晰度和剧集比 App 少？**
A：这是网页版本身的限制，外壳改变不了。本项目只处理导流弹层，不涉及视频源。

**Q：会不会封号？**
A：不登录就无所谓。这个 App 的流量和你在手机浏览器里看网页版完全等价，没有伪造任何请求或签名。

**Q：安全吗？我凭什么信？**
A：源码一共一个 Java 文件 + 一个清单文件，通读一遍十分钟够。没有 native 库（`lib/` 下什么都没有）、没有动态加载、没有第三方依赖。不放心就别下 Release，按「方式二」自己编译，签名密钥在你自己手里。

**Q：能不能也做别的短剧站？**
A：改 `HOME` 常量就行，`WORDS` 按目标站点调整。

## 自定义

改 `app/src/main/java/com/hongguo/shell/MainActivity.java`：

| 想改什么 | 改哪里 |
| --- | --- |
| 换站点 | `private static final String HOME = "https://hongguoduanju.com/";` |
| 换/加要隐藏的文字 | `WORDS` 数组，加一条字符串即可 |
| 不拦下载 | 删掉 `setDownloadListener(...)` 那一段 |
| 强制横屏 | `AndroidManifest.xml` 的 `<activity>` 加 `android:screenOrientation="sensorLandscape"` |
| 想用 Chrome 内核 | 引入 `androidx.webkit` 或换用 Custom Tabs（会增加依赖） |

改完提交一次，Actions 会自动重新出包。

## 已知限制

- 只对「可见文字」有效：纯图标按钮、canvas 绘制的弹层抓不到。
- 站点改版后文案变了，需要更新 `WORDS`（个人维护，更新可能滞后）。
- 网页版的播放体验、清晰度、剧集完整度都不如官方 App。
- 依赖系统 WebView，老设备上视频解码能力有限。

## 目录结构

```
hongguo-shell/
├── .github/workflows/build.yml        # GitHub Actions：推送即自动出 APK
├── app/
│   ├── build.gradle.kts               # 模块配置（UTF-8 编译，minSdk 24）
│   └── src/main/
│       ├── AndroidManifest.xml        # 只声明 INTERNET
│       ├── java/com/hongguo/shell/
│       │   └── MainActivity.java      # 全部逻辑，约 200 行
│       └── res/drawable/ic_launcher.xml
├── build.gradle.kts
├── settings.gradle.kts
├── README.md
└── LICENSE
```

## 参与

- 发现某个导流按钮没被隐藏：开 issue，附**按钮上的完整文字**（截图更好）；
- 站点改版导致大面积失效：开 issue 说明现象；
- 欢迎 PR，但请保持"零第三方依赖、零多余权限"这两条底线。

## 免责声明

本项目为个人学习性质的第三方工具，与红果短剧、字节跳动及其关联公司无任何隶属或合作关系。
项目不提供、不存储、不分发任何视频内容，所有内容均来自官方网页版本体。
使用者应自行遵守当地法律法规与平台服务条款；因使用本项目产生的任何后果由使用者自行承担。

## License

[MIT](LICENSE)
