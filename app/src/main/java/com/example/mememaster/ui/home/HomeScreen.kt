package com.example.mememaster.ui.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import com.example.mememaster.R

/**
 * 贴纸缓存管理单例
 * 负责管理贴纸搜索结果、加载状态、已下载文件列表的全局缓存
 */
object StickerCache {
    /** 贴纸搜索结果URL列表 */
    val searchResults = mutableStateListOf<String>()

    /** 数据加载状态标记 */
    var isLoading by mutableStateOf(true)

    /** 已下载贴纸文件名集合 */
    var downloadedFiles by mutableStateOf(emptySet<String>())

    /**
     * 初始化热门贴纸数据
     * 首次加载时默认搜索"可爱"关键词的贴纸
     *
     * @param context 上下文
     */
    fun initHotStickers(context: Context) {
        if (searchResults.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                isLoading = true
                searchResults.addAll(fetchFromSogou("可爱"))
                downloadedFiles = getDownloadedFileNames(context)
                isLoading = false
            }
        }
    }
}

/**
 * 首页贴纸浏览与搜索界面
 * 提供贴纸搜索、展示、下载、删除等核心功能
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 搜索框输入内容状态
    var searchQuery by remember { mutableStateOf("") }

    // 页面初始化：加载热门贴纸数据
    LaunchedEffect(Unit) {
        StickerCache.initHotStickers(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        // 搜索框区域
        SearchTextField(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearch = {
                focusManager.clearFocus()
                scope.launch {
                    StickerCache.isLoading = true
                    StickerCache.searchResults.clear()
                    val query = if (searchQuery.isEmpty()) "可爱" else searchQuery
                    StickerCache.searchResults.addAll(fetchFromSogou(query))
                    StickerCache.isLoading = false
                }
            }
        )

        // 加载进度指示器
        if (StickerCache.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 空状态展示
        if (!StickerCache.isLoading && StickerCache.searchResults.isEmpty()) {
            EmptyStateView()
        }

        // 贴纸网格列表
        StickerGridView(
            context = context,
            scope = scope
        )
    }
}

/**
 * 搜索输入框组件
 *
 * @param searchQuery 当前搜索关键词
 * @param onSearchQueryChange 搜索关键词变更回调
 * @param onSearch 搜索执行回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTextField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("搜索搜狗表情包...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索图标") },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/**
 * 空状态展示组件
 * 当搜索结果为空时显示的提示界面
 */
@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.found_fail),
                contentDescription = "暂无表情包",
                modifier = Modifier.size(120.dp),
                alpha = 0.6f
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "没有搜索到相关表情包",
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 贴纸网格展示组件
 *
 * @param context 上下文
 * @param scope 协程作用域
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerGridView(
    context: Context,
    scope: CoroutineScope
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(StickerCache.searchResults) { url ->
            val fileName = generateFileName(url)
            val isDownloaded = StickerCache.downloadedFiles.contains(fileName)
            var isDownloading by remember { mutableStateOf(false) }

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
                                isDownloading = true
                                scope.launch {
                                    downloadSticker(context, url) {
                                        StickerCache.downloadedFiles = getDownloadedFileNames(context)
                                        isDownloading = false
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            if (isDownloaded) {
                                deleteSticker(context, fileName) {
                                    StickerCache.downloadedFiles = getDownloadedFileNames(context)
                                    Toast.makeText(context, "已成功移除", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
            ) {
                // 贴纸图片展示
                AsyncImage(
                    model = url,
                    contentDescription = "贴纸图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 下载中进度提示
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }

                // 已下载状态标记
                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "已下载",
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 从搜狗图片接口获取贴纸数据
 *
 * @param keyword 搜索关键词
 * @return 贴纸图片URL列表
 */
suspend fun fetchFromSogou(keyword: String): List<String> = withContext(Dispatchers.IO) {
    val stickerList = mutableListOf<String>()
    try {
        // 编码搜索关键词
        val encodedKey = URLEncoder.encode(keyword, "UTF-8")
        // 搜狗图片搜索API接口
        val apiUrl = "https://qyapi.ipaybuy.cn/api/bqbsougou?id=115016&key=1a4f8drgz00df6zzov1pe9j100edlct1&words=$encodedKey&page=1"

        // 发起网络请求并解析响应
        val response = URL(apiUrl).readText()
        val jsonObject = JSONObject(response)

        // 解析贴纸URL数组
        if (jsonObject.has("res")) {
            val resArray = jsonObject.getJSONArray("res")
            for (i in 0 until resArray.length()) {
                stickerList.add(resArray.getString(i))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    stickerList
}

/**
 * 根据URL生成唯一的文件名
 * 使用MD5哈希保证文件名唯一性
 *
 * @param url 图片URL
 * @return 唯一文件名
 */
fun generateFileName(url: String): String {
    return try {
        val md5Digest = MessageDigest.getInstance("MD5")
        val hashBytes = md5Digest.digest(url.toByteArray())
        val fileName = hashBytes.joinToString("") { "%02x".format(it) }
        "sogou_$fileName.png"
    } catch (e: Exception) {
        // 哈希生成失败时使用时间戳兜底
        "sogou_${System.currentTimeMillis()}.png"
    }
}

/**
 * 获取已下载贴纸文件名称集合
 *
 * @param context 上下文
 * @return 已下载文件名称集合
 */
fun getDownloadedFileNames(context: Context): Set<String> {
    val downloadFolder = File(context.filesDir, "downloaded_stickers")
    return downloadFolder.listFiles()?.map { it.name }?.toSet() ?: emptySet()
}

/**
 * 下载贴纸文件到本地存储
 *
 * @param context 上下文
 * @param url 贴纸图片URL
 * @param onDone 下载完成回调
 */
suspend fun downloadSticker(context: Context, url: String, onDone: () -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            // 创建下载目录
            val downloadFolder = File(context.filesDir, "downloaded_stickers")
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs()
            }

            // 创建文件并写入数据
            val stickerFile = File(downloadFolder, generateFileName(url))
            URL(url).openStream().use { inputStream ->
                stickerFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 主线程回调并提示
            withContext(Dispatchers.Main) {
                onDone()
                Toast.makeText(context, "下载成功，请去编辑器查看", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * 删除已下载的贴纸文件
 *
 * @param context 上下文
 * @param fileName 要删除的文件名
 * @param onDone 删除完成回调
 */
fun deleteSticker(context: Context, fileName: String, onDone: () -> Unit) {
    val stickerFile = File(File(context.filesDir, "downloaded_stickers"), fileName)
    if (stickerFile.exists() && stickerFile.delete()) {
        onDone()
    }
}