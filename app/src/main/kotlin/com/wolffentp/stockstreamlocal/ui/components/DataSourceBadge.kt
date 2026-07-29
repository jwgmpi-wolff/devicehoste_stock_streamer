package com.wolffentp.stockstreamlocal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wolffentp.stockstreamlocal.market.model.DataSourceLabel
import com.wolffentp.stockstreamlocal.ui.theme.*

/**
 * A small colored pill badge that communicates the data origin of a single field.
 * Every quote cell in the table must display one of these badges.
 */
@Composable
fun DataSourceBadge(
    label: DataSourceLabel,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
) {
    val (bg, text) = when (label) {
        DataSourceLabel.LIVE                  -> BadgeLive    to Color.White
        DataSourceLabel.DELAYED               -> BadgeDelayed to Color.White
        DataSourceLabel.STALE                 -> BadgeStale   to Color.White
        DataSourceLabel.IMPORTED_BASELINE     -> BadgeImported to Color.White
        DataSourceLabel.CALCULATED            -> BadgeCalc    to Color.White
        DataSourceLabel.NOT_PROVIDED_BY_SOURCE -> BadgeUnavail to Color.White
        DataSourceLabel.UNSUPPORTED_BY_PROVIDER -> BadgeError to Color.White
        DataSourceLabel.ERROR                 -> BadgeError   to Color.White
        DataSourceLabel.UNAVAILABLE           -> BadgeUnavail to Color.White
    }
    Text(
        text = label.displayText,
        color = text,
        fontSize = 8.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .background(backgroundColor ?: bg, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
