package com.schultetable.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScoreHistoryDialog(
    scores: List<ScoreRecord>,
    gridSize: Int,
    gameMode: GameMode,
    useColors: Boolean,
    onDismiss: () -> Unit
) {
    val modeDescription = "${gridSize}×${gridSize} ${gameMode.displayName} ${if (useColors) Strings.COLORFUL else Strings.MONOCHROME}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Strings.SCORE_HISTORY,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = modeDescription,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (scores.isEmpty()) {
                    Text(
                        text = Strings.NO_SCORES,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Strings.RANK,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Strings.TIME,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Strings.DATE,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(2f)
                                )
                            }
                            HorizontalDivider()
                        }

                        itemsIndexed(scores) { index, record ->
                            ScoreItem(index = index + 1, record = record)
                            if (index < scores.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(Strings.CLOSE)
            }
        }
    )
}

@Composable
private fun ScoreItem(index: Int, record: ScoreRecord) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (index) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "$index"
            },
            fontSize = 14.sp,
            fontWeight = if (index <= 3) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "%.2f 秒".format(record.elapsedTime),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when (index) {
                1 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = dateStr,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f)
        )
    }
}