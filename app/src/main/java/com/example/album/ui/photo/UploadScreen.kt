package com.example.album.ui.photo

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.album.data.util.ImageStore
import com.example.album.ui.auth.AuthCard
import com.example.album.ui.auth.ErrorBanner
import com.example.album.ui.auth.GradientHeader
import com.example.album.ui.auth.PrimaryPinkButton
import com.example.album.ui.theme.AlbumTheme
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.MistBlueLight
import com.example.album.ui.theme.MistBlueText

@Composable
fun UploadScreen(vm: PhotoViewModel) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PhotoViewModel.MAX_BATCH)
    ) { uris -> if (uris.isNotEmpty()) vm.onPicked(uris) }

    LaunchedEffect(vm.toastMessage) {
        vm.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    val launchPicker = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    UploadContent(
        photos = vm.pickedPhotos,
        uploading = vm.uploading,
        uploadStage = vm.uploadStage,
        uploadError = vm.uploadError,
        onPick = launchPicker,
        onRemove = { vm.removePicked(it) },
        onClear = { vm.clearPicked() },
        onUpload = { vm.startUpload() },
        onDismissError = { vm.clearUploadError() }
    )
}

/** 无状态层：多选照片网格 + 批量上传，便于预览与测试 */
@Composable
fun UploadContent(
    photos: List<ImageStore.PickedMeta>,
    uploading: Boolean,
    uploadStage: String,
    uploadError: String?,
    onPick: () -> Unit,
    onRemove: (ImageStore.PickedMeta) -> Unit,
    onClear: () -> Unit,
    onUpload: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "上传照片",
            subtitle = "一次最多挑 10 张，一起收好",
            height = 210.dp
        )

        Column(
            Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                if (photos.isEmpty()) {
                    EmptyPickerArea(onPick)
                } else {
                    PickedSummary(photos, uploading = uploading, onClear = onClear)
                    Spacer(Modifier.height(12.dp))
                    PickedGrid(photos, uploading = uploading, onRemove = onRemove)
                    Spacer(Modifier.height(12.dp))
                    AddMoreRow(
                        remaining = PhotoViewModel.MAX_BATCH - photos.size,
                        enabled = !uploading,
                        onClick = onPick
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryPinkButton(
                        text = if (uploading) uploadStage else "开始上传（${photos.size} 张）",
                        enabled = !uploading,
                        loading = uploading,
                        onClick = onUpload
                    )
                }

                uploadError?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = it, onDismiss = onDismissError)
                }
            }
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun EmptyPickerArea(onPick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MistBlueLight)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CloudUpload,
                contentDescription = null,
                tint = MistBlueText,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("点击选择照片", fontSize = 15.sp, color = MistBlueText)
            Spacer(Modifier.height(4.dp))
            Text(
                "支持一次多选，最多 ${PhotoViewModel.MAX_BATCH} 张 · 每月上限 500MB",
                fontSize = 12.sp,
                color = InkSecondary
            )
        }
    }
}

@Composable
private fun PickedSummary(
    photos: List<ImageStore.PickedMeta>,
    uploading: Boolean,
    onClear: () -> Unit
) {
    val totalBytes = photos.sumOf { it.size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "已选 ${photos.size}/${PhotoViewModel.MAX_BATCH} 张",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "共 ${ImageStore.formatSize(totalBytes)}",
                fontSize = 12.sp,
                color = InkSecondary
            )
        }
        Text(
            "清空",
            fontSize = 14.sp,
            color = if (uploading) InkSecondary.copy(alpha = 0.5f) else MistBlueText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(enabled = !uploading, onClick = onClear)
                .padding(6.dp)
        )
    }
}

/** 固定三列网格：最多 10 张，直接按行排布，避免与外层滚动冲突 */
@Composable
private fun PickedGrid(
    photos: List<ImageStore.PickedMeta>,
    uploading: Boolean,
    onRemove: (ImageStore.PickedMeta) -> Unit
) {
    photos.chunked(3).forEachIndexed { rowIndex, row ->
        if (rowIndex > 0) Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { meta ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MistBlueLight)
                ) {
                    AsyncImage(
                        model = meta.uri,
                        contentDescription = meta.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (!uploading) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { onRemove(meta) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "移除",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
            // 不足三张时补齐占位，保持网格对齐
            repeat(3 - row.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AddMoreRow(remaining: Int, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MistBlueLight.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled && remaining > 0, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.AddPhotoAlternate,
            contentDescription = null,
            tint = if (remaining > 0) MistBlueText else InkSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            if (remaining > 0) "添加更多（还可选 $remaining 张）" else "已选满 10 张",
            fontSize = 13.sp,
            color = if (remaining > 0) MistBlueText else InkSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

// ---------------- 预览 ----------------

private fun previewMeta(index: Int) = ImageStore.PickedMeta(
    uri = Uri.EMPTY,
    name = "IMG_2026$index.jpg",
    size = 2_048_000L,
    width = 1080,
    height = 1920,
    mimeType = "image/jpeg"
)

@Preview(showBackground = true, showSystemUi = true, name = "未选择")
@Composable
private fun UploadEmptyPreview() {
    AlbumTheme {
        UploadContent(
            photos = emptyList(), uploading = false, uploadStage = "", uploadError = null,
            onPick = {}, onRemove = {}, onClear = {}, onUpload = {}, onDismissError = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "已选多张")
@Composable
private fun UploadPickedPreview() {
    AlbumTheme {
        UploadContent(
            photos = listOf(previewMeta(1), previewMeta(2), previewMeta(3), previewMeta(4)),
            uploading = false, uploadStage = "", uploadError = null,
            onPick = {}, onRemove = {}, onClear = {}, onUpload = {}, onDismissError = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "上传中")
@Composable
private fun UploadingPreview() {
    AlbumTheme {
        UploadContent(
            photos = listOf(previewMeta(1), previewMeta(2), previewMeta(3)),
            uploading = true, uploadStage = "正在上传 2/3…", uploadError = null,
            onPick = {}, onRemove = {}, onClear = {}, onUpload = {}, onDismissError = {}
        )
    }
}
