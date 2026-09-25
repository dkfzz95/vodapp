package com.vodapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    object Loading : UiState
    data class Error(val message: String) : UiState
}

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Category> = emptyList(),
    val vods: List<Vod> = emptyList(),
    val page: Int = 1,
    val pageCount: Int = 1,
    val selectedCategory: Category? = null,
)

data class DetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: VodDetail? = null,
    val playLines: List<PlayLine> = emptyList(),
)

class VodViewModel : ViewModel() {

    // 动态持有可用的 ApiClient（多源自动切换）
    @Volatile
    private var client: ApiClient = ApiClient(AppConfig.sources.first().api)

    private val _home = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = _home

    private val _detail = MutableStateFlow(DetailState())
    val detail: StateFlow<DetailState> = _detail

    private val _activeSource = MutableStateFlow(AppConfig.sources.first())
    val activeSource: StateFlow<SourceConfig> = _activeSource

    init {
        loadHome()
    }

    /** 探测可用源，返回第一个能连通的源 */
    private suspend fun probeAvailableSource(): SourceConfig? = withContext(Dispatchers.IO) {
        for (s in AppConfig.sources) {
            try {
                val c = ApiClient(s.api)
                c.home(1)
                return@withContext s
            } catch (e: Exception) {
                // 继续尝试下一个源
            }
        }
        null
    }

    fun loadHome(page: Int = 1) {
        viewModelScope.launch {
            _home.value = _home.value.copy(loading = true, error = null)
            try {
                // 主动探测可用源
                val src = probeAvailableSource()
                if (src != null) {
                    client = ApiClient(src.api)
                    _activeSource.value = src
                }
                val resp = withContext(Dispatchers.IO) { client.home(page) }
                _home.value = HomeState(
                    loading = false,
                    categories = resp.`class`,
                    vods = resp.list,
                    page = page,
                    pageCount = resp.pagecount,
                )
            } catch (e: Exception) {
                _home.value = _home.value.copy(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun loadCategory(category: Category, page: Int = 1) {
        viewModelScope.launch {
            _home.value = _home.value.copy(
                loading = true, error = null, selectedCategory = category
            )
            try {
                val resp = withContext(Dispatchers.IO) {
                    if (category.type_id == 0) client.home(page)
                    else client.category(category.type_id, page)
                }
                _home.value = _home.value.copy(
                    loading = false,
                    vods = resp.list,
                    page = page,
                    pageCount = resp.pagecount,
                )
            } catch (e: Exception) {
                _home.value = _home.value.copy(loading = false, error = e.message ?: "加载失败")
            }
        }
    }

    fun search(keyword: String, page: Int = 1) {
        viewModelScope.launch {
            _home.value = _home.value.copy(loading = true, error = null)
            try {
                val resp = withContext(Dispatchers.IO) { client.search(keyword, page) }
                _home.value = _home.value.copy(
                    loading = false,
                    vods = resp.list,
                    page = page,
                    pageCount = resp.pagecount,
                )
            } catch (e: Exception) {
                _home.value = _home.value.copy(loading = false, error = e.message ?: "搜索失败")
            }
        }
    }

    fun loadDetail(vodId: Long) {
        viewModelScope.launch {
            _detail.value = DetailState(loading = true)
            try {
                val d = withContext(Dispatchers.IO) { client.detail(vodId) }
                if (d == null) {
                    _detail.value = DetailState(loading = false, error = "未找到详情")
                } else {
                    val lines = parsePlayLines(d.vod_play_from, d.vod_play_url)
                    _detail.value = DetailState(loading = false, detail = d, playLines = lines)
                }
            } catch (e: Exception) {
                _detail.value = DetailState(loading = false, error = e.message ?: "加载失败")
            }
        }
    }
}
