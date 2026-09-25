# 影视（VodApp）

一个基于 **量子资源（lzizy）苹果CMS采集接口** 的安卓影视 App，支持**手机 + 安卓电视**。

- 数据源：`https://lzizy1.com/api.php/provide/vod/`（15.5万部，m3u8 直链）
- 播放：Media3 / ExoPlayer 原生 HLS 播放
- 界面：Jetpack Compose（手机竖屏 + 电视横屏通用）
- 功能：首页、分类浏览、搜索、详情、线路/选集切换、播放

---

## 怎么编译出 APK（两条路）

### 路线 A：GitHub 云端编译（推荐，无需装任何东西）

1. 把整个项目文件夹上传到一个 **GitHub 仓库**（公开或私有均可）
2. 进入仓库 → **Actions** 标签 → 左侧选择 `Build APK` → 点 **Run workflow**
3. 等 3~5 分钟编译完成，回到该次运行详情页，底部 **Artifacts** 里下载 `vodapp-debug-apk`
4. 把 APK 传到手机/电视安装即可

> 首次编译会自动下载依赖，时间较长（约 5-10 分钟），属正常。

### 路线 B：本地 Android Studio 编译

1. 电脑安装 [Android Studio](https://developer.android.com/studio)
2. 打开本项目文件夹
3. 等待 Gradle 同步完成后，点 **Build → Build Bundle(s)/APK(s) → Build APK(s)**
4. APK 在 `app/build/outputs/apk/debug/` 目录

---

## 项目结构

```
app/src/main/java/com/vodapp/
├── MainActivity.kt          # 入口 + 主题
├── data/
│   ├── Models.kt            # 数据模型（对应苹果CMS JSON）
│   └── ApiClient.kt         # 接口客户端 + 选集解析
├── ui/
│   ├── AppRoot.kt           # 导航
│   ├── VodViewModel.kt      # 状态管理
│   ├── HomeScreen.kt        # 首页/分类/搜索/列表
│   └── DetailScreen.kt      # 详情/线路/选集
└── player/
    ├── PlayerActivity.kt    # 播放器
    └── DataSourceFactory.kt # 数据源工厂
```

---

## 换数据源 / 加多源

编辑 `data/Models.kt` 里的 `AppConfig.sources`：

```kotlin
object AppConfig {
    val sources = listOf(
        SourceConfig("lzizy", "量子资源", "https://lzizy1.com/api.php/provide/vod/"),
        // 新增源：直链 m3u8 的苹果CMS接口都行
        SourceConfig("xxx", "源名称", "https://xxx.com/api.php/provide/vod/"),
    )
}
```

> 注意：源必须返回**直接的 m3u8 地址**（不是 `play/xxx` 跳转页）。
> 已知「极速 jisuzy」返回的是跳转页，NewBox/本App 都无法直接播放；
> 「量子 lzizy」返回直接 m3u8，可正常播放。

---

## 技术栈

| 组件 | 用途 |
|------|------|
| Kotlin 2.0 | 主语言 |
| Jetpack Compose | UI |
| Media3 / ExoPlayer 1.4 | HLS 播放 |
| OkHttp 4 | 网络 |
| kotlinx.serialization | JSON |
| Coil | 图片加载 |

## 环境要求

- Android 5.0 (API 21) 及以上
- 手机 / 平板 / 安卓电视 / 电视盒子 通用
