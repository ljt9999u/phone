package com.example.album.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.album.ui.theme.AlbumTheme
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.RoseError

/** 连接 ViewModel 的包装层 */
@Composable
fun RegisterScreen(vm: AuthViewModel, onBack: () -> Unit) {
    RegisterContent(
        loading = vm.registerLoading,
        errorMessage = vm.errorMessage,
        onClearError = { vm.clearError() },
        onRegister = { phone, pwd, nick -> vm.register(phone, pwd, nick) },
        onBack = onBack
    )
}

/** 无状态页面内容：可独立预览 */
@Composable
fun RegisterContent(
    loading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onRegister: (phone: String, password: String, nickname: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    var phone by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var fieldError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "创建账号",
            subtitle = "开始收集你的温柔时光",
            onBack = onBack
        )

        Column(
            Modifier
                .offset(y = (-44).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                Text("注册安坐", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                Spacer(Modifier.height(4.dp))
                Text("注册成功会生成专属安全码，请注意保存", fontSize = 13.sp, color = InkSecondary)
                Spacer(Modifier.height(22.dp))

                SoftTextField(
                    value = phone,
                    onValueChange = { phone = it; fieldError = null; onClearError() },
                    label = "手机号",
                    placeholder = "11 位手机号",
                    keyboardType = KeyboardType.Phone
                )
                Spacer(Modifier.height(14.dp))
                SoftTextField(
                    value = nickname,
                    onValueChange = { nickname = it; fieldError = null },
                    label = "昵称（可选）",
                    placeholder = "大家怎么称呼你"
                )
                Spacer(Modifier.height(14.dp))
                SoftTextField(
                    value = password,
                    onValueChange = { password = it; fieldError = null; onClearError() },
                    label = "密码",
                    isPassword = true
                )
                Spacer(Modifier.height(14.dp))
                SoftTextField(
                    value = confirm,
                    onValueChange = { confirm = it; fieldError = null; onClearError() },
                    label = "确认密码",
                    isPassword = true
                )

                fieldError?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, fontSize = 13.sp, color = RoseError)
                }
                errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = it, onDismiss = onClearError)
                }

                Spacer(Modifier.height(22.dp))
                PrimaryPinkButton(
                    text = "注 册",
                    enabled = phone.isNotBlank() && password.isNotBlank() && confirm.isNotBlank(),
                    loading = loading
                ) {
                    val p = phone.trim()
                    when {
                        !PHONE_REGEX.matches(p) -> fieldError = "手机号格式不正确"
                        password.length !in 6..64 -> fieldError = "密码长度需在 6-64 位之间"
                        password != confirm -> fieldError = "两次输入的密码不一致"
                        nickname.length > 50 -> fieldError = "昵称最长 50 个字"
                        else -> onRegister(p, password, nickname)
                    }
                }

                Spacer(Modifier.height(16.dp))
                FooterLink("已有账号？", "返回登录", onBack)
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    AlbumTheme {
        RegisterContent()
    }
}
