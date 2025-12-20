package com.example.mememaster.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun GalleryScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF9C4)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "📂 我的作品仓库")
    }
}