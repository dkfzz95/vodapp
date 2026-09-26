package com.vodapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vodapp.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VodTheme {
                AppRoot()
            }
        }
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    background = Color(0xFF1A2A3A),
    surface = Color(0xFF243447),
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF2C3A4A),
    onSurfaceVariant = Color(0xFFB0C0D0),
)

@Composable
fun VodTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
