package com.example.mememaster.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.mememaster.model.ComponentType
import com.example.mememaster.model.MemeComponent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 表情包保存工具类
 * 负责表情包的位图合成、内部存储保存、系统相册导出等核心功能
 */
object MemeSaver {

    /**
     * 合成最终的表情包位图
     * 基于原始底图和用户添加的组件（文字/贴纸），在原始图片像素尺寸上进行绘制，保证画质无损
     *
     * @param context 上下文
     * @param baseBitmap 原始高清底图
     * @param components 表情包组件列表（文字/贴纸）
     * @param containerWidth 屏幕编辑区域宽度（px）
     * @param containerHeight 屏幕编辑区域高度（px）
     * @param density 屏幕密度（用于DP与PX单位转换）
     * @return 合成后的最终位图
     */
    fun createBitmap(
        context: Context,
        baseBitmap: Bitmap,
        components: List<MemeComponent>,
        containerWidth: Int,
        containerHeight: Int,
        density: Float
    ): Bitmap {
        // 创建可编辑的位图副本，基于原始图片尺寸保证画质
        val resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        // 计算ContentScale.Fit模式下的缩放比例和偏移量
        // 用于将屏幕坐标转换为原始图片像素坐标
        val viewAspectRatio = containerWidth.toFloat() / containerHeight.toFloat()
        val bitmapAspectRatio = baseBitmap.width.toFloat() / baseBitmap.height.toFloat()

        val scale: Float
        val dx: Float // 图片在容器内的水平偏移（px）
        val dy: Float // 图片在容器内的垂直偏移（px）

        if (bitmapAspectRatio > viewAspectRatio) {
            // 图片宽高比大于容器：宽度填满容器，高度居中
            scale = containerWidth.toFloat() / baseBitmap.width.toFloat()
            dx = 0f
            dy = (containerHeight - baseBitmap.height * scale) / 2f
        } else {
            // 图片宽高比小于容器：高度填满容器，宽度居中
            scale = containerHeight.toFloat() / baseBitmap.height.toFloat()
            dx = (containerWidth - baseBitmap.width * scale) / 2f
            dy = 0f
        }

        // 遍历绘制所有表情包组件
        components.forEach { component ->
            canvas.save()

            // 坐标转换：将屏幕DP坐标转换为原始图片像素坐标
            // 1. DP转PX
            val screenX = component.offset.x * density
            val screenY = component.offset.y * density

            // 2. 计算组件相对于图片显示区域的坐标
            val relativeX = screenX - dx
            val relativeY = screenY - dy

            // 3. 还原到原始图片像素坐标系
            val finalX = relativeX / scale
            val finalY = relativeY / scale

            // 应用组件的位置、旋转、缩放变换
            canvas.translate(finalX, finalY)
            canvas.rotate(component.rotation)
            canvas.scale(component.scale, component.scale)

            // 根据组件类型进行绘制
            when (val type = component.type) {
                is ComponentType.Text -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = type.color.toArgb()
                        style = Paint.Style.FILL
                        // 文字大小：UI基准值(28sp)转换为原始图片像素尺寸
                        textSize = (28f * density) / scale
                        // 文字阴影：按比例适配原始图片尺寸
                        setShadowLayer(5f / scale, 0f, 0f, android.graphics.Color.BLACK)
                    }

                    // 修正文字垂直对齐：Canvas.drawText的Y轴是基线，需转换为左上角坐标
                    val fontMetrics = paint.fontMetrics
                    val baselineOffset = -fontMetrics.ascent
                    canvas.drawText(type.content, 0f, baselineOffset, paint)
                }

                is ComponentType.Sticker -> {
                    // 加载本地贴纸资源
                    val stickerBitmap = BitmapFactory.decodeResource(context.resources, type.resId)
                    stickerBitmap?.let {
                        // 计算贴纸在原始图片中的显示尺寸（基准100dp转换为原始像素）
                        val targetBaseWidthOnBitmap = (100f * density) / scale
                        val stickerScale = targetBaseWidthOnBitmap / it.width

                        // 绘制贴纸
                        val matrix = Matrix()
                        matrix.postScale(stickerScale, stickerScale)
                        canvas.drawBitmap(it, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
                        it.recycle() // 释放位图资源
                    }
                }

                is ComponentType.RemoteSticker -> {
                    // 加载远程/本地文件贴纸
                    val inputStream = context.contentResolver.openInputStream(type.uri)
                    val stickerBitmap = BitmapFactory.decodeStream(inputStream)
                    stickerBitmap?.let {
                        // 计算贴纸在原始图片中的显示尺寸（基准100dp转换为原始像素）
                        val targetBaseWidthOnBitmap = (100f * density) / scale
                        val stickerScale = targetBaseWidthOnBitmap / it.width

                        // 绘制贴纸
                        val matrix = Matrix()
                        matrix.postScale(stickerScale, stickerScale)
                        canvas.drawBitmap(it, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
                        it.recycle() // 释放位图资源
                    }
                }
            }

            canvas.restore()
        }

        return resultBitmap
    }

    /**
     * 将合成的位图保存到应用私有内部存储
     * 存储路径：/data/data/包名/files/my_memes/
     *
     * @param context 上下文
     * @param bitmap 要保存的位图
     * @return 保存是否成功
     */
    fun saveToInternalStorage(context: Context, bitmap: Bitmap): Boolean {
        return try {
            // 生成唯一文件名：Meme_时间戳.jpg
            val fileName = "Meme_${System.currentTimeMillis()}.jpg"
            // 创建存储目录
            val directory = File(context.filesDir, "my_memes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

            // 保存位图为JPEG格式
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
            Toast.makeText(context, "已存入仓库", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * 将应用私有存储的表情包文件导出到系统相册
     * Android Q及以上保存到DCIM/MemeMaster目录，以下版本按系统默认处理
     *
     * @param context 上下文
     * @param file 要导出的本地文件
     */
    fun exportToSystemGallery(context: Context, file: File) {
        try {
            // 读取私有存储的位图文件
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            // 构建媒体库插入参数
            val filename = "Export_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/MemeMaster")
                }
            }

            // 插入到系统媒体库并写入文件
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    Toast.makeText(context, "导出成功！请查看相册", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}