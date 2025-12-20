package com.example.mememaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mememaster.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 这里我们不使用默认主题，直接调用我们的主屏幕
            // 后续我们会在 ui/theme 里定义更高级的主题
            MainScreen()
        }
    }
}