package com.example.album.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.album.ui.auth.AuthViewModel
import com.example.album.ui.photo.GalleryScreen
import com.example.album.ui.photo.PhotoViewModel
import com.example.album.ui.photo.UploadScreen
import com.example.album.ui.profile.ProfileScreen
import com.example.album.ui.profile.ProfileViewModel
import com.example.album.ui.theme.AlbumTheme
import com.example.album.ui.theme.CardWhite
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.RoseDeep
import com.example.album.ui.theme.SakuraPinkLight

private data class TabItem(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("上传", Icons.Filled.CloudUpload),
    TabItem("照片", Icons.Filled.PhotoLibrary),
    TabItem("我的", Icons.Filled.AccountCircle)
)

/** 登录后的主框架：底部三 Tab（上传 / 照片 / 我的） */
@Composable
fun MainScreen(authVm: AuthViewModel) {
    val photoVm: PhotoViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()

    MainScaffold { tab ->
        when (tab) {
            0 -> UploadScreen(photoVm)
            1 -> GalleryScreen(photoVm)
            else -> ProfileScreen(authVm, profileVm)
        }
    }
}

/** 无状态脚手架：底部导航 + 页面容器，便于预览 */
@Composable
fun MainScaffold(content: @Composable (Int) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            NavigationBar(containerColor = CardWhite, tonalElevation = 6.dp) {
                TABS.forEachIndexed { index, item ->
                    val selected = tab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = index },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RoseDeep,
                            selectedTextColor = RoseDeep,
                            indicatorColor = SakuraPinkLight,
                            unselectedIconColor = InkSecondary,
                            unselectedTextColor = InkSecondary
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content(tab)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "主框架")
@Composable
private fun MainScaffoldPreview() {
    AlbumTheme {
        MainScaffold { tab ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(CreamBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "当前页面：${TABS[tab].label}",
                    color = InkSecondary,
                    fontSize = 16.sp
                )
            }
        }
    }
}
