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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.mememaster.model.ComponentType
import com.example.mememaster.model.MemeComponent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object MemeSaver {

    /**
     * @param context 上下文
     * @param baseBitmap 原始高清底图
     * @param components 组件列表
     * @param containerWidth 屏幕上编辑区域(灰色Box)的宽度 (px)
     * @param containerHeight 屏幕上编辑区域(灰色Box)的高度 (px)
     * @param density 屏幕密度 (用于将 dp 转为 px)
     */
    fun createBitmap(
        context: Context,
        baseBitmap: Bitmap,
        components: List<MemeComponent>,
        containerWidth: Int,
        containerHeight: Int,
        density: Float
    ): Bitmap {
        // 1. 创建画布
        // 为了保证画质，必须在原始图片大小上进行绘制，而不是屏幕截图大小
        val resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        // 2. 计算 ContentScale.Fit 的数学逻辑
        // 计算图片和容器的宽高比
        val viewAspectRatio = containerWidth.toFloat() / containerHeight.toFloat()
        val bitmapAspectRatio = baseBitmap.width.toFloat() / baseBitmap.height.toFloat()

        // 计算缩放比例 (scale) 和 偏移量 (dx, dy)
        // scale 是 "原始图片像素" 到 "屏幕显示像素" 的比例
        val scale: Float
        val dx: Float // 图片在容器内的左边距 (px)
        val dy: Float // 图片在容器内的上边距 (px)

        if (bitmapAspectRatio > viewAspectRatio) {
            // 图片更宽 -> 宽度填满，高度居中
            scale = containerWidth.toFloat() / baseBitmap.width.toFloat()
            dx = 0f
            dy = (containerHeight - baseBitmap.height * scale) / 2f
        } else {
            // 图片更高 -> 高度填满，宽度居中
            scale = containerHeight.toFloat() / baseBitmap.height.toFloat()
            dx = (containerWidth - baseBitmap.width * scale) / 2f
            dy = 0f
        }

        // 3. 遍历绘制组件
        components.forEach { component ->
            canvas.save()

            // --- 坐标转换核心公式 ---
            // 1. 获取组件在屏幕上的坐标 (DP -> PX)
            val screenX = component.offset.x * density
            val screenY = component.offset.y * density

            // 2. 减去图片的显示偏移量 (dx, dy)，得到相对于图片显示区域左上角的坐标
            val relativeX = screenX - dx
            val relativeY = screenY - dy

            // 3. 除以缩放比例，还原到原始图片的像素坐标系
            val finalX = relativeX / scale
            val finalY = relativeY / scale

            // 移动画布焦点到目标位置
            canvas.translate(finalX, finalY)
            // 应用旋转
            canvas.rotate(component.rotation)
            // 应用缩放 (组件自身的缩放)
            canvas.scale(component.scale, component.scale)

            when (val type = component.type) {
                is ComponentType.Text -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = type.color.hashCode() // 简单转换颜色，建议完善 Color 转 Int 逻辑
                        style = Paint.Style.FILL
                        // 字体大小修正：
                        // UI用的是 HeadlineMedium (约28sp)。
                        // 计算公式：屏幕上的像素大小 / 图片缩放比例
                        textSize = (28f * density) / scale
                        setShadowLayer(5f / scale, 0f, 0f, android.graphics.Color.BLACK)
                    }

                    // 文字垂直对齐修正
                    // Android Canvas drawText 的 Y 是基线(Baseline)，而 Compose 的 Offset 是左上角(Top-Left)
                    // 需要向下移动一个 Ascent 的距离
                    val fontMetrics = paint.fontMetrics
                    val baselineOffset = -fontMetrics.ascent // ascent 是负数，所以取反

                    canvas.drawText(type.content, 0f, baselineOffset, paint)
                }
                is ComponentType.Sticker -> {
                    val stickerBitmap = BitmapFactory.decodeResource(context.resources, type.resId)
                    if (stickerBitmap != null) {
                        // UI 上贴图默认大小是 100dp
                        // 计算贴图在原始图片中应该占据的像素宽度
                        val targetBaseWidthOnBitmap = (100f * density) / scale

                        val stickerScale = targetBaseWidthOnBitmap / stickerBitmap.width

                        // 绘制贴图
                        val matrix = Matrix()
                        matrix.postScale(stickerScale, stickerScale)
                        // 因为我们已经 translate 到了中心点附近 (Compose offset 通常是组件左上角)
                        // 但 transformable 在 UI 上可能会导致 offset 变为中心点，这取决于你的 UI 实现细节。
                        // 这里假设 component.offset 指的是组件的 左上角。

                        canvas.drawBitmap(stickerBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
                        stickerBitmap.recycle()
                    }
                }
                is ComponentType.RemoteSticker -> {
                    val inputStream = context.contentResolver.openInputStream(type.uri)
                    val stickerBitmap = BitmapFactory.decodeStream(inputStream)
                    if (stickerBitmap != null) {
                        val targetBaseWidthOnBitmap = (100f * density) / scale
                        val stickerScale = targetBaseWidthOnBitmap / stickerBitmap.width
                        val matrix = Matrix()
                        matrix.postScale(stickerScale, stickerScale)
                        canvas.drawBitmap(stickerBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
                        stickerBitmap.recycle()
                    }
                }
                else -> {}
            }

            canvas.restore()
        }

        return resultBitmap
    }

    // 2. 新增：保存到应用私有仓库 (Internal Storage)
    fun saveToInternalStorage(context: Context, bitmap: Bitmap): Boolean {
        return try {
            // 创建文件名：Meme_时间戳.jpg
            val fileName = "Meme_${System.currentTimeMillis()}.jpg"
            // 获取应用私有目录 files/my_memes
            val directory = File(context.filesDir, "my_memes")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)

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

    // 3. 导出：将私有文件导出到系统相册 (External Storage)
    fun exportToSystemGallery(context: Context, file: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            val filename = "Export_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/MemeMaster")
                }
            }

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