package com.example.mememaster.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mememaster.ui.editor.EditorScreen
import com.example.mememaster.ui.gallery.GalleryScreen
import com.example.mememaster.ui.home.HomeScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    // 定义底部导航的选项
    val items = listOf("发现", "制作", "仓库")
    val icons = listOf(Icons.Default.Home, Icons.Default.Create, Icons.Default.Person)
    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White, // 高质感白底
                contentColor = Color.Black
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            // 路由跳转逻辑
                            when (index) {
                                0 -> navController.navigate("home")
                                1 -> navController.navigate("editor")
                                2 -> navController.navigate("gallery")
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 导航主机：决定显示哪个页面
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("editor") { EditorScreen() }
            composable("gallery") { GalleryScreen() }
        }
    }
}