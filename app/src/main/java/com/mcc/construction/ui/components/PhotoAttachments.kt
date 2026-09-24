package com.mcc.construction.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.mcc.construction.R
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing

/** One attached photo: a stable [key] (for removal) and the image [uri]. */
data class PhotoAttachment(val key: String, val uri: String)

/**
 * A row of photo thumbnails with an "Add photo" tile — proof of work on a task,
 * pictures on a daily report or incident. Each thumbnail has a 48dp remove
 * button. The row scrolls sideways when it outgrows the screen.
 *
 * The row only displays; opening the picker is the caller's job, usually with
 * [rememberPhotoPicker], so the screen itself stays stateless.
 */
@Composable
fun PhotoAttachmentRow(
    photos: List<PhotoAttachment>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: ((PhotoAttachment) -> Unit)?,
    modifier: Modifier = Modifier,
    maxPhotos: Int = DEFAULT_MAX_PHOTOS,
    enabled: Boolean = true
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            photos.forEachIndexed { index, photo ->
                PhotoThumbnail(
                    photo = photo,
                    position = index + 1,
                    onRemove = if (enabled && onRemovePhoto != null) ({ onRemovePhoto(photo) }) else null
                )
            }
            if (enabled && photos.size < maxPhotos) {
                AddPhotoTile(onClick = onAddPhoto)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = pluralStringResource(R.plurals.photos_attached, photos.size, photos.size, maxPhotos),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PhotoThumbnail(photo: PhotoAttachment, position: Int, onRemove: (() -> Unit)?) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        SubcomposeAsyncImage(
            model = photo.uri,
            contentDescription = stringResource(R.string.photo_attached_description, position),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = {
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.xl)
                )
            }
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.photo_remove, position),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier
            .size(THUMBNAIL_SIZE)
            .clip(shape)
            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.AddAPhoto,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.photo_add),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Opens the Android photo picker, which needs no storage permission. Returns
 * a function to call from an "Add photo" button; the picked content URIs go
 * to [onPicked].
 *
 * @param maxItems how many photos one pick may return. 1 opens the
 *   single-image picker.
 */
@Composable
fun rememberPhotoPicker(maxItems: Int, onPicked: (List<Uri>) -> Unit): () -> Unit {
    val currentOnPicked = rememberUpdatedState(onPicked)
    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    if (maxItems <= 1) {
        val single = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) currentOnPicked.value(listOf(uri))
        }
        return { single.launch(request) }
    }
    val multiple = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems)
    ) { uris -> if (uris.isNotEmpty()) currentOnPicked.value(uris) }
    return { multiple.launch(request) }
}

private val THUMBNAIL_SIZE = 96.dp
private const val DEFAULT_MAX_PHOTOS = 5

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PhotoAttachmentRowPreview() {
    PreviewSurface {
        PhotoAttachmentRow(
            photos = listOf(PhotoAttachment("1", ""), PhotoAttachment("2", "")),
            onAddPhoto = {},
            onRemovePhoto = {},
            modifier = Modifier.padding(Spacing.lg)
        )
    }
}
