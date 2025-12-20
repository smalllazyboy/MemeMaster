package com.example.mememaster.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun EditorScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🎨 在这里制作表情")
    }
}