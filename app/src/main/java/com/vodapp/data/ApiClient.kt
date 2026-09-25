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
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            return resp.body?.string() ?: throw Exception("空响应")
        }
    }

    /** 首页列表（含分类树 class 字段） */
    suspend fun home(page: Int = 1): ListResponse {
        val url = "$apiBase?ac=list&pg=$page"
        return json.decodeFromString<ListResponse>(get(url))
    }

    /** 分类列表 */
    suspend fun category(typeId: Int, page: Int = 1): ListResponse {
        val url = "$apiBase?ac=list&t=$typeId&pg=$page"
        return json.decodeFromString<ListResponse>(get(url))
    }

    /** 搜索 */
    suspend fun search(keyword: String, page: Int = 1): ListResponse {
        val url = "$apiBase?ac=list&wd=$keyword&pg=$page"
        return json.decodeFromString<ListResponse>(get(url))
    }

    /** 详情 */
    suspend fun detail(vodId: Long): VodDetail? {
        val url = "$apiBase?ac=detail&ids=$vodId"
        val resp = json.decodeFromString<DetailResponse>(get(url))
        return resp.list.firstOrNull()
    }
}

/** 将「vod_play_from / vod_play_url」解析成线路+选集 */
fun parsePlayLines(playFrom: String, playUrl: String): List<PlayLine> {
    if (playUrl.isBlank()) return emptyList()

    // 线路名用 $$$ 分隔，选集组用 # 分隔（可能多线路，每线一组）
    // 苹果CMS：play_from 用 $$$ 分隔线路；play_url 里同名线路对应，用 $$$ 分隔多线路
    val fromNames = playFrom.split("$$$").filter { it.isNotBlank() }
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
            val lineName = fromNames.getOrNull(i) ?: "线路${i + 1}"
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
