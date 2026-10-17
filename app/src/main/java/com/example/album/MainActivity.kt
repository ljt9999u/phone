package com.example.album

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.album.ui.MainScreen
import com.example.album.ui.auth.AuthViewModel
import com.example.album.ui.auth.LoginScreen
import com.example.album.ui.auth.RegisterScreen
import com.example.album.ui.auth.ResetSecretScreen
import com.example.album.ui.auth.SecretSavedScreen
import com.example.album.ui.theme.AlbumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbumTheme {
                AuthRoot(viewModel())
            }
        }
    }
}

/** 认证流程导航：登录 ⇄ 注册 / 重置安全码 → 安全码展示 → 主界面 */
@Composable
fun AuthRoot(vm: AuthViewModel) {
    var showRegister by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    // 安全码只显示一次，优先于一切页面（含「我的」页内发起的重置）
    val reveal = vm.secretReveal
    if (reveal != null) {
        BackHandler(enabled = true) { }
        SecretSavedScreen(
            reveal = reveal,
            onDone = {
                vm.consumeSecretReveal()
                if (reveal.fromRegister) {
                    vm.enterApp()
                } else {
                    showReset = false
                    vm.exitToLogin() // 会话已失效，回登录页
                }
            }
        )
        return
    }

    if (vm.loggedIn) {
        MainScreen(vm)
        return
    }

    BackHandler(enabled = showRegister || showReset) {
        showRegister = false
        showReset = false
    }

    when {
        showRegister -> RegisterScreen(vm, onBack = { showRegister = false })
        showReset -> ResetSecretScreen(vm, onBack = { showReset = false })
        else -> LoginScreen(
            vm = vm,
            onGoRegister = { showRegister = true },
            onGoReset = { showReset = true }
        )
    }
}
