package com.example.mememaster.ui.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(false) }

    // 本地已下载文件名列表，用于查重
    var downloadedFiles by remember { mutableStateOf(getDownloadedFileNames(context)) }

    // 初始加载：显示热门表情（搜索关键词默认为“表情包”）
    LaunchedEffect(Unit) {
        isLoading = true
        searchResults.addAll(fetchFromSogou("表情包"))
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        // 1. 搜索框
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("搜索搜狗表情包...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                scope.launch {
                    isLoading = true
                    searchResults.clear()
                    val query = if (searchQuery.isEmpty()) "表情包" else searchQuery
                    searchResults.addAll(fetchFromSogou(query))
                    isLoading = false
                }
            }),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        // 2. 网格列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(searchResults) { url ->
                val fileName = generateFileName(url)
                val isDownloaded = downloadedFiles.contains(fileName)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .combinedClickable(
                            onClick = {
                                if (isDownloaded) {
                                    Toast.makeText(context, "已在库中", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        downloadSticker(context, url) {
                                            downloadedFiles = getDownloadedFileNames(context)
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                if (isDownloaded) {
                                    deleteSticker(context, fileName) {
                                        downloadedFiles = getDownloadedFileNames(context)
                                        Toast.makeText(context, "已成功移除", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 已下载状态可视化
                    if (isDownloaded) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 核心：模拟搜狗图片搜索接口 (API原理同教程)
 */
suspend fun fetchFromSogou(keyword: String): List<String> = withContext(Dispatchers.IO) {
    val list = mutableListOf<String>()
    try {
        val encodedKey = URLEncoder.encode(keyword, "UTF-8")
        // 搜狗图片搜索 API 节点
        val url = "https://cn.apihz.cn/api/img/apihzbqbsougou.php?id=88888888&key=88888888&page=1&words=伤心"
        val response = URL(url).readText()
        val json = JSONObject(response)

        // 关键适配：res 字段直接是字符串数组
        if (json.has("res")) {
            val resArray = json.getJSONArray("res")
            for (i in 0 until resArray.length()) {
                list.add(resArray.getString(i)) // 直接获取字符串
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    list
}

// 文件名管理：根据URL生成唯一ID
fun generateFileName(url: String): String {
    val bytes = MessageDigest.getInstance("MD5").digest(url.toByteArray())
    return "sogou_${bytes.joinToString("") { "%02x".format(it) }}.png"
}

fun getDownloadedFileNames(context: Context): Set<String> {
    val folder = File(context.filesDir, "downloaded_stickers")
    return folder.listFiles()?.map { it.name }?.toSet() ?: emptySet()
}

suspend fun downloadSticker(context: Context, url: String, onDone: () -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val folder = File(context.filesDir, "downloaded_stickers")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, generateFileName(url))

            URL(url).openStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            withContext(Dispatchers.Main) {
                onDone()
                Toast.makeText(context, "下载成功，请去编辑器查看", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show() }
        }
    }
}

fun deleteSticker(context: Context, fileName: String, onDone: () -> Unit) {
    val file = File(File(context.filesDir, "downloaded_stickers"), fileName)
    if (file.exists() && file.delete()) onDone()
}