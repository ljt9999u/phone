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
fun ResetSecretScreen(vm: AuthViewModel, onBack: () -> Unit) {
    ResetSecretContent(
        initialPhone = vm.prefs.phone,
        loading = vm.resetLoading,
        errorMessage = vm.errorMessage,
        onClearError = { vm.clearError() },
        onReset = { phone, pwd -> vm.resetSecret(phone, pwd) },
        onBack = onBack
    )
}

/** 无状态页面内容：可独立预览 */
@Composable
fun ResetSecretContent(
    initialPhone: String = "",
    loading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    onReset: (phone: String, password: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var password by rememberSaveable { mutableStateOf("") }
    var fieldError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "重置安全码",
            subtitle = "凭手机号和密码验证身份",
            onBack = onBack
        )

        Column(
            Modifier
                .offset(y = (-44).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                Text("找回安全码", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "验证通过后生成新的安全码：原安全码作废，所有设备将被强制下线。",
                    fontSize = 13.sp,
                    color = InkSecondary,
                    lineHeight = 19.sp
                )
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
                    label = "登录密码",
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
                    text = "重置安全码",
                    enabled = phone.isNotBlank() && password.isNotBlank(),
                    loading = loading
                ) {
                    val p = phone.trim()
                    when {
                        !PHONE_REGEX.matches(p) -> fieldError = "手机号格式不正确"
                        password.isEmpty() -> fieldError = "请输入登录密码"
                        else -> onReset(p, password)
                    }
                }

                Spacer(Modifier.height(16.dp))
                FooterLink("想起来了？", "返回登录", onBack)
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ResetSecretScreenPreview() {
    AlbumTheme {
        ResetSecretContent(initialPhone = "13800138000")
    }
}
