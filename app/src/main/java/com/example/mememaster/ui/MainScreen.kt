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

/**
 * 应用主界面
 * 包含底部导航栏和页面导航框架，管理发现、制作、仓库三个核心页面的切换
 */
@Composable
fun MainScreen() {
    // 导航控制器：管理页面跳转
    val navController = rememberNavController()

    // 底部导航配置
    val navItems = listOf("发现", "制作", "仓库")
    val navIcons = listOf(Icons.Default.Home, Icons.Default.Create, Icons.Default.Person)

    // 当前选中的导航项索引
    var selectedNavItem by remember { mutableIntStateOf(0) }

    Scaffold(
        // 底部导航栏
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(navIcons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedNavItem == index,
                        onClick = {
                            selectedNavItem = index
                            // 根据索引跳转对应页面
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
        // 导航主机：管理页面内容展示
        NavHost(
            navController = navController,
            startDestination = "home", // 默认显示发现页
            modifier = Modifier.padding(innerPadding)
        ) {
            // 发现页面路由
            composable("home") { HomeScreen() }
            // 制作页面路由
            composable("editor") { EditorScreen() }
            // 仓库页面路由
            composable("gallery") { GalleryScreen() }
        }
    }
}