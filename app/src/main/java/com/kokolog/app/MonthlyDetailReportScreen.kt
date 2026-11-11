package com.kokolog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

private val DetailPage = Color(0xFFFAF7EF)
private val DetailPaper = Color(0xFFFFFCF7)
private val DetailBeige = Color(0xFFF0E8DC)
private val DetailMint = Color(0xFFE7EFE6)
private val DetailMuted = Color(0xFF7A6A58)
private val DetailInk = Color(0xFF4E463F)

/**
 * STEP4.2: 月次詳細レポート（Debug / 内部テスト用プレビュー）。
 *
 * 注意: 課金/初回無料の権利チェックは未実装。
 *      MainActivityのルート側でビルド種別を再確認し、通常Releaseでは表示しない。
 *      権利管理実装までは、本番公開アプリへ直接つながないこと。
 *
 * - RoomのEntityやDAOは変更せず、ReportAnalyzerの集計結果のみ表示する。
 * - データが少ない月でも集計値は表示するが、傾向を断定しない。
 * - 根拠のあるInsightから元のココロへ遷移できる。
 */
@Composable
fun MonthlyDetailReportScreen(
    year: Int,
    month: Int,
    logs: List<EmotionLog>,
    changeLogs: List<EmotionChangeLog>,
    onBack: () -> Unit,
    onOpenLog: (Long) -> Unit
) {
    val periodMonth = remember(year, month) { YearMonth.of(year, month) }
    // Roomの状態リストはスナップショット化し、記録編集時に再集計する。
    val logsSnapshot = logs.toList()
    val changesSnapshot = changeLogs.toList()
    val report = remember(year, month, logsSnapshot, changesSnapshot) {
        ReportAnalyzer.analyze(logsSnapshot, changesSnapshot, ReportPeriod.month(year, month))
    }
    // Related record dialog: 外部リンク等は使わず、既存の詳細画面へ。
    var selectedInsight by remember { mutableStateOf<ReportInsight?>(null) }
    val overview = MonthlyDetailNarrative.overview(report)
    val summary = report.summary
    val details = report.details
    val showDetailedSections = MonthlyDetailNarrative.shouldShowDetailedSections(report)

    Column(
        modifier = Modifier.fillMaxSize()
            .background(DetailPage)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("〈", color = DetailInk, fontSize = 24.sp) }
            Text("月次詳細レポート", color = DetailInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("テスト用プレビュー・課金は発生しません", color = DetailMuted, fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("📖", fontSize = 25.sp)
            Text("KOKOLOG MONTHLY REPORT", color = DetailMuted, fontSize = 12.sp)
            Text("${month}月のココロ", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("${year}年${month}月1日〜${periodMonth.lengthOfMonth()}日", color = DetailMuted, fontSize = 13.sp)
            Text("あなたのココロを、ゆっくり読み返す時間。", color = DetailMuted, fontSize = 14.sp)
        }

        DetailCard(DetailBeige) {
            DetailHeading("今月のココロのまとめ", "✨")
            overview.forEach { DetailBody(it) }
            DetailFootnote("記録から読み取れる範囲をまとめています。感情の原因や心身の状態を診断するものではありません。")
        }

        // データが少ない月でもテスターは確認できる。販売対象外は短い注記のみ。
        if (!report.readiness.canOfferMonthlyDetail) {
            Text(
                "※この月は有料の詳細分析の提供対象外です（テスト表示）。",
                color = DetailMuted,
                fontSize = 12.sp
            )
        }

        // まとまった発見は最初に最大3件。総括には同じ文章を重複させない。
        if (report.featuredInsights.isNotEmpty()) {
            DetailCard(DetailMint) {
                DetailHeading(MonthlyDetailNarrative.featuredTitle(report), "✨")
                if (!report.readiness.hasSufficientRecords) {
                    DetailFootnote("記録がまだ限られるため、今月の参考情報としてご覧ください。")
                }
                report.featuredInsights.take(3).forEachIndexed { index, insight ->
                    if (index > 0) DetailDivider()
                    Text("${index + 1}. ${insight.title}", color = Color(0xFF405847),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    DetailBody(insight.message)
                    if (insight.evidenceLogIds.any { it > 0 }) {
                        Text("関連するココロを読み返す　〉",
                            modifier = Modifier.fillMaxWidth().clickable { selectedInsight = insight }
                                .padding(vertical = 3.dp),
                            color = Color(0xFF405847), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                DetailFootnote("記録に見られた特徴です。感情の原因を推定したものではありません。")
            }
        }

        // 詳しい比較が難しい月は、0件のグラフを並べず、記録の事実を短く紹介する。
        if (summary.totalCount == 0) {
            DetailCard(DetailMint) {
                DetailHeading("この月はまだ白紙です", "🌱")
                DetailBody("記録がある月を選ぶと、感情の強さやその月に残したココロを振り返れます。")
            }
        } else if (!showDetailedSections) {
            // 少ない月は説明カードを重複させず、内訳と振り返りに集中する。
            DetailCard(DetailPaper) {
                DetailHeading("この月に残したココロ", "📖")
                details.intensity.firstOrNull()?.let { top ->
                    Text("${emotionIcon(top.emotion)} ${top.emotion}の強度", color = DetailInk,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    val presentLevels = MonthlyDetailNarrative.visibleIntensityLevels(top)
                    presentLevels.forEach { (level, count) ->
                        DetailBarRow("強度$level", count, maxOf(1, top.validCount),
                            Color(0xFFB7AAC8), "$count 件")
                    }
                    DetailFootnote("この感情が記録された${top.validCount}件の内訳です。")
                }
                if (details.categories.isNotEmpty()) {
                    DetailDivider()
                    val categories = details.categories.take(3)
                        .joinToString("、") { "${it.name} ${it.totalCount}件" }
                    DetailBody("記録したカテゴリー：$categories")
                }
                if (details.comparison.previousTotalCount == 0) {
                    DetailFootnote("前月には記録がないため、月ごとの感情比較は表示していません。")
                }
            }
        } else {
        DetailSection(1, "感情の強さと変化", "どんな感情が、どのくらい強く残った？") {
            val top = details.intensity.firstOrNull()
            if (top == null) {
                DetailBody("強度を集計できる記録がありません。")
            } else {
                Text("${emotionIcon(top.emotion)} ${top.emotion}の強度分布", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                MonthlyDetailNarrative.visibleIntensityLevels(top).forEach { (level, count) ->
                    DetailBarRow("強度$level", count, maxOf(1, top.validCount), Color(0xFFB7AAC8), "$count 件")
                }
                DetailFootnote("今月記録された強度だけを表示しています。")
                DetailNote(MonthlyDetailNarrative.intensityText(top), Color(0xFFF5F0F7))
            }
            val changes = details.changes
            if (changes.changeEventCount > 0) {
                DetailDivider()
                DetailBody("「その後のココロ」を残した記録：${changes.originalLogsWithChanges}件（変化の記録 ${changes.changeEventCount}件）")
                changes.transitions.firstOrNull()?.let {
                    DetailFootnote("記録された変化の例：${it.fromEmotion} → ${it.toEmotion}（${it.count}回）")
                }
            } else {
                DetailFootnote("この月に集計対象となる「その後のココロ」はありませんでした。")
            }
            DetailInsight(report, { selectedInsight = it }, ReportInsightKind.INTENSITY)
        }

        DetailSection(2, "曜日・時間帯の傾向", "いつココロを残すことが多かった？") {
            Text("記録した曜日", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val weekdayNames = listOf("月", "火", "水", "木", "金", "土", "日")
            val maxWeekday = maxOf(1, details.weekdays.maxOfOrNull { it.totalCount } ?: 0)
            MonthlyDetailNarrative.visibleWeekdays(report).forEach { day ->
                DetailBarRow("${weekdayNames[day.dayOfWeek - 1]}曜", day.totalCount, maxWeekday,
                    Color(0xFFB8A88E), "${day.totalCount}件")
            }
            DetailDivider()
            Text("記録した時間帯", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val maxSlot = maxOf(1, details.timeSlots.maxOfOrNull { it.totalCount } ?: 0)
            MonthlyDetailNarrative.visibleTimeSlots(report).forEach { slot ->
                DetailBarRow(slot.slot.label, slot.totalCount, maxSlot,
                    Color(0xFF9EB5CA), "${slot.totalCount}件")
            }
            DetailFootnote("この月に記録のあった曜日・時間帯だけを表示しています。")
            DetailFootnote("時間はココロに設定した記録日時に基づきます。件数だけで感情の原因を判断することはできません。")
            DetailInsight(report, { selectedInsight = it }, ReportInsightKind.WEEKDAY, ReportInsightKind.TIME_SLOT)
        }

        DetailSection(3, "カテゴリー・タグのつながり", "どんなテーマについて、どんなココロを残した？") {
            Text("カテゴリー別", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (details.categories.isEmpty()) DetailBody("カテゴリーの記録はありません。")
            details.categories.take(4).forEach { group ->
                DetailGroupRow(group, summary.totalCount, tag = false)
            }
            if (details.categories.size > 4) DetailFootnote("このほかに${details.categories.size - 4}カテゴリーあります。")
            DetailDivider()
            Text("記録で使ったタグ", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (details.tags.isEmpty()) {
                DetailBody("今月はタグが登録されていません。")
            } else {
                details.tags.take(4).forEach { DetailGroupRow(it, summary.totalCount, tag = true) }
                if (details.tags.size > 4) DetailFootnote("このほかに${details.tags.size - 4}種類のタグがあります。")
            }
            DetailFootnote("1件の記録に複数タグを付けられるため、タグの件数を合計しても記録総数と一致しないことがあります。")
            DetailInsight(report, { selectedInsight = it }, ReportInsightKind.CATEGORY, ReportInsightKind.TAG)
        }

        DetailSection(4, "前月のココロと比べる", "先月から、記録された感情の割合はどう変わった？") {
            val comparison = details.comparison
            val previousMonth = periodMonth.minusMonths(1)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailMetric("${previousMonth.monthValue}月", "${comparison.previousTotalCount}件", Modifier.weight(1f))
                DetailMetric("${month}月", "${comparison.currentTotalCount}件", Modifier.weight(1f))
            }
            DetailNote(MonthlyDetailNarrative.comparisonText(report), Color(0xFFF5F0F7))
            if (comparison.previousTotalCount >= 10 && comparison.currentTotalCount >= 10) {
                comparison.emotionComparisons.take(3).forEach { emotion ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${emotionIcon(emotion.emotion)} ${emotion.emotion}", color = DetailInk, fontSize = 14.sp)
                        Text("${MonthlyDetailNarrative.percent(emotion.previousShare)} → ${MonthlyDetailNarrative.percent(emotion.currentShare)}",
                            color = DetailMuted, fontSize = 12.sp)
                    }
                }
            }
            DetailFootnote("割合は各月の全記録を分母に計算しています。増減は良し悪しや改善・悪化を表すものではありません。")
            DetailInsight(report, { selectedInsight = it }, ReportInsightKind.PERIOD_COMPARISON)
        }

        } // showDetailedSections: 4つの分析セクションは、提供できる発見がある月だけ展開。

        DetailCard(DetailBeige) {
            DetailHeading("今月のココロを振り返って", "📖")
            MonthlyDetailNarrative.closing(report).forEach { DetailBody(it) }
            Text("またいつでも、ここに戻ってきてください。", color = DetailMuted,
                fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Text("あなたのココロを、そっと置いておく場所。", color = DetailMuted, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)
    }

    selectedInsight?.let { insight ->
        val evidence = logsSnapshot.filter { it.id in insight.evidenceLogIds && it.id > 0 }
            .sortedByDescending { it.createdAt }
        AlertDialog(
            onDismissRequest = { selectedInsight = null },
            containerColor = DetailPaper,
            title = { Text("関連するココロ", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("この発見の根拠になった記録です。", color = DetailMuted, fontSize = 13.sp)
                    if (evidence.isEmpty()) {
                        DetailBody("読み返せる記録がありません。")
                    } else {
                        evidence.take(5).forEach { log ->
                            Text(
                                "${SimpleDateFormat("M/d", Locale.JAPAN).format(Date(log.createdAt))}  ${emotionIcon(log.emotion)} ${log.emotion}　〉",
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedInsight = null; onOpenLog(log.id) }
                                    .padding(vertical = 8.dp),
                                color = DetailInk, fontSize = 14.sp
                            )
                        }
                        if (evidence.size > 5) DetailFootnote("ほかに${evidence.size - 5}件の記録があります。")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedInsight = null }) { Text("閉じる", color = DetailInk) }
            }
        )
    }
}

/** セクションとカードのデザインを統一し、今後分析項目を追加しやすくする。 */
@Composable
private fun DetailSection(number: Int, title: String, subtitle: String,
                          content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFFE8DDD0)).padding(9.dp)
            ) { Text("%02d".format(Locale.ROOT, number), color = Color(0xFF816C57), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = DetailInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = DetailMuted, fontSize = 13.sp)
            }
        }
        DetailCard(DetailPaper, content)
    }
}

@Composable
private fun DetailCard(background: Color,
                       content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = background)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable private fun DetailHeading(title: String, emoji: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(title, color = DetailInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun DetailBody(text: String) {
    Text(text, color = DetailInk, fontSize = 14.sp, lineHeight = 23.sp)
}

@Composable private fun DetailFootnote(text: String) {
    Text(text, color = DetailMuted, fontSize = 13.sp, lineHeight = 21.sp)
}

@Composable private fun DetailDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFECE4D9)))
}

@Composable private fun DetailNote(text: String, color: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color).padding(12.dp)) {
        DetailBody(text)
    }
}

@Composable private fun DetailMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(DetailBeige).padding(13.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = DetailMuted, fontSize = 12.sp)
        Text(value, color = DetailInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** 幅は同じグラフ内の最大値に対する比率で描く。0件は長さ0。 */
@Composable
private fun DetailBarRow(label: String, count: Int, maxCount: Int, tint: Color, right: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = DetailMuted, fontSize = 13.sp, modifier = Modifier.width(64.dp))
        Box(Modifier.weight(1f).height(11.dp).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEEE8DF))) {
            if (count > 0) {
                Box(Modifier.fillMaxWidth(count.toFloat() / maxOf(1, maxCount))
                    .fillMaxHeight().background(tint))
            }
        }
        Text(right, color = DetailInk, fontSize = 13.sp, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun DetailGroupRow(group: ReportGroupBreakdown, total: Int, tag: Boolean) {
    val top = group.emotions.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text((if (tag) "#" else "") + group.name, color = DetailInk, fontSize = 14.sp,
                fontWeight = FontWeight.Medium)
            Text("${group.totalCount}件", color = DetailMuted, fontSize = 13.sp)
        }
        DetailBarRow("全体比", group.totalCount, maxOf(1, total), Color(0xFFBDA78E),
            MonthlyDetailNarrative.percent(group.totalCount.toDouble() / maxOf(1, total)))
        if (group.totalCount >= 5 && top != null) {
            DetailFootnote("この中で多い感情：${emotionIcon(top.name)} ${top.name}（${top.count}/${group.totalCount}件）")
        }
    }
}

/** 数値の説明と根拠記録への入口は、集計エンジンが生成できたときだけ表示。 */
@Composable
private fun DetailInsight(report: ReportResult, onSelect: (ReportInsight) -> Unit,
                          vararg kinds: ReportInsightKind) {
    val insight = MonthlyDetailNarrative.insightFor(report, *kinds) ?: return
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(DetailMint).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("✨ 記録から見えてきたこと", color = Color(0xFF405847),
            fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (insight in report.featuredInsights) {
            DetailFootnote("冒頭で紹介した発見です。上の分析とあわせて振り返れます。")
        } else {
            Text(insight.message, color = Color(0xFF405847), fontSize = 14.sp, lineHeight = 22.sp)
        }
        if (insight.evidenceLogIds.any { it > 0 }) {
            Text("関連するココロを読み返す　〉", modifier = Modifier.clickable { onSelect(insight) },
                color = Color(0xFF405847), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
