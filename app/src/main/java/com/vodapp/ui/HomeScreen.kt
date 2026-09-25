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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vodapp.data.Vod
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: VodViewModel, onOpen: (Vod) -> Unit) {
    val home by vm.home.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        // 顶栏
        TopAppBar(
            title = { Text("影视") },
            actions = {
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
}

@Composable
fun CategoryBar(
    categories: List<com.vodapp.data.Category>,
    selected: com.vodapp.data.Category?,
    onSelect: (com.vodapp.data.Category) -> Unit
) {
    val rootCats = categories.filter { it.type_pid == 0 }
    // 顶部加一个"全部"
    val all = com.vodapp.data.Category(0, 0, "全部")
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(all, selected?.type_id == 0 && selected != null) { onSelect(all) }
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
        Box(modifier = Modifier.aspectRatio(0.7f)) {
            AsyncImage(
                model = vod.vod_pic,
                contentDescription = vod.vod_name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
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
