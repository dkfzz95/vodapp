package com.vodapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vodapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    /** 手动切换数据源 */
    fun switchSource(index: Int) {
        val src = AppConfig.sources.getOrNull(index) ?: return
        client = ApiClient(src.api)
        _activeSource.value = src
        loadHome(1)
    }

    fun loadHome(page: Int = 1) {
        viewModelScope.launch {
            _home.value = _home.value.copy(loading = true, error = null)
            try {
                val resp = withContext(Dispatchers.IO) {
                    // 简单重试：失败或空则重试
                    var result: ListResponse? = null
                    var lastErr: Exception? = null
                    for (i in 0 until 3) {
                        try {
                            val r = client.home(page)
                            if (r.list.isNotEmpty() || r.`class`.isNotEmpty()) {
                                result = r
                                break
                            }
                        } catch (e: Exception) {
                            lastErr = e
                        }
                        if (i < 2) delay(500)
                    }
                    result ?: throw (lastErr ?: Exception("加载失败"))
                }
                _home.value = HomeState(
                    loading = false,
                    categories = resp.`class`,
                    vods = resp.list,
                    page = page,
                    pageCount = resp.pagecount,
                )
            } catch (e: Exception) {
                _home.value = _home.value.copy(loading = false, error = e.message ?: "加载失败，请点重试")
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
                    var result: ListResponse? = null
                    var lastErr: Exception? = null
                    for (i in 0 until 3) {
                        try {
                            val r = if (category.type_id == 0) client.home(page)
                            else client.category(category.type_id, page)
                            if (r.list.isNotEmpty() || r.`class`.isNotEmpty()) {
                                result = r
                                break
                            }
                        } catch (e: Exception) { lastErr = e }
                        if (i < 2) delay(500)
                    }
                    result ?: throw (lastErr ?: Exception("加载失败"))
                }
                _home.value = _home.value.copy(
                    loading = false,
                    vods = resp.list,
                    page = page,
                    pageCount = resp.pagecount,
                )
            } catch (e: Exception) {
                _home.value = _home.value.copy(loading = false, error = e.message ?: "加载失败，请点重试")
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
