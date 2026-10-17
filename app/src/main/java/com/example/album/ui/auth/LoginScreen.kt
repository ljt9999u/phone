package com.example.album.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.album.ui.theme.MistBlueText
import com.example.album.ui.theme.RoseError

internal val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")

/** 连接 ViewModel 的包装层 */
@Composable
fun LoginScreen(vm: AuthViewModel, onGoRegister: () -> Unit, onGoReset: () -> Unit) {
    LoginContent(
        initialPhone = vm.prefs.phone,
        initialSecret = vm.prefs.secretUuid,
        loading = vm.loginLoading,
        errorMessage = vm.errorMessage,
        onClearError = { vm.clearError() },
        onLogin = { phone, pwd, secret -> vm.login(phone, pwd, secret) },
        onGoRegister = onGoRegister,
        onGoReset = onGoReset
    )
}

/** 无状态页面内容：可独立预览 */
@Composable
fun LoginContent(
    initialPhone: String = "",
    initialSecret: String = "",
    loading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onLogin: (phone: String, password: String, secret: String) -> Unit = { _, _, _ -> },
    onGoRegister: () -> Unit = {},
    onGoReset: () -> Unit = {}
) {
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var password by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf(initialSecret) }
    var fieldError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(title = "安坐", subtitle = "把温柔的日子，一张一张收好")

        Column(
            Modifier
                .offset(y = (-44).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                Text("欢迎回来", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                Spacer(Modifier.height(4.dp))
                Text("登录后继续整理你的回忆", fontSize = 13.sp, color = InkSecondary)
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
                    value = password,
                    onValueChange = { password = it; fieldError = null; onClearError() },
                    label = "密码",
                    isPassword = true
                )
                Spacer(Modifier.height(14.dp))
                SoftTextField(
                    value = secret,
                    onValueChange = { secret = it; fieldError = null; onClearError() },
                    label = "安全码",
                    placeholder = "注册时保存的安全码"
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
                    text = "登 录",
                    enabled = phone.isNotBlank() && password.isNotBlank() && secret.isNotBlank(),
                    loading = loading
                ) {
                    val p = phone.trim()
                    when {
                        !PHONE_REGEX.matches(p) -> fieldError = "手机号格式不正确"
                        password.isEmpty() -> fieldError = "请输入登录密码"
                        secret.trim().isEmpty() -> fieldError = "请输入安全码"
                        else -> onLogin(p, password, secret.trim())
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "忘记安全码？",
                    fontSize = 13.sp,
                    color = MistBlueText,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onGoReset)
                        .padding(6.dp)
                )
                Spacer(Modifier.height(6.dp))
                FooterLink("还没有账号？", "立即注册", onGoRegister)
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    AlbumTheme {
        LoginContent(
            initialPhone = "13800138000",
            initialSecret = "11111111-1111-1111-1111-111111111111"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "错误态")
@Composable
private fun LoginScreenErrorPreview() {
    AlbumTheme {
        LoginContent(errorMessage = "手机号、密码或安全码错误")
    }
}
