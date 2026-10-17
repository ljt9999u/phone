package com.example.album.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.album.ui.theme.CardWhite
import com.example.album.ui.theme.FieldBg
import com.example.album.ui.theme.FieldBorder
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.MistBlue
import com.example.album.ui.theme.MistBlueText
import com.example.album.ui.theme.RoseDeep
import com.example.album.ui.theme.RoseError
import com.example.album.ui.theme.SakuraPink
import com.example.album.ui.theme.SakuraPinkDeep
import com.example.album.ui.theme.SakuraPinkLight

private val CardShape = RoundedCornerShape(20.dp)
private val ButtonShape = RoundedCornerShape(27.dp)
private val FieldShape = RoundedCornerShape(14.dp)

/**
 * 顶部视觉重心：樱花粉 → 雾霾蓝渐变，底部大圆角，
 * 配少量半透明装饰圆营造柔和氛围（约 30% 粉 + 10% 蓝的来源）。
 */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String,
    height: Dp = 240.dp,
    onBack: (() -> Unit)? = null
) {
    val headerShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(headerShape)
            .background(
                Brush.linearGradient(
                    0f to SakuraPink,
                    0.55f to SakuraPinkLight,
                    1f to MistBlue,
                    start = Offset.Zero,
                    end = Offset(900f, 900f)
                )
            )
    ) {
        // 装饰圆：右上大圆、左下小圆、右下雾霾蓝圆
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 42.dp, y = (-34).dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-26).dp, y = 26.dp)
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 88.dp, bottom = 34.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(MistBlue.copy(alpha = 0.45f))
        )

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            if (onBack != null) {
                Spacer(Modifier.height(6.dp))
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = RoseDeep
                    )
                }
                Spacer(Modifier.height(2.dp))
            } else {
                Spacer(Modifier.height(34.dp))
            }
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = RoseDeep)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 14.sp, color = RoseDeep.copy(alpha = 0.62f))
        }
    }
}

/** 内容卡片：纯白 + 大圆角 + 极淡粉色调阴影，在奶白底上更立体 */
@Composable
fun AuthCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = CardShape,
                ambientColor = SakuraPink.copy(alpha = 0.30f),
                spotColor = SakuraPink.copy(alpha = 0.30f)
            )
            .clip(CardShape)
            .background(CardWhite)
            .padding(horizontal = 24.dp, vertical = 26.dp),
        content = content
    )
}

/** 柔和输入框：奶粉白底 + 极淡粉描边，聚焦时樱花粉 */
@Composable
fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    var showPassword by remember { mutableStateOf(false) }
    val selectionColors = TextSelectionColors(
        handleColor = SakuraPinkDeep,
        backgroundColor = SakuraPink.copy(alpha = 0.30f)
    )
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = if (placeholder.isEmpty()) {
                null
            } else {
                { Text(placeholder, color = InkSecondary.copy(alpha = 0.75f), fontSize = 14.sp) }
            },
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            visualTransformation = if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = {
                if (isPassword) {
                    Text(
                        text = if (showPassword) "隐藏" else "显示",
                        color = MistBlueText,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { showPassword = !showPassword }
                            .padding(horizontal = 6.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                disabledContainerColor = FieldBg,
                focusedBorderColor = SakuraPink,
                unfocusedBorderColor = FieldBorder,
                cursorColor = SakuraPinkDeep,
                focusedLabelColor = RoseDeep,
                unfocusedLabelColor = InkSecondary,
                focusedTrailingIconColor = MistBlueText,
                unfocusedTrailingIconColor = MistBlueText
            )
        )
    }
}

/** 主按钮：樱花粉横向渐变胶囊，深玫瑰文字保证对比度 */
@Composable
fun PrimaryPinkButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 10.dp,
                shape = ButtonShape,
                ambientColor = SakuraPink.copy(alpha = 0.45f),
                spotColor = SakuraPink.copy(alpha = 0.45f)
            )
            .clip(ButtonShape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(SakuraPink, SakuraPinkDeep))
                } else {
                    SolidColor(SakuraPinkLight)
                }
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = RoseDeep,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text,
                color = if (enabled) RoseDeep else InkSecondary.copy(alpha = 0.6f),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }
    }
}

/** 错误横幅：极浅粉底 + 玫瑰红文字，点击可关闭 */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SakuraPinkLight)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(RoseError.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = RoseError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(message, color = RoseError, fontSize = 13.sp)
    }
}

/** 底部引导链接：灰玫瑰前缀 + 雾霾蓝动作 */
@Composable
fun FooterLink(prefix: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(prefix, color = InkSecondary, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            action,
            color = MistBlueText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

/** 安全码展示框：等宽字体 + 一键复制 */
@Composable
fun SecretValueBox(uuid: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FieldBg)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            uuid,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = InkPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "复制",
            color = RoseDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable {
                clipboard.setText(AnnotatedString(uuid))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ---------------- 预览 ----------------

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFFFF9FB)
@Composable
private fun GradientHeaderPreview() {
    com.example.album.ui.theme.AlbumTheme {
        GradientHeader(title = "安坐", subtitle = "把温柔的日子，一张一张收好")
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFFFFF9FB)
@Composable
private fun AuthWidgetsPreview() {
    com.example.album.ui.theme.AlbumTheme {
        Column(Modifier.padding(24.dp)) {
            var phone by remember { mutableStateOf("") }
            SoftTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "手机号",
                placeholder = "11 位手机号"
            )
            Spacer(Modifier.height(14.dp))
            PrimaryPinkButton(text = "登 录", onClick = {})
            Spacer(Modifier.height(14.dp))
            ErrorBanner(message = "手机号、密码或安全码错误")
            Spacer(Modifier.height(14.dp))
            SecretValueBox("e2779600-507b-4ec5-9963-2da7d9b1ee9d")
        }
    }
}
