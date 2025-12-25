package com.example.mememaster.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.mememaster.utils.MemeSaver
import java.io.File

/**
 * 作品仓库页面
 * 展示已保存的表情包作品，支持预览、导出、删除等操作
 */
@Composable
fun GalleryScreen() {
    // 上下文对象
    val context = LocalContext.current

    // 已保存的表情包文件列表
    var memeFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // 当前预览的文件（null表示不显示预览弹窗）
    var previewFile by remember { mutableStateOf<File?>(null) }

    /**
     * 加载本地存储的表情包文件
     * 从应用私有目录读取jpg格式文件，并按修改时间倒序排列
     */
    fun loadFiles() {
        val directory = File(context.filesDir, "my_memes")
        if (directory.exists()) {
            memeFiles = directory.listFiles()
                ?.filter { it.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    // 页面初始化时加载文件列表
    LaunchedEffect(Unit) {
        loadFiles()
    }

    // 表情包预览弹窗
    if (previewFile != null) {
        MemePreviewDialog(
            file = previewFile!!,
            onDismiss = { previewFile = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 页面标题
        Text(
            text = "我的作品仓库",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 空状态展示
        if (memeFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "还没有作品哦",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        "快去创作吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            // 作品网格列表
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(memeFiles) { file ->
                    MemeGalleryItem(
                        file = file,
                        onClick = { previewFile = file },
                        onExport = { MemeSaver.exportToSystemGallery(context, file) },
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
 * 表情包预览弹窗组件
 * 展示选中的表情包大图，支持关闭操作
 *
 * @param file 要预览的表情包文件
 * @param onDismiss 弹窗关闭回调
 */
@Composable
fun MemePreviewDialog(
    file: File,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box {
                // 预览大图
                Image(
                    painter = rememberAsyncImagePainter(file),
                    contentDescription = "表情包预览",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )

                // 关闭按钮
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭预览",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 仓库列表项组件
 * 展示单个表情包作品，包含预览图和操作按钮
 *
 * @param file 表情包文件
 * @param onClick 预览点击回调
 * @param onExport 导出到相册回调
 * @param onDelete 删除作品回调
 */
@Composable
fun MemeGalleryItem(
    file: File,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // 作品预览图
            Image(
                painter = rememberAsyncImagePainter(file),
                contentDescription = "作品预览",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray)
                    .clickable { onClick() },
                contentScale = ContentScale.Crop
            )

            // 操作按钮栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除作品",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 导出按钮
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