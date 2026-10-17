package com.example.album.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary

/** 注册 / 重置成功后的安全码唯一展示页：仅显示这一次，引导用户立即保存 */
@Composable
fun SecretSavedScreen(reveal: SecretReveal, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "保存安全码",
            subtitle = "它只显示这一次，请妥善保管"
        )

        Column(
            Modifier
                .offset(y = (-44).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                Text(
                    if (reveal.fromRegister) "注册成功" else "重置成功",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                reveal.notice?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 13.sp, color = InkSecondary, lineHeight = 19.sp)
                }
                Spacer(Modifier.height(20.dp))

                Text("你的安全码", fontSize = 13.sp, color = InkSecondary)
                Spacer(Modifier.height(8.dp))
                SecretValueBox(reveal.secretUuid)

                Spacer(Modifier.height(16.dp))
                Text(
                    "请立即复制并妥善保存。安全码丢失后无法找回，" +
                        "只能通过手机号和密码重置；重置后原安全码作废、所有设备强制下线。",
                    fontSize = 12.sp,
                    color = InkSecondary,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(24.dp))
                PrimaryPinkButton(
                    text = if (reveal.fromRegister) "我已保存，进入安坐" else "我已保存，去登录",
                    onClick = onDone
                )
            }
            Spacer(Modifier.height(64.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SecretSavedScreenPreview() {
    com.example.album.ui.theme.AlbumTheme {
        SecretSavedScreen(
            reveal = SecretReveal(
                secretUuid = "e2779600-507b-4ec5-9963-2da7d9b1ee9d",
                notice = null,
                fromRegister = true
            ),
            onDone = {}
        )
    }
}
