package com.vodapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vodapp.data.AppConfig
import com.vodapp.data.Vod
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: VodViewModel, onOpen: (Vod) -> Unit) {
    val home by vm.home.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    var showSourceDialog by remember { mutableStateOf(false) }

    val src = vm.activeSource.collectAsState().value

    Column(Modifier.fillMaxSize()) {
        // 顶栏（显示当前源，点击切换）
        TopAppBar(
            title = { Text("影视") },
            actions = {
                TextButton(onClick = { showSourceDialog = true }) {
                    Text(src.name, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showSearch = !showSearch }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
            }
        )

        if (showSearch) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                placeholder = { Text("输入片名") },
                singleLine = true,
                trailingIcon = {
                    TextButton(onClick = {
                        vm.search(keyword.trim())
                        showSearch = false
                    }) { Text("搜索") }
                }
            )
        }

        // 分类导航
        CategoryBar(home.categories, home.selectedCategory) { cat ->
            vm.loadCategory(cat)
        }

        // 内容区
        when {
            home.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            home.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(home.error ?: "加载失败", color = Color(0xFF888888))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.loadHome() }) { Text("重试") }
                }
            }
            else -> {
                VodGrid(home.vods, onOpen)
            }
        }
    }

    // 切换源对话框
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("切换数据源") },
            text = {
                Column {
                    AppConfig.sources.forEachIndexed { i, s ->
                        val isActive = s.key == src.key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showSourceDialog = false
                                    vm.switchSource(i)
                                }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    s.name + (if (isActive) " ✓" else ""),
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (s.desc.isNotBlank()) {
                                    Text(
                                        s.desc,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else Color(0xFF888888),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
fun CategoryBar(
    categories: List<com.vodapp.data.Category>,
    selected: com.vodapp.data.Category?,
    onSelect: (com.vodapp.data.Category) -> Unit
) {
    // 有层级时只显示根级；扁平结构（type_pid 全为 -1）时显示全部
    val hasHierarchy = categories.any { it.type_pid == 0 }
    val rootCats = if (hasHierarchy) {
        categories.filter { it.type_pid == 0 }
    } else {
        categories
    }
    // 顶部加一个"全部"
    val all = com.vodapp.data.Category(0, 0, "全部")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(all, selected == null, { onSelect(all) })
        }
        items(rootCats) { cat ->
            val isSel = selected?.type_id == cat.type_id
            CategoryChip(cat, isSel) { onSelect(cat) }
        }
    }
}

@Composable
fun CategoryChip(cat: com.vodapp.data.Category, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            cat.type_name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VodGrid(vods: List<Vod>, onOpen: (Vod) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vods) { vod ->
            VodCard(vod, onOpen)
        }
    }
}

@Composable
fun VodCard(vod: Vod, onOpen: (Vod) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(vod) }
    ) {
        // 有海报显示海报，无海报用彩色占位块 + 片名
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(color(pod = vod))
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (vod.vod_pic.isNotBlank()) {
                AsyncImage(
                    model = vod.vod_pic,
                    contentDescription = vod.vod_name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // 无海报：占位块中央显示片名
                Text(
                    vod.vod_name,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            if (vod.vod_remarks.isNotBlank()) {
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(topStart = 4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        vod.vod_remarks,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            vod.vod_name,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 根据名称生成稳定的占位色
fun color(pod: Vod): Color {
    val palette = listOf(
        Color(0xFF424242), Color(0xFF37474F), Color(0xFF4E342E),
        Color(0xFF1B5E20), Color(0xFF283593), Color(0xFF6A1B9A),
        Color(0xFF00695C), Color(0xFF9E9D24), Color(0xFFBF360C),
    )
    return palette[(pod.vod_name.hashCode() and 0x7fffffff) % palette.size]
}
