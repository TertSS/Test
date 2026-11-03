package com.readtracker.lite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.readtracker.lite.data.Calc
import com.readtracker.lite.data.CardStyle
import com.readtracker.lite.data.Format
import com.readtracker.lite.data.Settings
import com.readtracker.lite.data.Status
import com.readtracker.lite.data.Title
import java.io.File

/**
 * Карточка библиотеки (II-6): обложка слева, название/статус в центре, крупное число справа.
 * Три варианта вида; тонкая цветная кромка статуса у левого края.
 */
@Composable
fun TitleCard(
    title: Title,
    settings: Settings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pal = settings.palette
    val mode = settings.theme
    val statusColor = pal.statusColor(title.status).toColor()
    val accent = pal.accent.toColor()
    val cardBg = ThemeColors.card(mode)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(width = 1.dp, color = statusColor.copy(alpha = 0.18f), shape = shape)
            .combinedClickableCompat(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Цветная вертикальная кромка статуса 3 dp
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(statusColor)
        )

        // Обложка 52×74
        CoverBox(
            title = title,
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        )

        // Центр: название, статус, бейджи, закладка/причина дропа
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(
                text = title.name.ifEmpty { "Без названия" },
                color = ThemeColors.onSurface(mode),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (settings.singleLineName) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Точка + текст статуса
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(statusColor)
                )
                if (settings.statusTextInList) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        title.status.ru,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FormatBadge(title.format.short, pal.formatTagColor(title.format).toColor())
                if (title.ongoing && !settings.hideOngoingTag) {
                    FormatBadge("Онг.", pal.tagOngoing.toColor())
                }
            }
            if (settings.bookmarksEnabled && title.bookmark.isNotBlank() &&
                settings.bookmarkPosition == com.readtracker.lite.data.BookmarkPos.BOTTOM
            ) {
                Spacer(Modifier.height(3.dp))
                BookmarkLine(title.bookmark, accent)
            }
            if (title.status == Status.DROPPED && settings.dropReasonInCards && title.dropReason.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    title.dropReason,
                    color = pal.dropped.toColor(),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Справа: блок цифр
        NumbersBlock(
            title = title,
            settings = settings,
            modifier = Modifier.padding(end = 10.dp, top = 10.dp, bottom = 10.dp),
        )

        if (settings.cardChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ThemeColors.grey,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
                    .align(Alignment.CenterVertically),
            )
        }
    }
    // Полоса прогресса для стиля «Цифры · полоса»
    if (settings.cardStyle == CardStyle.BAR) {
        val prog = Calc.progress(title, settings.totalWordsEnabled)
        val frac = prog?.value ?: (if (title.status == Status.COMPLETED) 1f else 0f)
        val barColor = when (prog?.colorKey) {
            Calc.Progress.COLOR_ACCENT -> accent
            Calc.Progress.COLOR_WEB_TAG -> pal.tagWeb.toColor()
            else -> statusColor
        }
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor.copy(alpha = 0.14f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun NumbersBlock(title: Title, settings: Settings, modifier: Modifier = Modifier) {
    val pal = settings.palette
    val accent = pal.accent.toColor()
    val words = Calc.effWords(title).toLong()
    val wordsText = Calc.num(words, settings.abbreviateNumbers)

    val parts = ArrayList<String>()
    val vLabel = Calc.volumesLabel(title)
    if (vLabel.isNotEmpty()) parts.add(vLabel)
    if (title.format == Format.WEB && settings.webChaptersOnCards) {
        parts.add(Calc.chaptersLabel(title))
    }
    val metaLine = parts.joinToString(" · ")
    val ratingText =
        if (settings.ratingEnabled) Calc.ratingLabel(title.rating, settings.ratingScale) else ""
    val bookmarkInline = settings.bookmarksEnabled &&
        settings.bookmarkPosition == com.readtracker.lite.data.BookmarkPos.INLINE &&
        title.bookmark.isNotBlank()

    val tile = settings.cardStyle == CardStyle.TILE

    val inner = Modifier
        .widthIn(min = if (tile) 78.dp else 62.dp, max = if (tile) 112.dp else 108.dp)
        .then(
            if (tile) {
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.11f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            } else Modifier
        )

    Column(
        modifier = modifier.then(inner),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = wordsText,
            color = accent,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
        if (metaLine.isNotEmpty()) {
            Text(
                metaLine,
                color = ThemeColors.grey,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        if (bookmarkInline) {
            BookmarkLine(title.bookmark, accent, small = true)
        }
        if (ratingText.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(ratingText, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookmarkLine(text: String, accent: Color, small: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Book,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(if (small) 10.dp else 12.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text,
            color = accent,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FormatBadge(label: String, color: Color) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Обложка карточки: 52×74, радиус 8; нет картинки — заглушка цвета формата с первой буквой. */
@Composable
fun CoverBox(title: Title, modifier: Modifier = Modifier, w: Int = 52, h: Int = 74) {
    val shape = RoundedCornerShape(8.dp)
    val src = when {
        title.coverUrl.isNotBlank() -> title.coverUrl
        title.coverPath.isNotBlank() && File(title.coverPath).exists() -> File(title.coverPath)
        else -> null
    }
    Box(
        modifier
            .size(width = w.dp, height = h.dp)
            .clip(shape)
            .background(title.coverColor.toColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (src != null) {
            SubcomposeAsyncImage(
                model = src,
                contentDescription = title.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {},
                error = {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        } else {
            // Плитка цвета статуса с первой буквой названия
            Text(
                text = title.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Клик + долгое нажатие (тап — экран/редактор, долгое — удаление с подтверждением). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick, onLongClick = onLongClick)
