package com.runninghub.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.Dimens

@Composable
fun AppCard(
    title: String,
    imageUrl: String?,
    authorName: String?,
    authorAvatar: String?,
    likeCount: String,
    useCount: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.RadiusMD),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSM)
    ) {
        Column {
            SmartAsyncImage(
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            Column(modifier = Modifier.padding(Dimens.SpaceMD)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SmartAsyncImage(
                        imageUrl = authorAvatar,
                        contentDescription = authorName,
                        modifier = Modifier
                            .size(Dimens.AvatarSizeSM)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(Dimens.SpaceSM))
                    Text(
                        text = authorName ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${useCount}次使用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
