package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.BookEntity
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.TvColors
import java.io.File
import kotlin.math.abs

@Composable
fun TvActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1.0f,
        animationSpec = spring(),
        label = "btn_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> TvColors.SurfaceVariant.copy(alpha = 0.5f)
            filled && focused -> TvColors.PrimaryBright
            filled -> TvColors.Primary
            focused -> TvColors.FocusContainer
            else -> TvColors.Surface
        },
        label = "btn_bg"
    )
    val borderColor = if (focused) TvColors.FocusBorder else {
        if (filled) Color.Transparent else TvColors.BorderSubtle
    }

    Surface(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (filled) Color.White else TvColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                if (text.isNotEmpty()) Spacer(Modifier.width(8.dp))
            }
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (filled) Color.White else TvColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TvSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    usePrimaryActiveColor: Boolean = false,
    tag: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1.0f,
        animationSpec = spring(),
        label = "chip_scale"
    )
    val bg = when {
        selected && usePrimaryActiveColor -> TvColors.Primary
        selected -> TvColors.ActiveChipContainer
        focused -> TvColors.FocusContainer
        else -> TvColors.Surface
    }
    val borderColor = when {
        focused -> TvColors.FocusBorder
        selected -> Color.Transparent
        else -> TvColors.BorderSubtle
    }

    Surface(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else TvColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else TvColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    containerColor: Color = TvColors.Surface,
    padding: PaddingValues = PaddingValues(14.dp),
    tag: String? = null,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.02f else 1.0f,
        animationSpec = spring(),
        label = "card_scale"
    )
    val border = when {
        focused -> BorderStroke(2.dp, TvColors.FocusBorder)
        selected -> BorderStroke(1.5.dp, TvColors.PrimaryBright)
        else -> BorderStroke(1.dp, TvColors.BorderSubtle)
    }
    val bg = when {
        selected -> TvColors.FocusContainer.copy(alpha = 0.7f)
        focused -> TvColors.SurfaceVariant
        else -> containerColor
    }

    Surface(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = border
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun BookCoverArt(
    book: BookEntity,
    modifier: Modifier = Modifier
) {
    val coverExists = remember(book.coverPath) {
        book.coverPath?.let { File(it).exists() } == true
    }

    Box(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, TvColors.BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        if (coverExists) {
            AsyncImage(
                model = File(book.coverPath!!),
                contentDescription = "${book.title} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val palettePair = remember(book.title) {
                val palettes = listOf(
                    Color(0xFF1A3A5C) to Color(0xFF0E1D30),
                    Color(0xFF3E2A47) to Color(0xFF1F1524),
                    Color(0xFF1E4035) to Color(0xFF10221C),
                    Color(0xFF4A3122) to Color(0xFF241811),
                    Color(0xFF2A324B) to Color(0xFF151926)
                )
                palettes[abs(book.title.hashCode()) % palettes.size]
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(palettePair.first, palettePair.second)))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.format,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = book.title,
                        fontFamily = LiterataFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5EFE6),
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
