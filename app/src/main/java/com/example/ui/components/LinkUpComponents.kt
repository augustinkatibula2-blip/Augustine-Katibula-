package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.CompensationType
import com.example.domain.FormatUtils
import com.example.domain.VerificationTier
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun LinkUpLogo(
    modifier: Modifier = Modifier,
    darkBackground: Boolean = false,
    showTagline: Boolean = false,
    iconSize: Dp = 28.dp
) {
    val useLightText = darkBackground || MaterialTheme.linkUpColors.isDark
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = if (useLightText) ElectricBlue else NavyPrimary,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.24f, h * 0.24f)
            )
            val path = Path().apply {
                moveTo(w * 0.28f, h * 0.28f)
                lineTo(w * 0.28f, h * 0.68f)
                lineTo(w * 0.56f, h * 0.68f)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = w * 0.13f, cap = StrokeCap.Round)
            )
            drawLine(
                color = Color(0xFF93C5FD),
                start = Offset(w * 0.48f, h * 0.50f),
                end = Offset(w * 0.72f, h * 0.28f),
                strokeWidth = w * 0.10f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = w * 0.10f,
                center = Offset(w * 0.72f, h * 0.28f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.08f,
                center = Offset(w * 0.72f, h * 0.68f)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Link",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = if (useLightText) Color.White else NavyPrimary
                )
                Text(
                    text = "Up",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = if (useLightText) ElectricBlueBright else ElectricBlue
                )
            }
            if (showTagline) {
                Text(
                    text = "Connect. Collaborate. Build.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (useLightText) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InitialsOrPhotoAvatar(
    fullName: String,
    photoUri: String?,
    lowDataMode: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val initials = fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "LU" }

    val palette = listOf(
        NavyPrimary,
        ElectricBlueDark,
        Color(0xFF0F766E),
        Color(0xFF1E3A8A),
        Color(0xFF334155)
    )
    val bgColor = palette[abs(fullName.hashCode()) % palette.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!lowDataMode && !photoUri.isNullOrBlank()) {
            AsyncImage(
                model = Uri.parse(photoUri),
                contentDescription = "Profile photo of $fullName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.36f).sp
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillTagRow(
    skillsCsv: String,
    modifier: Modifier = Modifier,
    maxTags: Int = 5,
    highlightAccent: Boolean = false
) {
    val tags = FormatUtils.parseTags(skillsCsv, maxTags)
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                color = if (highlightAccent) MaterialTheme.linkUpColors.accentContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlightAccent) MaterialTheme.linkUpColors.onAccentContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CompatibilityScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val ext = MaterialTheme.linkUpColors
    val (bg, fg) = when {
        score >= 80 -> ext.successBg to ext.successFg
        score >= 60 -> ext.accentContainer to ext.onAccentContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$score% Match",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg
        )
    }
}

@Composable
fun VerificationBadgeChip(statusCode: String, modifier: Modifier = Modifier) {
    val tier = VerificationTier.fromCode(statusCode)
    if (tier == VerificationTier.UNVERIFIED) return
    val ext = MaterialTheme.linkUpColors
    Surface(
        modifier = modifier,
        color = ext.successBg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = tier.label,
                tint = ext.successFg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = tier.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = ext.successFg
            )
        }
    }
}

@Composable
fun CompensationBadge(typeCode: String, modifier: Modifier = Modifier) {
    val type = CompensationType.fromCode(typeCode)
    val ext = MaterialTheme.linkUpColors
    val (bg, fg) = when (type) {
        CompensationType.PAID_POSITION, CompensationType.PAID_FREELANCE -> ext.successBg to ext.successFg
        CompensationType.EQUITY_BASED -> ext.accentContainer to ext.onAccentContainer
        CompensationType.UNPAID_INTERNSHIP, CompensationType.VOLUNTEER -> ext.warningBg to ext.warningFg
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = type.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Rounded.Inbox,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
