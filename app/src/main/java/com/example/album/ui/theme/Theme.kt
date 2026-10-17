package com.example.album.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 安坐品牌配色：樱花粉 + 雾霾蓝 + 奶白，按 60/30/10 比例使用。
 * 品牌色固定，不随系统动态取色，也不做暗色变体（相册类工具保持恒定温柔观感）。
 */
private val AlbumColorScheme = lightColorScheme(
    primary = SakuraPink,
    onPrimary = RoseDeep,
    primaryContainer = SakuraPinkLight,
    onPrimaryContainer = RoseDeep,
    secondary = MistBlue,
    onSecondary = Color(0xFF33586E),
    secondaryContainer = MistBlueLight,
    onSecondaryContainer = Color(0xFF33586E),
    tertiary = MistBlueText,
    background = CreamBg,
    onBackground = InkPrimary,
    surface = CardWhite,
    onSurface = InkPrimary,
    surfaceVariant = FieldBg,
    onSurfaceVariant = InkSecondary,
    error = RoseError,
    onError = Color.White,
    outline = FieldBorder,
    outlineVariant = FieldBorder
)

@Composable
fun AlbumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlbumColorScheme,
        typography = Typography,
        content = content
    )
}
