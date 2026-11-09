package com.kokolog.app

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

/**
 * KokoLog 月次無料レポート画面 v1.0
 *
 * - 対象月の集計は ReportAnalyzer に一元化する（UI側で集計し直さない）。
 * - Roomの全感情ログを受け取る。「気にしない」の一覧表示設定はレポートに反映しない。
 * - 毎月確定した過去の月のみ閲覧し、画面内の矢印で前後の月を切り替えられる。
 * - 初回無料／購入の操作は未実装。動かない課金ボタンを表示しない。
 *
 * STEP2: 画面表示と実データ集計の結合。
 * STEP3以降: 未読状態、通知、詳細分析、初回無料解放、購入フロー。
 */
@Composable
fun MonthlyReportScreen(
    initialYear: Int,
    initialMonth: Int,
    logs: List<EmotionLog>,
    changeLogs: List<EmotionChangeLog>,
    onMonthViewed: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    // 保存後に戻ってきたときにも選択していた月を維持する。
    var monthKey by rememberSaveable(initialYear, initialMonth) {
        mutableStateOf(YearMonth.of(initialYear, initialMonth).toString())
    }
    val month = remember(monthKey) { YearMonth.parse(monthKey) }
    val latestClosedMonth = YearMonth.now().minusMonths(1)

    // mutableStateListOf を直接 remember のキーにすると、要素更新を検知できない場合がある。
    // スナップショットに変換し、追加・編集・削除後は再集計する。
    val logSnapshot = logs.toList()
    val changeSnapshot = changeLogs.toList()
    val report = remember(monthKey, logSnapshot, changeSnapshot) {
        ReportAnalyzer.analyze(
            logs = logSnapshot,
            changeLogs = changeSnapshot,
            period = ReportPeriod.month(month.year, month.monthValue)
        )
    }
    val summary = report.summary
    val comparison = report.details.comparison

    // 実際に画面を開いた月だけ「閲覧済み」にする。
    // 記録0件の月は通知対象にしないため、閲覧履歴も付けない。
    // 矢印で別の月へ移動した場合も、その月の閲覧だけを記録する。
    LaunchedEffect(monthKey, summary.totalCount) {
        if (summary.totalCount > 0) onMonthViewed(month.year, month.monthValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7EF))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("〈", color = KokologTextColor, fontSize = 24.sp)
            }
            Text(
                text = "月次レポート",
                color = KokologTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("📖", fontSize = 24.sp)
            Text("KOKOLOG MONTHLY REPORT", color = KokologSubTextColor, fontSize = 12.sp)
            Text(
                text = "${month.monthValue}月のココロ",
                fontWeight = FontWeight.Bold,
                color = KokologTextColor,
                fontSize = 24.sp
            )
            Text(
                text = "${month.year}年${month.monthValue}月1日〜${month.lengthOfMonth()}日",
                color = KokologSubTextColor,
                fontSize = 13.sp
            )
            Text(
                text = "この月に残したココロを、そっと振り返ってみましょう。",
                color = KokologSubTextColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // 年月の変更は現在月（未確定）へ進めない。過去月は記録0件でも開ける。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthKey = month.minusMonths(1).toString() }) {
                Text("〈", color = KokologTextColor, fontSize = 22.sp)
            }
            Text(
                "${month.year}年${month.monthValue}月",
                color = KokologTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            IconButton(
                enabled = month < latestClosedMonth,
                onClick = { monthKey = month.plusMonths(1).toString() }
            ) {
                Text(
                    "〉",
                    color = if (month < latestClosedMonth) KokologTextColor else Color(0xFFCAC4BC),
                    fontSize = 22.sp
                )
            }
        }

        ReportPanel(Color(0xFFF0E8DC)) {
            ReportTitle("今月の記録", "📚")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportNumberCard("残したココロ", "${summary.totalCount}件", Modifier.weight(1f))
                ReportNumberCard("記録した日数", "${summary.recordedDays}日", Modifier.weight(1f))
            }
            if (summary.totalCount > 0) {
                if (comparison.previousTotalCount == 0) {
                    ReportSubText("前月には記録がありませんでした。")
                } else {
                    val diff = comparison.totalDifference
                    val changeText = when {
                        diff > 0 -> "前月より${diff}件多く記録されました。"
                        diff < 0 -> "前月より${abs(diff)}件少なく記録されました。"
                        else -> "前月と同じ件数のココロが残されています。"
                    }
                    ReportSubText(changeText)
                }
                if (summary.ignoredCount > 0) {
                    ReportSubText("「気にしない」にしたココロも${summary.ignoredCount}件含んでいます。")
                }
            }
        }

        if (summary.totalCount == 0) {
            // 記録0件では分析プレビューや課金案内を出さない。
            ReportPanel(Color(0xFFFFFCF7)) {
                ReportTitle("この月のココロ", "🌱")
                Text(
                    "この月には、まだココロが残されていません。\n記録した月を選んで、いつでも振り返ってみてください。",
                    color = KokologSubTextColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        } else {
            ReportPanel(Color(0xFFFFFCF7)) {
                ReportTitle("どんなココロが多かった？", "💗")
                val top = summary.emotionCounts.firstOrNull()
                if (top != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emotionIcon(top.name), fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            ReportSubText("多く残された感情")
                            Text(
                                "${top.name}（${top.count}件）",
                                color = KokologTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
                EmotionStackedBar(summary.emotionCounts)
                // 上位4種類を表示。残りは「そのほか」にまとめ、実際の記録数と一致させる。
                val topCounts = summary.emotionCounts.take(4)
                topCounts.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.width(10.dp).height(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(emotionColor(item.name))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${emotionIcon(item.name)} ${item.name}", color = KokologTextColor, fontSize = 14.sp)
                        }
                        ReportSubText("${item.count}件 / ${formatPercent(item.share)}")
                    }
                }
                if (summary.emotionCounts.size > 4) {
                    val others = summary.emotionCounts.drop(4).sumOf { it.count }
                    ReportSubText("そのほかの感情：${others}件")
                }
            }

            ReportPanel(Color(0xFFFFFCF7)) {
                ReportTitle("どんなことを記録した？", "🗂️")
                summary.categoryCounts.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = KokologTextColor, fontSize = 14.sp)
                            ReportSubText("${item.count}件  ${formatPercent(item.share)}")
                        }
                        Box(
                            Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFE9DF))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(item.share.toFloat().coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(Color(0xFFBDA78E))
                            )
                        }
                    }
                }
            }

            ReportPanel(Color(0xFFE7EFE6)) {
                ReportTitle("今月の小さな発見", "✨")
                if (report.freePreview != null) {
                    Text(
                        report.freePreview.message,
                        color = Color(0xFF405847),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Text(
                        "記録された内容から読み取れる範囲の発見です。",
                        color = Color(0xFF647665),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        "この月は、まだ十分な傾向を読み取れませんでした。\nひとつひとつの記録を、そのまま大切に残しておきましょう。",
                        color = Color(0xFF405847),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // 課金前段階の予告。解放処理・購入画面が未実装なので押せないボタンは置かない。
            ReportPanel(Color(0xFFF0E8DC)) {
                ReportTitle("もっと深く、ココロを知る", "📖")
                Text(
                    "記録から、こんな角度でも振り返れる詳細レポートを準備しています。",
                    color = KokologSubTextColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
                listOf(
                    "感情の強さと変化",
                    "曜日・時間帯の傾向",
                    "カテゴリー・タグとの関係",
                    "前月のココロとの比較",
                    "今月のココロの総括"
                ).forEach { label ->
                    Text("🔒 $label", color = KokologTextColor, fontSize = 14.sp)
                }
                Text(
                    if (report.readiness.canOfferMonthlyDetail)
                        "詳細レポートと初回無料体験は、今後のアップデートで利用できるようになります。"
                    else
                        "詳細な傾向を読むためには、もう少し記録が必要です。初回無料体験は今後実装予定です。",
                    color = KokologSubTextColor,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
            }
        }

        Text(
            "あなたのココロを、そっと置いておく場所。",
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            color = KokologSubTextColor,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** 標準のレポートカード。新しい分析を増やす際も同じ余白と背景を使う。 */
@Composable
private fun ReportPanel(
    containerColor: Color,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ReportTitle(label: String, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 17.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KokologTextColor)
    }
}

@Composable
private fun ReportNumberCard(label: String, number: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFCF7)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(number, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = KokologTextColor)
        Text(label, fontSize = 12.sp, color = KokologSubTextColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReportSubText(text: String) {
    Text(text, color = KokologSubTextColor, fontSize = 13.sp, lineHeight = 20.sp)
}

/** 感情別の割合を、数値に比例した横並びのカラーバーで表示する。 */
@Composable
private fun EmotionStackedBar(counts: List<ReportCount>) {
    if (counts.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().height(14.dp)
            .clip(RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        counts.forEach { item ->
            Box(
                Modifier.weight(item.count.toFloat())
                    .fillMaxHeight()
                    .background(emotionColor(item.name))
            )
        }
    }
}

/** ReportAnalyzer の share は0〜1。表示時にパーセントへ変換する。 */
private fun formatPercent(share: Double): String =
    String.format(Locale.JAPAN, "%.1f%%", share * 100.0)
