package com.example.album.ui.photo

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.album.data.model.PhotoItem
import com.example.album.data.util.ImageStore
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.MistBlueLight
import com.example.album.ui.theme.MistBlueText
import com.example.album.ui.theme.SakuraPink
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun GalleryScreen(vm: PhotoViewModel) {
    val context = LocalContext.current
    var viewer by remember { mutableStateOf<PhotoItem?>(null) }

    LaunchedEffect(Unit) { vm.loadGalleryIfNeeded() }
    LaunchedEffect(vm.toastMessage) {
        vm.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { if (it) vm.loadMore() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
    ) {
        // 顶部标题栏（蓝色点缀）
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "我的照片",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    "共 ${vm.total} 张",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }
            IconButton(onClick = { vm.refreshGallery() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = MistBlueText)
            }
        }

        when {
            vm.galleryLoading && vm.photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SakuraPink)
                }
            }
            vm.galleryError != null && vm.photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(vm.galleryError!!, color = InkSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "点击重试",
                            color = MistBlueText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { vm.refreshGallery() }
                                .padding(6.dp)
                        )
                    }
                }
            }
            vm.photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = MistBlueText.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("还没有照片，去上传第一张吧", color = InkSecondary, fontSize = 14.sp)
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vm.photos, key = { it.id }) { photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.photoName,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MistBlueLight)
                                .clickable { viewer = photo },
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (vm.loadingMore) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = SakuraPink,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    viewer?.let { photo ->
        PhotoViewerDialog(
            photo = photo,
            downloading = vm.downloading,
            onDownload = { vm.download(photo) },
            onDelete = {
                vm.delete(photo)
                viewer = null
            },
            onClose = { viewer = null }
        )
    }
}

@Composable
private fun PhotoViewerDialog(
    photo: PhotoItem,
    downloading: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            AsyncImage(
                model = photo.url,
                contentDescription = photo.photoName,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(photo.photoName, color = Color.White, fontSize = 14.sp, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ImageStore.formatSize(photo.fileSize)} · " +
                        "${photo.width}×${photo.height} · ${ImageStore.formatTime(photo.uploadTime)}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    ViewerAction(
                        icon = { Icon(Icons.Filled.Download, null, tint = Color.White) },
                        label = if (downloading) "下载中…" else "下载",
                        onClick = onDownload
                    )
                    ViewerAction(
                        icon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFFFB4C4)) },
                        label = "删除",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
