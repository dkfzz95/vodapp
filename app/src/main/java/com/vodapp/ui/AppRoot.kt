package com.vodapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vodapp.data.Vod

sealed class Screen {
    object Home : Screen()
    data class Detail(val vod: Vod) : Screen()
}

@Composable
fun AppRoot() {
    val vm: VodViewModel = viewModel()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Box(Modifier.fillMaxSize()) {
        when (val s = screen) {
            is Screen.Home -> {
                HomeScreen(vm) { vod ->
                    screen = Screen.Detail(vod)
                }
            }
            is Screen.Detail -> {
                DetailScreen(vm, s.vod) {
                    screen = Screen.Home
                }
            }
        }
    }
}
