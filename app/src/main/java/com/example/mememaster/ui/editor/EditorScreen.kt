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
import com.example.mememaster.model.ComponentType // 同样需要导入 ComponentType
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons // 为图标导入
import androidx.compose.material.icons.filled.Add // 为"Add"图标导入
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer // 导入用于执行旋转缩放的层
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.mememaster.R
import androidx.compose.foundation.shape.CircleShape // 必须导入圆形形状
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

@OptIn(ExperimentalFoundationApi::class) // <
@Composable
fun EditorScreen() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // 管理屏幕上所有的组件（文字、贴图等）
    val components = remember { mutableStateListOf<MemeComponent>() }

    // 新增：记录当前正在编辑哪个组件
    var editingComponent by remember { mutableStateOf<MemeComponent?>(null) }

    // 👇 新增：专门控制编辑对话框的状态
    var showEditDialog by remember { mutableStateOf(false) }

    // 选图启动器（补全逻辑）
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedImageUri = it
    }

    val density = LocalDensity.current.density

    var showStickerSheet by remember { mutableStateOf(false) }

    // 新增：用于存储编辑区域(Canvas Box)的实际像素大小
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val context = LocalContext.current

    // 1. 获取已下载的贴图列表（通过异步实现）
    var isLoading by remember { mutableStateOf(true) }
