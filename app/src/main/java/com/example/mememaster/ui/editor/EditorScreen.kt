package com.example.mememaster.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.mememaster.model.MemeComponent
import com.example.mememaster.model.ComponentType
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.mememaster.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import com.example.mememaster.utils.MemeSaver
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * 表情包编辑页面
 * 提供图片选择、文字/贴纸添加、编辑（缩放/旋转/拖拽）、保存等核心功能
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen() {
    // 上下文与基础配置相关
    val context = LocalContext.current                  // 上下文对象
    val density = LocalDensity.current.density          // 屏幕密度（用于DP/PX转换）

    // 核心编辑内容状态
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 选中的底图Uri
    val components = remember { mutableStateListOf<MemeComponent>() } // 管理屏幕上所有的组件（文字、贴图等）

    // 编辑交互状态
    var editingComponent by remember { mutableStateOf<MemeComponent?>(null) } // 当前正在编辑的组件
    var showEditDialog by remember { mutableStateOf(false) }                 // 文字编辑对话框显示状态
    var showStickerSheet by remember { mutableStateOf(false) }               // 贴纸选择面板显示状态
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }               // 编辑区域(Canvas Box)的实际像素大小

    // 功能组件
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { // 图片选择启动器
        selectedImageUri = it
    }

    // 贴纸加载相关状态
    var isLoading by remember { mutableStateOf(true) }               // 加载已下载贴纸相关状态
    var downloadedStickers = remember { mutableStateListOf<Uri>() }  // 已下载贴纸列表

    // 加载已下载贴纸列表
    LaunchedEffect(showStickerSheet) {
        isLoading = true
        try {
            val folder = File(context.filesDir, "downloaded_stickers").apply {
                mkdirs() // 确保文件夹存在
            }
            // 读取文件夹下所有文件并转为Uri列表
            val uris = folder.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()

            // 清空旧数据并添加新数据（触发UI重组）
            downloadedStickers.clear()
            downloadedStickers.addAll(uris)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载已下载贴纸失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false // 结束加载状态
        }
    }

    // 本地内置贴纸资源列表
    val stickerList = listOf(
        R.drawable.sticker_panda,
        R.drawable.programer,
        R.drawable.hello,
        R.drawable.think,
        R.drawable.obedient
    )

    /**
     * 将Uri转换为Bitmap对象
     * 兼容Android P以下版本的API差异
     * @param uri 图片Uri
     * @return 转换后的Bitmap
     */
    fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            // 编辑画布区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(224, 224, 224))
                    .onGloballyPositioned { coordinates ->
                        // 获取画布在屏幕上的实际像素大小
                        canvasSize = coordinates.size
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            editingComponent = null
                        })
                    }
            ) {
                // 底图展示
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // 渲染所有表情包组件
                components.forEach { component ->
                    // 组件缩放/旋转状态监听
                    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
                        // 更新组件缩放比例
                        component.scale *= zoomChange
                        // 更新组件旋转角度
                        component.rotation += rotationChange
                        // 更新组件位置（兼容双指移动）
                        component.offset = Offset(
                            component.offset.x + panChange.x / density,
                            component.offset.y + panChange.y / density
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(component.offset.x.dp, component.offset.y.dp)
                            // 应用缩放和旋转效果
                            .graphicsLayer(
                                scaleX = component.scale,
                                scaleY = component.scale,
                                rotationZ = component.rotation
                            )
                            // 绑定双指变换手势
                            .transformable(state = state)
                            .pointerInput(component.id) {
                                // 处理点击/双击手势
                                detectTapGestures(
                                    onTap = {
                                        editingComponent = component // 单击选中组件
                                    },
                                    onDoubleTap = {
                                        if (component.type is ComponentType.Text) {
                                            editingComponent = component
                                            showEditDialog = true // 双击文字组件打开编辑弹窗
                                        }
                                    }
                                )
                            }
                            // 处理单指拖拽手势
                            .pointerInput(component.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    component.offset = Offset(
                                        component.offset.x + dragAmount.x / density,
                                        component.offset.y + dragAmount.y / density
                                    )
                                }
                            }
                            // 选中状态边框
                            .border(
                                width = if (editingComponent == component) 2.dp else 0.dp,
                                color = if (editingComponent == component) Color(0xFF03DAC5) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        // 根据组件类型渲染UI
                        when (val type = component.type) {
                            is ComponentType.Text -> {
                                Text(
                                    text = type.content,
                                    color = type.color,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            is ComponentType.Sticker -> {
                                Image(
                                    painter = painterResource(id = type.resId),
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp) // 初始大小
                                )
                            }
                            is ComponentType.RemoteSticker -> {
                                AsyncImage(
                                    model = type.uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }

                        // 组件删除按钮（仅选中时显示）
                        if (editingComponent == component) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-10).dp)
                                    .size(24.dp)
                                    .background(Color(0xFFF0F0F0), CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            components.remove(component)
                                            editingComponent = null
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                    }
                }
            }

            // 底部操作工具栏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 功能按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 选择图片按钮
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF5F5F5),
                        onClick = { launcher.launch("image/*") }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "换图", tint = Color.Black)
                        }
                    }

                    // 添加文字按钮
                    Button(
                        onClick = {
                            val newText = MemeComponent(type = ComponentType.Text("新文字"))
                            components.add(newText)
                            editingComponent = newText
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("加文字", style = MaterialTheme.typography.labelLarge)
                    }

                    // 添加贴纸按钮
                    Button(
                        onClick = { showStickerSheet = !showStickerSheet },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(painterResource(id = R.drawable.sticker_panda), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("加贴图", style = MaterialTheme.typography.labelLarge)
                    }
                }

                // 保存到仓库按钮
                Button(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            try {
                                val baseBitmap = uriToBitmap(uri)
                                // 合成最终表情包位图
                                val resultBitmap = MemeSaver.createBitmap(
                                    context = context,
                                    baseBitmap = baseBitmap,
                                    components = components,
                                    containerWidth = canvasSize.width,
                                    containerHeight = canvasSize.height,
                                    density = density
                                )

                                MemeSaver.saveToInternalStorage(context, resultBitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } ?: Toast.makeText(context, "请先选择一张图片", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                ) {
                    Text("保存到仓库", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
            }
        }

        // 贴纸选择面板
        if (showStickerSheet) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item{
                        Text(
                            "加载中...",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .wrapContentSize(align = Alignment.Center)
                        )
                    }
                } else {
                    // 渲染本地内置贴纸
                    items(stickerList) { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clickable {
                                    components.add(MemeComponent(type = ComponentType.Sticker(resId)))
                                    showStickerSheet = false
                                }
                        )
                    }
                    // 渲染已下载贴纸
                    items(downloadedStickers) { uri ->
                        val file = File(uri.path ?: "")
                        AsyncImage(
                            model = uri,
                            modifier = Modifier
                                .size(80.dp)
                                .combinedClickable(
                                    // 单击添加贴纸
                                    onClick = {
                                        components.add(
                                            MemeComponent(
                                                type = ComponentType.RemoteSticker(uri)
                                            )
                                        )
                                        showStickerSheet = false
                                    },
                                    // 长按删除贴纸
                                    onLongClick = {
                                        if (file.exists()) {
                                            val isDeleted = file.delete()
                                            if (isDeleted) {
                                                downloadedStickers.remove(uri)
                                                Toast.makeText(
                                                    context,
                                                    "已删除该贴纸",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "删除失败",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                ),
                            contentDescription = null
                        )
                    }
                }
            }
        }

        // 文字编辑弹窗
        if (showEditDialog) {
            editingComponent?.let { component ->
                val currentType = component.type
                if (currentType is ComponentType.Text) {
                    TextEditDialog(
                        initialText = currentType.content,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { newContent ->
                            component.type = currentType.copy(content = newContent)
                            showEditDialog = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 文字编辑弹窗
 * @param initialText 初始文字内容
 * @param onDismiss 弹窗关闭回调
 * @param onConfirm 文字确认修改回调
 */
@Composable
fun TextEditDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("编辑文字内容", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("输入文字...") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(text) }) { Text("确定") }
                }
            }
        }
    }
}