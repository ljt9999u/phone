package com.example.album.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.album.data.model.DeviceItem
import com.example.album.data.util.ImageStore
import com.example.album.ui.auth.AuthCard
import com.example.album.ui.auth.AuthViewModel
import com.example.album.ui.auth.ErrorBanner
import com.example.album.ui.auth.GradientHeader
import com.example.album.ui.auth.PrimaryPinkButton
import com.example.album.ui.auth.SoftTextField
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.MistBlueLight
import com.example.album.ui.theme.MistBlueText
import com.example.album.ui.theme.RoseDeep
import com.example.album.ui.theme.SakuraPink
import com.example.album.ui.theme.SakuraPinkLight

@Composable
fun ProfileScreen(authVm: AuthViewModel, vm: ProfileViewModel) {
    LaunchedEffect(Unit) { vm.loadIfNeeded() }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val me = vm.me
    val displayName = me?.nickname?.takeIf { it.isNotBlank() }
        ?: authVm.prefs.nickname.ifBlank { "朋友" }
    val phone = me?.phone ?: authVm.prefs.phone
    val isAdmin = me?.admin ?: authVm.prefs.isAdmin

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "你好，$displayName",
            subtitle = if (isAdmin) "管理员模式" else "把日子过成喜欢的样子",
            height = 210.dp
        )

        Column(
            Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 24.dp)
        ) {
            vm.errorMessage?.let {
                ErrorBanner(message = it, onDismiss = { vm.clearError() })
                Spacer(Modifier.height(12.dp))
            }

            // ---- 个人信息 ----
            AuthCard {
                SectionTitle("个人信息")
                InfoRow("手机号", phone)
                InfoRow("昵称", displayName)
                InfoRow("用户 ID", (me?.id ?: authVm.prefs.userId).toString())
                InfoRow("身份", if (isAdmin) "管理员" else "普通用户")
            }
            Spacer(Modifier.height(16.dp))

            // ---- 账号安全 ----
            AuthCard {
                SectionTitle("账号安全")
                Text(
                    "修改密码：后端接口待提供，暂可通过重置安全码保护账号。",
                    fontSize = 12.sp,
                    color = InkSecondary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))
                PrimaryPinkButton(text = "重置安全码") { showResetDialog = true }
            }
            Spacer(Modifier.height(16.dp))

            // ---- 登录设备 ----
            AuthCard {
                SectionTitle("登录设备（最近 ${vm.devices.size} 条）")
                if (vm.devices.isEmpty()) {
                    Text(
                        if (vm.loading) "加载中…" else "暂无记录",
                        fontSize = 13.sp,
                        color = InkSecondary
                    )
                } else {
                    vm.devices.forEach { d ->
                        DeviceRow(d)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            PrimaryPinkButton(text = "退出登录", loading = authVm.loggingOut) {
                authVm.logout()
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    // 重置安全码：先说明，再验证密码
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("重置安全码", color = InkPrimary) },
            text = {
                Text(
                    "重置后原安全码作废、所有设备强制下线（包括当前设备），需要用新安全码重新登录。确定继续吗？",
                    color = InkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    showPasswordDialog = true
                }) { Text("继续", color = RoseDeep) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消", color = InkSecondary)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    }

    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("验证登录密码", color = InkPrimary) },
            text = {
                SoftTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "登录密码",
                    isPassword = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        authVm.resetSecret(phone, password)
                    },
                    enabled = password.isNotBlank() && !authVm.resetLoading
                ) { Text("确认重置", color = RoseDeep) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("取消", color = InkSecondary)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = InkSecondary)
        Text(value, fontSize = 14.sp, color = InkPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DeviceRow(d: DeviceItem) {
    val online = d.logoutTime == null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (online) SakuraPink else MistBlueLight)
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                d.deviceName ?: "未知设备",
                fontSize = 14.sp,
                color = InkPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${d.loginIp ?: "-"} · ${ImageStore.formatTime(d.loginTime ?: "")}",
                fontSize = 12.sp,
                color = InkSecondary
            )
        }
        Text(
            if (online) "在线" else "已下线",
            fontSize = 12.sp,
            color = if (online) RoseDeep else InkSecondary,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (online) SakuraPinkLight else MistBlueLight)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
