package com.selma.autoflix.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selma.autoflix.R
import com.selma.autoflix.ui.theme.DarkSlate
import com.selma.autoflix.ui.theme.Terracotta

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_logo_ribbon),
        contentDescription = "AutoFlix Logo",
        modifier = modifier.size(size)
    )
}

@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    onCarModeClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandLogo(size = 38.dp)
            Column {
                Text(
                    text = "آتوفلیکس",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkSlate,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = "AutoFlix Car & TV",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
