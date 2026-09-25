package com.vodapp.data

import kotlinx.serialization.Serializable

/** 分类节点（class 字段） */
@Serializable
data class Category(
    val type_id: Int = 0,
    val type_pid: Int = 0,
    val type_name: String = ""
)

/** 列表/搜索返回的单条影视 */
@Serializable
data class Vod(
    val vod_id: Long = 0,
    val vod_name: String = "",
    val type_id: Int = 0,
    val type_name: String = "",
    val vod_pic: String = "",
    val vod_remarks: String = "",
    val vod_year: String = "",
    val vod_area: String = "",
    val vod_score: String = "",
)

/** 列表响应（首页/分类/搜索共用） */
@Serializable
data class ListResponse(
    val code: Int = 0,
    val msg: String = "",
    val page: Int = 1,
    val pagecount: Int = 0,
    val total: Int = 0,
    val list: List<Vod> = emptyList(),
    val `class`: List<Category> = emptyList(),
)

/** 详情 */
@Serializable
data class DetailResponse(
    val code: Int = 0,
    val list: List<VodDetail> = emptyList(),
)

@Serializable
data class VodDetail(
    val vod_id: Long = 0,
    val vod_name: String = "",
    val vod_pic: String = "",
    val vod_actor: String = "",
    val vod_director: String = "",
    val vod_content: String = "",
    val type_name: String = "",
    val vod_year: String = "",
    val vod_area: String = "",
    val vod_lang: String = "",
    val vod_remarks: String = "",
    val vod_play_from: String = "",
    val vod_play_url: String = "",
)

/** 解析后的「线路 + 选集」结构 */
data class PlayLine(
    val name: String,
    val episodes: List<Episode>,
)

data class Episode(
    val name: String,
    val url: String,
)

/** 可配置的数据源 */
data class SourceConfig(
    val key: String,
    val name: String,
    val api: String,
)

object AppConfig {
    // 主数据源（量子，m3u8 直链，TVBox 友好）
    val sources = listOf(
        SourceConfig("lzizy", "量子资源", "https://lzizy1.com/api.php/provide/vod/"),
        // 可扩展更多源
    )
}
