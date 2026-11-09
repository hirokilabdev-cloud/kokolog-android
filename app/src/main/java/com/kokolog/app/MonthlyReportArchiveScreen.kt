package com.kokolog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * STEP3：過去の月次レポート一覧。
 * - 記録のある「確定済みの月」を新しい順に表示する。
 * - 記録そのものは一切変更しない。
 * - 閲覧状態はMainActivityから渡され、端末へ永続化される。
 * - 記録日時は他画面と同じく端末ローカルタイムゾーンで扱う。
 */
@Composable
fun MonthlyReportArchiveScreen(
    logs: List<EmotionLog>,
    readMonthKeys: Set<String>,
    onBack: () -> Unit,
    onReportClick: (Int, Int) -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val latestClosedMonth = YearMonth.now().minusMonths(1)
    val monthlyCounts = logs.groupingBy { log ->
        YearMonth.from(Instant.ofEpochMilli(log.createdAt).atZone(zoneId))
    }.eachCount().filterKeys { it <= latestClosedMonth }
        .toList().sortedByDescending { it.first }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFFFAF7EF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("〈", color = KokologTextColor, fontSize = 23.sp)
            }
            Text("過去の月次レポート", color = KokologTextColor,
                fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "これまで残したココロを、月ごとに振り返れます。",
            color = KokologSubTextColor,
            fontSize = 14.sp
        )
        if (monthlyCounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))
            ) {
                Text("まだ確定済みの月次レポートはありません。",
                    modifier = Modifier.padding(16.dp),
                    color = KokologSubTextColor, fontSize = 14.sp)
            }
        }
        monthlyCounts.forEach { (month, count) ->
            val isUnread = month.toString() !in readMonthKeys
            Card(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                    onReportClick(month.year, month.monthValue)
                },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnread) Color(0xFFE7EFE6) else Color(0xFFFFFCF7)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("📖 ${month.year}年${month.monthValue}月のココロ",
                            color = KokologTextColor, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold)
                        Text("残したココロ ${count}件",
                            color = KokologSubTextColor, fontSize = 13.sp)
                    }
                    if (isUnread) {
                        Text("NEW", color = Color(0xFF48634D),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(9.dp))
                    }
                    Text("〉", color = KokologTextColor, fontSize = 21.sp)
                }
            }
        }
        Text("記録がない月は一覧に表示されません。レポート画面の矢印から閲覧できます。",
            color = KokologSubTextColor, fontSize = 12.sp)
    }
}