//    var downloadedStickers by remember { mutableStateOf(emptyList<Uri>()) }
    var downloadedStickers = remember { mutableStateListOf<Uri>() }
    LaunchedEffect(showStickerSheet) {
        isLoading = true // 开始加载
        try {
            val folder = File(context.filesDir, "downloaded_stickers").apply {
                mkdirs() // 确保文件夹存在
            }
            // 读取文件夹下所有文件并转为Uri列表
            val uris = folder.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()

            // 核心：mutableStateListOf 不能直接赋值，需先清空再添加
            downloadedStickers.clear() // 清空旧数据
            downloadedStickers.addAll(uris) // 添加新数据（触发UI重组）
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载已下载贴纸失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false // 无论成功/失败，结束加载
        }
    }
    // 假设你已经在 drawable 里放了这些图片
    val stickerList = listOf(
        R.drawable.sticker_panda,
        R.drawable.programer,
        R.drawable.hello,
        R.drawable.think,
        R.drawable.obedient
    )

    // 辅助函数：Uri 转 Bitmap
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
            // --- 灵动画布区 ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(224, 224, 224))
                    .onGloballyPositioned { coordinates ->
                        // 核心：获取画布在屏幕上的实际像素大小
                        canvasSize = coordinates.size
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            editingComponent = null
                        })
                    }
            ) {
                // 1. 底图
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // 2. 动态组件层
                components.forEach { component ->
                    // 创建缩放和旋转的状态监听器
                    val state = rememberTransformableState { zoomChange, panChange, rotationChange ->
                        // 更新缩放
                        component.scale *= zoomChange
                        // 更新旋转
                        component.rotation += rotationChange
                        // 同时支持双指移动位置
                        component.offset = Offset(
                            component.offset.x + panChange.x / density,
                            component.offset.y + panChange.y / density
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(component.offset.x.dp, component.offset.y.dp)
                            // 核心：应用旋转和缩放效果
                            .graphicsLayer(
                                scaleX = component.scale,
                                scaleY = component.scale,
                                rotationZ = component.rotation
                            )
                            // 核心：支持双指变换
                            .transformable(state = state)
                            .pointerInput(component.id) {
                                // 👇 改进核心：同时处理 拖拽、单击、双击
                                // 注意：detectDragGestures 会和 detectTapGestures 竞争，
                                // 建议将点击手势放在前面
                                detectTapGestures(
                                    onTap = {
                                        editingComponent = component // 单击选中
                                    },
                                    onDoubleTap = {
                                        if (component.type is ComponentType.Text) {
                                            editingComponent = component // 确保选中的同时弹出弹窗
                                            showEditDialog = true
                                        }
                                    }
                                )
                            }
                            // 单指拖拽逻辑保持不变
                            .pointerInput(component.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    component.offset = Offset(
                                        component.offset.x + dragAmount.x / density,
                                        component.offset.y + dragAmount.y / density
                                    )
                                }
                            }
                            .border(
                                width = if (editingComponent == component) 2.dp else 0.dp,
                                color = if (editingComponent == component) Color(0xFF03DAC5) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
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
                            is ComponentType.RemoteSticker -> { // 新增渲染逻辑
                                AsyncImage(
                                    model = type.uri,
                                    contentDescription = null,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                            else -> {

                            }
                        }

                        // 2. 删除按钮层 (只在被选中时显示)
                        if (editingComponent == component) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-10).dp)
                                    .size(24.dp)
                                    // 使用圆形背景，灰白配色 (例如 0xFFF0F0F0)
                                    .background(Color(0xFFF0F0F0), CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape) // 增加细边框更有质感
                                    .pointerInput(Unit) {
                                        // 👇 关键：删除按钮也要用 detectTapGestures 确保点击灵敏度
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
                                    tint = Color.DarkGray, // 深灰图标
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                    }
                }
            }

            // --- 底部工具栏 ---
            // --- 统一的工具栏排布 ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // 行间距
            ) {
                // 第一行：功能按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // 按钮间距
                ) {
                    // 换图按钮（带圆角的 OutlinedIconButton 风格）
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

                    // 加文字按钮
                    Button(
                        onClick = {
                            val newText = MemeComponent(type = ComponentType.Text("新文字"))
                            components.add(newText)
                            editingComponent = newText
                            // 注意：这里需要配合你之前提到的 showEditDialog = true 逻辑
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)) // 统一深紫色
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("加文字", style = MaterialTheme.typography.labelLarge)
                    }

                    // 加贴图按钮
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

                // 第二行：保存按钮（改为保存到仓库）
                Button(
                    onClick = {
                        selectedImageUri?.let { uri ->
                            try {
                                val baseBitmap = uriToBitmap(uri)
                                // 核心修复：传入画布的实际像素尺寸 (canvasSize) 和 density
                                val resultBitmap = MemeSaver.createBitmap(
                                    context = context,
                                    baseBitmap = baseBitmap,
                                    components = components,
                                    containerWidth = canvasSize.width, // 使用 onGloballyPositioned 获取的值
                                    containerHeight = canvasSize.height,
                                    density = density
                                )

                                val success = MemeSaver.saveToInternalStorage(context, resultBitmap)
                                if (success) {
                                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "保存出错: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
        // 贴图选择面板
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
                                .fillMaxWidth() // 占满 LazyRow 的宽度
                                .height(100.dp) // 匹配 LazyRow 的高度
                                .wrapContentSize(align = Alignment.Center) // 文字自身居中
                        )
                    }
                } else {
                    items(stickerList) { resId ->
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clickable {
                                    // 点击贴图，添加到画布
                                    components.add(MemeComponent(type = ComponentType.Sticker(resId)))
                                    showStickerSheet = false
                                }
                        )
                    }
                    // 新增：下载的贴图
                    items(downloadedStickers) { uri ->
                        // 1. 从Uri解析出本地文件（因为是已下载的贴纸，Uri对应本地文件）
                        val file = File(uri.path ?: "")
                        AsyncImage(
                            model = uri,
                            modifier = Modifier
                                .size(80.dp)
//                                .clickable {
//                                    // 使用我们新定义的 RemoteSticker 类型
//                                    components.add(
//                                        MemeComponent(
//                                            type = ComponentType.RemoteSticker(
//                                                uri
//                                            )
//                                        )
//                                    )
//                                    showStickerSheet = false
                                .combinedClickable(
                                    // 原点击功能：添加到components
                                    onClick = {
                                        components.add(
                                            MemeComponent(
                                                type = ComponentType.RemoteSticker(uri)
                                            )
                                        )
                                        showStickerSheet = false
                                    },
                                    // 新增长按功能：删除资源
                                    onLongClick = {
                                        // 3. 确认文件存在后删除
                                        if (file.exists()) {
                                            val isDeleted = file.delete()
                                            if (isDeleted) {
                                                // 4. 删除成功后，从列表中移除该Uri（UI自动刷新）
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
        // --- 编辑弹窗控制 ---
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

@Composable
fun TextEditDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp), // 高质感大圆角
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
