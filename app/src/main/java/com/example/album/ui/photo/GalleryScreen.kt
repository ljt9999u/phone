package com.example.album.ui.photo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.album.data.model.PhotoItem
import com.example.album.ui.theme.AlbumTheme
import com.example.album.ui.theme.CardWhite
import com.example.album.ui.theme.CreamBg
import com.example.album.ui.theme.InkPrimary
import com.example.album.ui.theme.InkSecondary
import com.example.album.ui.theme.MistBlueLight
import com.example.album.ui.theme.MistBlueText
import com.example.album.ui.theme.RoseDeep
import com.example.album.ui.theme.SakuraPink
import com.example.album.ui.theme.SakuraPinkLight
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun GalleryScreen(vm: PhotoViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.loadGalleryIfNeeded() }
    LaunchedEffect(vm.toastMessage) {
        vm.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    GalleryContent(
        photos = vm.photos,
        total = vm.total,
        loading = vm.galleryLoading,
        error = vm.galleryError,
        loadingMore = vm.loadingMore,
        downloading = vm.downloading,
        selectionMode = vm.selectionMode,
        selectedIds = vm.selectedIds,
        batchDeleting = vm.batchDeleting,
        onRefresh = { vm.refreshGallery() },
        onLoadMore = { vm.loadMore() },
        onDownload = { vm.download(it) },
        onDelete = { vm.delete(it) },
        onEnterSelection = { vm.enterSelection() },
        onExitSelection = { vm.exitSelection() },
        onToggleSelect = { vm.toggleSelect(it) },
        onToggleSelectAll = { vm.toggleSelectAll() },
        onBatchDelete = { vm.deleteSelected() }
    )
}

/** 无状态层：便于预览与测试 */
@Composable
fun GalleryContent(
    photos: List<PhotoItem>,
    total: Long,
    loading: Boolean,
    error: String?,
    loadingMore: Boolean,
    downloading: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onDownload: (PhotoItem) -> Unit,
    onDelete: (PhotoItem) -> Unit,
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    batchDeleting: Boolean = false,
    onEnterSelection: () -> Unit = {},
    onExitSelection: () -> Unit = {},
    onToggleSelect: (Long) -> Unit = {},
    onToggleSelectAll: () -> Unit = {},
    onBatchDelete: () -> Unit = {}
) {
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

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
            .collect { if (it) onLoadMore() }
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
                    if (selectionMode) "已选 ${selectedIds.size} 张" else "我的照片",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    if (selectionMode) "轻点照片可取消选择" else "共 $total 张",
                    fontSize = 12.sp,
                    color = InkSecondary
                )
            }
            if (photos.isNotEmpty()) {
                Text(
                    if (selectionMode) "取消" else "选择",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MistBlueText,
                    modifier = Modifier
                        .clickable {
                            if (selectionMode) onExitSelection() else onEnterSelection()
                        }
                        .padding(6.dp)
                )
            }
            if (!selectionMode) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = MistBlueText)
                }
            }
        }

        if (selectionMode) {
            SelectionBar(
                allSelected = photos.isNotEmpty() && selectedIds.size == photos.size,
                selectedCount = selectedIds.size,
                deleting = batchDeleting,
                onToggleSelectAll = onToggleSelectAll,
                onDelete = { confirmDelete = true }
            )
        }

        when {
            loading && photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SakuraPink)
                }
            }
            error != null && photos.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error, color = InkSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "点击重试",
                            color = MistBlueText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onRefresh() }
                                .padding(6.dp)
                        )
                    }
                }
            }
            photos.isEmpty() -> {
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
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        val selected = photo.id in selectedIds
                        Box(
                            Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MistBlueLight)
                                .clickable {
                                    if (selectionMode) {
                                        onToggleSelect(photo.id)
                                    } else {
                                        viewerIndex = photos.indexOfFirst { it.id == photo.id }
                                            .takeIf { it >= 0 }
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = photo.photoName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (selectionMode) {
                                if (!selected) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = 0.28f))
                                    )
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) SakuraPink else Color.White.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "已选择",
                                            tint = RoseDeep,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (loadingMore) {
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

    if (photos.isNotEmpty()) {
        viewerIndex?.let { index ->
            PhotoViewerDialog(
                photos = photos,
                initialIndex = index.coerceIn(0, photos.lastIndex),
                downloading = downloading,
                onDownload = { onDownload(it) },
                onDelete = { onDelete(it) },
                onClose = { viewerIndex = null }
            )
        }
    }

    if (confirmDelete) {
        BatchDeleteDialog(
            count = selectedIds.size,
            onConfirm = {
                confirmDelete = false
                onBatchDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** 选择模式下的操作条：全选 + 批量删除 */
@Composable
private fun SelectionBar(
    allSelected: Boolean,
    selectedCount: Int,
    deleting: Boolean,
    onToggleSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (allSelected) "取消全选" else "全选",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MistBlueText,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !deleting, onClick = onToggleSelectAll)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (selectedCount > 0 && !deleting) SakuraPink else SakuraPinkLight)
                .clickable(enabled = selectedCount > 0 && !deleting, onClick = onDelete)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = RoseDeep,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(6.dp))
                Text("删除中…", fontSize = 13.sp, color = RoseDeep, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = if (selectedCount > 0) RoseDeep else InkSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "删除 ${if (selectedCount > 0) "$selectedCount 张" else ""}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedCount > 0) RoseDeep else InkSecondary
                )
            }
        }
    }
}

