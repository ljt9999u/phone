package com.example.album.ui.photo

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.album.ui.auth.AuthCard
import com.example.album.ui.auth.ErrorBanner
import com.example.album.ui.auth.GradientHeader
import com.example.album.ui.auth.PrimaryPinkButton
import com.example.album.data.util.ImageStore
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
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.onPicked(it) } }

    LaunchedEffect(vm.toastMessage) {
        vm.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(title = "上传照片", subtitle = "把这一刻，轻轻放进相册", height = 210.dp)

        Column(
            Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = 24.dp)
        ) {
            AuthCard {
                val meta = vm.picked
                if (meta == null) {
                    // 选择区
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MistBlueLight)
                            .clickable {
                                picker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
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
                                "每月上传上限 500MB",
                                fontSize = 12.sp,
                                color = InkSecondary
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = meta.uri,
                        contentDescription = meta.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        meta.name,
                        fontSize = 14.sp,
                        color = InkPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${ImageStore.formatSize(meta.size)} · ${meta.width}×${meta.height}",
                        fontSize = 12.sp,
                        color = InkSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryPinkButton(
                        text = if (vm.uploading) vm.uploadStage else "开始上传",
                        enabled = !vm.uploading,
                        loading = vm.uploading
                    ) { vm.startUpload() }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(
                            "重新选择",
                            fontSize = 14.sp,
                            color = MistBlueText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(enabled = !vm.uploading) {
                                    vm.clearPicked()
                                    picker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(6.dp)
                        )
                        Spacer(Modifier.width(20.dp))
                        Text(
                            "取消",
                            fontSize = 14.sp,
                            color = InkSecondary,
                            modifier = Modifier
                                .clickable(enabled = !vm.uploading) { vm.clearPicked() }
                                .padding(6.dp)
                        )
                    }
                }

                vm.uploadError?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = it, onDismiss = { vm.clearUploadError() })
                }
            }
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UploadEmptyPreview() {
    AlbumTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(CreamBg)
        ) {
            GradientHeader(title = "上传照片", subtitle = "把这一刻，轻轻放进相册", height = 210.dp)
            Column(
                Modifier
                    .offset(y = (-36).dp)
                    .padding(horizontal = 24.dp)
            ) {
                AuthCard {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MistBlueLight),
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
                        }
                    }
                }
            }
        }
    }
}
