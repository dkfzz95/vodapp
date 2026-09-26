package com.vodapp.data

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 苹果CMS v10 采集接口客户端 */
class ApiClient(private val apiBase: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "okhttp/3.12.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(req)
        }
        .build()

    private fun get(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("网络错误 HTTP ${resp.code}")
            return resp.body?.string() ?: throw Exception("空响应")
        }
    }

    /** 安全 JSON 解析：非 JSON 内容返回友好错误 */
    private inline fun <reified T> safeDecode(raw: String, what: String): T {
        val trim = raw.trim()
        if (trim.isEmpty()) throw Exception("$what：返回为空")
        if (!trim.startsWith("{") && !trim.startsWith("[")) {
            // 返回的是纯文本（如「暂不支持搜索」），直接暴露
            throw Exception(trim.take(60))
        }
        return try {
            json.decodeFromString<T>(trim)
        } catch (e: Exception) {
            throw Exception("$what：数据解析失败")
        }
    }

    /** 首页列表（ac=videolist 返回带海报的完整字段） */
    suspend fun home(page: Int = 1): ListResponse {
        val url = "$apiBase?ac=videolist&pg=$page"
        return safeDecode(get(url), "加载首页")
    }

    /** 分类树（ac=list&t=1 返回 class 字段） */
    suspend fun categories(): List<Category> {
        val url = "$apiBase?ac=list&t=1"
        val resp = safeDecode<ListResponse>(get(url), "加载分类")
        return resp.`class`
    }

    /** 分类列表（用 videolist 带海报） */
    suspend fun category(typeId: Int, page: Int = 1): ListResponse {
        val url = "$apiBase?ac=videolist&t=$typeId&pg=$page"
        return safeDecode(get(url), "加载分类")
    }

    /** 搜索（用 videolist 带海报） */
    suspend fun search(keyword: String, page: Int = 1): ListResponse {
        val url = "$apiBase?ac=videolist&wd=$keyword&pg=$page"
        return safeDecode(get(url), "搜索")
    }

    /** 详情 */
    suspend fun detail(vodId: Long): VodDetail? {
        val url = "$apiBase?ac=detail&ids=$vodId"
        val resp = safeDecode<DetailResponse>(get(url), "加载详情")
        return resp.list.firstOrNull()
    }
}

/** 将「vod_play_from / vod_play_url」解析成线路+选集 */
fun parsePlayLines(playFrom: String, playUrl: String): List<PlayLine> {
    if (playUrl.isBlank()) return emptyList()

    // 选集组用 $$$ 分隔多线路，每线内 # 分隔各集（name$url）
    val urlBlocks = playUrl.split("$$$")

    val result = mutableListOf<PlayLine>()
    for ((i, block) in urlBlocks.withIndex()) {
        if (block.isBlank()) continue
        val episodes = mutableListOf<Episode>()
        // 选集用 # 分隔，每集 name$url
        for (ep in block.split("#")) {
            if (ep.isBlank()) continue
            val idx = ep.indexOf('$')
            if (idx > 0) {
                val name = ep.substring(0, idx)
                val url = ep.substring(idx + 1)
                episodes.add(Episode(name, url))
            }
        }
        if (episodes.isNotEmpty()) {
            // 线路名改用友好序号，丢弃源站内部代号（如 hhyun/snm3u8 等）
            val lineName = if (urlBlocks.size > 1) "线路${i + 1}" else "高清"
            result.add(PlayLine(lineName, episodes))
        }
    }
    return result
}

/** 清理 vod_content 里的 HTML 标签 */
fun cleanHtml(s: String): String =
    s.replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .trim()
