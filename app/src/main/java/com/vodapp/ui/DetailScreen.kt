package com.vodapp.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vodapp.data.Episode
import com.vodapp.data.PlayLine
import com.vodapp.data.Vod
import com.vodapp.data.cleanHtml
import com.vodapp.player.PlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: VodViewModel, vod: Vod, onBack: () -> Unit) {
    val detail by vm.detail.collectAsState()
    var selectedLine by remember { mutableStateOf<PlayLine?>(null) }

    LaunchedEffect(vod.vod_id) {
        vm.loadDetail(vod.vod_id)
    }

    // 自动选中第一条线路
    LaunchedEffect(detail.playLines) {
        if (detail.playLines.isNotEmpty() && selectedLine == null) {
            selectedLine = detail.playLines.first()
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(vod.vod_name, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        when {
            detail.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            detail.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(detail.error ?: "加载失败", color = Color(0xFF888888))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.loadDetail(vod.vod_id) }) { Text("重试") }
                }
            }
            else -> {
                val d = detail.detail
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 海报 + 信息
                    Row(Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = d?.vod_pic ?: vod.vod_pic,
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(0.7f),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                d?.vod_name ?: vod.vod_name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            val meta = listOfNotNull(
                                d?.type_name,
                                d?.vod_year,
                                d?.vod_area,
                                d?.vod_remarks
                            ).filter { it.isNotBlank() }.joinToString(" · ")
                            if (meta.isNotBlank()) {
                                Text(meta, style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA))
                            }
                            if (!d?.vod_actor.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("主演：${d.vod_actor}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA), maxLines = 2)
                            }
                            if (!d?.vod_director.isNullOrBlank()) {
                                Text("导演：${d.vod_director}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA), maxLines = 1)
                            }
                        }
                    }

                    // 简介
                    val content = cleanHtml(d?.vod_content ?: "")
                    if (content.isNotBlank()) {
                        Text(
                            content,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBBBBBB),
                        )
                    }

                    // 线路选择
                    if (detail.playLines.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Text("线路", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detail.playLines) { line ->
                                val isSel = line == selectedLine
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable { selectedLine = line }
                                ) {
                                    Text(
                                        line.name,
                                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // 选集
                    Spacer(Modifier.height(8.dp))
                    Text("选集", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                    EpisodeGrid(selectedLine?.episodes ?: emptyList()) { ep ->
                        // 跳转播放器
                        val ctx = LocalContext.current
                        val intent = Intent(ctx, PlayerActivity::class.java).apply {
                            putExtra(PlayerActivity.EXTRA_URL, ep.url)
                            putExtra(PlayerActivity.EXTRA_TITLE, "${d?.vod_name ?: vod.vod_name} ${ep.name}")
                        }
                        ctx.startActivity(intent)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun EpisodeGrid(episodes: List<Episode>, onPlay: (Episode) -> Unit) {
    if (episodes.isEmpty()) {
        Text("暂无选集", Modifier.padding(12.dp), color = Color(0xFF888888))
        return
    }
    // 用 FlowRow 效果：简单分行网格
    Column(Modifier.padding(horizontal = 12.dp)) {
        val chunked = episodes.chunked(8)
        for (row in chunked) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (ep in row) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPlay(ep) }
                    ) {
                        Box(
                            Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ep.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                // 补齐空位让每行对齐
                repeat(8 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
