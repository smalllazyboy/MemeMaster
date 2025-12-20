package com.example.mememaster.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8)), // 柔和的背景色
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🏠 发现热门表情包")
    }
}