package com.example.mememaster.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 定义组件类型
sealed class ComponentType {
    data class Text(val content: String, val color: Color = Color.White) : ComponentType()
    data class Sticker(val resId: Int) : ComponentType()
    // 新增：支持本地或网络 Uri 的贴图
    data class RemoteSticker(val uri: android.net.Uri) : ComponentType()
    data class Drawing(val path: Path, val color: Color = Color.Red) : ComponentType()
}

// 每一个在屏幕上的物体
class MemeComponent(
    val id: Long = System.currentTimeMillis(),
    type: ComponentType,
    offset: Offset = Offset(100f, 100f)
) {
    // 使用 mutableStateOf 包装，确保 Compose 能监测到属性变化并触发重绘
    var type by mutableStateOf(type)
    var offset by mutableStateOf(offset)
    var scale by mutableStateOf(1f)     // 新增：缩放倍数，默认 1.0
    var rotation by mutableStateOf(0f)  // 新增：旋转角度，默认 0 度
}