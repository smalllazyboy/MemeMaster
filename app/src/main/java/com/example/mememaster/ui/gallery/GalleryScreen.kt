package com.example.mememaster.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // [新增] 用于点击事件
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close // [新增] 关闭图标
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog // [新增] 用于预览弹窗
import coil.compose.rememberAsyncImagePainter
import com.example.mememaster.utils.MemeSaver
import java.io.File

@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    var memeFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // [新增] 状态：当前正在预览的文件，如果为 null 则不显示预览
    var previewFile by remember { mutableStateOf<File?>(null) }

    fun loadFiles() {
        val directory = File(context.filesDir, "my_memes")
        if (directory.exists()) {
            memeFiles = directory.listFiles()
                ?.filter { it.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        loadFiles()
    }

    // [新增] 预览弹窗逻辑
    if (previewFile != null) {
        MemePreviewDialog(
            file = previewFile!!,
            onDismiss = { previewFile = null } // 关闭弹窗时清空状态
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "我的作品仓库",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (memeFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有作品哦", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Text("快去创作吧！", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(memeFiles) { file ->
                    MemeGalleryItem(
                        file = file,
                        // [新增] 点击图片时的回调
                        onClick = {
                            previewFile = file
                        },
                        onExport = {
                            MemeSaver.exportToSystemGallery(context, file)
                        },
                        onDelete = {
                            file.delete()
                            loadFiles()
                        }
                    )
                }
            }
        }
    }
}

/**
 * [新增] 专门用于预览大图的弹窗组件
 */
@Composable
fun MemePreviewDialog(file: File, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        // 使用 Card 做背景，圆角稍微大一点
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box {
                // 大图显示
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = "预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(), // 高度自适应
                    contentScale = ContentScale.FillWidth
                )

                // 右上角关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MemeGalleryItem(
    file: File,
    onClick: () -> Unit, // [新增] 接收点击参数
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // 1. 作品预览图
            Image(
                painter = rememberAsyncImagePainter(file),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray)
                    .clickable { onClick() }, // [新增] 点击触发预览
                contentScale = ContentScale.Crop
            )

            // 2. 操作栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onExport,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                ) {
                    Text("导出", fontSize = 12.sp, color = Color.Black)
                }
            }
        }
    }
}