/** 批量删除确认弹窗 */
@Composable
private fun BatchDeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardWhite)
                .padding(24.dp)
        ) {
            Text("删除照片", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "将删除选中的 $count 张照片，云端文件会一并移除，且无法恢复。",
                fontSize = 13.sp,
                color = InkSecondary
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(MistBlueLight)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 15.sp, color = MistBlueText, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(SakuraPink)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text("删除", fontSize = 15.sp, color = RoseDeep, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PhotoViewerDialog(
    photos: List<PhotoItem>,
    initialIndex: Int,
    downloading: Boolean,
    onDownload: (PhotoItem) -> Unit,
    onDelete: (PhotoItem) -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
        val current = photos.getOrNull(pagerState.currentPage)

        // 双指缩放状态：放大后禁用翻页，交给拖拽平移
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var viewport by remember { mutableStateOf(IntSize.Zero) }

        LaunchedEffect(pagerState.currentPage) {
            scale = 1f
            offset = Offset.Zero
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // 左右滑动切换上一张 / 下一张
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale <= 1f
            ) { page ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewport = it }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.5f
                                        offset = Offset.Zero
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            // 双指缩放 + 放大后单指拖动；未放大时不拦截事件，保证翻页
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.count { it.pressed }
                                    if (pressed >= 2) {
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (zoomChange != 1f || panChange != Offset.Zero) {
                                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                            scale = newScale
                                            offset = if (newScale > 1f) {
                                                clampOffset(offset + panChange, viewport, newScale)
                                            } else {
                                                Offset.Zero
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (pressed == 1 && scale > 1f) {
                                        val panChange = event.calculatePan()
                                        if (panChange != Offset.Zero) {
                                            offset = clampOffset(offset + panChange, viewport, scale)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photos[page].url,
                        contentDescription = photos[page].photoName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 只保留悬浮图标按钮，不显示页码与图片信息，保证沉浸观感
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }

            current?.let { photo ->
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    CircleAction(
                        contentDescription = if (downloading) "下载中" else "下载",
                        enabled = !downloading,
                        onClick = { onDownload(photo) }
                    ) {
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    CircleAction(
                        contentDescription = "删除",
                        enabled = true,
                        onClick = { onDelete(photo) }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFFB4C4),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 查看器底部半透明圆形操作按钮 */
@Composable
private fun CircleAction(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) { icon() }
}

/** 限制放大后的平移范围，避免图片被拖出可视区域 */
private fun clampOffset(offset: Offset, viewport: IntSize, scale: Float): Offset {
    if (scale <= 1f || viewport == IntSize.Zero) return Offset.Zero
    val maxX = viewport.width * (scale - 1f) / 2f
    val maxY = viewport.height * (scale - 1f) / 2f
    return Offset(
        offset.x.coerceIn(-maxX, maxX),
        offset.y.coerceIn(-maxY, maxY)
    )
}

// ---------------- 预览 ----------------

private fun previewPhoto(id: Long, name: String) = PhotoItem(
    id = id,
    photoName = name,
    fileSize = 204800,
    width = 1080,
    height = 1920,
    uploadTime = "2026-10-14T23:00:00",
    url = ""
)

private val previewPhotos = listOf(
    previewPhoto(1, "樱花树下.jpg"),
    previewPhoto(2, "海边日落.jpg"),
    previewPhoto(3, "猫咪午睡.jpg"),
    previewPhoto(4, "山顶云海.jpg"),
    previewPhoto(5, "街角咖啡.jpg"),
    previewPhoto(6, "夜空烟火.jpg")
)

@Preview(showBackground = true, showSystemUi = true, name = "网格")
@Composable
private fun GalleryGridPreview() {
    AlbumTheme {
        GalleryContent(
            photos = previewPhotos, total = 6, loading = false, error = null,
            loadingMore = true, downloading = false,
            onRefresh = {}, onLoadMore = {}, onDownload = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "选择模式")
@Composable
private fun GallerySelectionPreview() {
    AlbumTheme {
        GalleryContent(
            photos = previewPhotos, total = 6, loading = false, error = null,
            loadingMore = false, downloading = false,
            selectionMode = true, selectedIds = setOf(1L, 3L, 5L),
            onRefresh = {}, onLoadMore = {}, onDownload = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "空态")
@Composable
private fun GalleryEmptyPreview() {
    AlbumTheme {
        GalleryContent(
            photos = emptyList(), total = 0, loading = false, error = null,
            loadingMore = false, downloading = false,
            onRefresh = {}, onLoadMore = {}, onDownload = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "加载中")
@Composable
private fun GalleryLoadingPreview() {
    AlbumTheme {
        GalleryContent(
            photos = emptyList(), total = 0, loading = true, error = null,
            loadingMore = false, downloading = false,
            onRefresh = {}, onLoadMore = {}, onDownload = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "错误态")
@Composable
private fun GalleryErrorPreview() {
    AlbumTheme {
        GalleryContent(
            photos = emptyList(), total = 0, loading = false,
            error = "网络异常，请稍后重试",
            loadingMore = false, downloading = false,
            onRefresh = {}, onLoadMore = {}, onDownload = {}, onDelete = {}
        )
    }
}
