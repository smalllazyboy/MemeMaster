package com.example.mememaster.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * 表情包组件类型密封类
 * 定义所有可添加到表情包的组件类型（文字、本地贴纸、远程/本地Uri贴纸）
 */
sealed class ComponentType {
    /**
     * 文字组件
     * @param content 文字内容
     * @param color 文字颜色，默认白色
     */
    data class Text(val content: String, val color: Color = Color.White) : ComponentType()

    /**
     * 本地资源贴纸组件
     * @param resId 贴纸资源ID
     */
    data class Sticker(val resId: Int) : ComponentType()

    /**
     * 远程/本地Uri贴纸组件
     * 支持本地文件或网络图片Uri
     * @param uri 贴纸图片Uri
     */
    data class RemoteSticker(val uri: android.net.Uri) : ComponentType()
}

/**
 * 表情包可交互组件实体类
 * 封装单个组件的所有属性和状态，支持Compose状态监听
 *
 * @param id 组件唯一标识，默认使用时间戳生成
 * @param type 组件类型（文字/贴纸/远程贴纸）
 * @param offset 组件初始偏移位置，默认(100f, 100f)
 */
class MemeComponent(
    val id: Long = System.currentTimeMillis(),
    type: ComponentType,
    offset: Offset = Offset(100f, 100f)
) {
    // 组件类型（支持状态监听）
    var type by mutableStateOf(type)

    // 组件在画布中的偏移位置（支持状态监听）
    var offset by mutableStateOf(offset)

    // 组件缩放倍数（支持状态监听），默认1.0
    var scale by mutableStateOf(1f)

    // 组件旋转角度（支持状态监听），默认0度
    var rotation by mutableStateOf(0f)
}