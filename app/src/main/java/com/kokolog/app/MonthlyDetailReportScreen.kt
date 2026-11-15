package com.kokolog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role
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
private val DetailSectionBg = Color(0xFFEEE4D6)
private val DetailContentBg = Color(0xFFFFFCF7)
private val DetailInsightBg = Color(0xFFE4EFE6)
private val DetailInsightInk = Color(0xFF365D43)
private val DetailActionBg = Color(0xFFD1E2D3)
private val DetailActionBorder = Color(0xFF9FB9A5)
private val DetailActionInk = Color(0xFF315841)
private val DetailNoteBg = Color(0xFFF2EDF5)
private val DetailNoteInk = Color(0xFF584965)
private val DetailBeige = DetailSectionBg
private val DetailMint = DetailInsightBg
private val DetailMuted = Color(0xFF695D51)
private val DetailInk = Color(0xFF453D35)
private val DetailDividerColor = Color(0xFFD7C9B7)

/**
 * STEP4.3-B: 月次詳細レポート（Debug / 内部テスト用プレビュー）。
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
    var selectedEvidenceIds by remember { mutableStateOf<List<Long>?>(null) }
    var showAfterHeartDialog by remember { mutableStateOf(false) }
    val changeStories = remember(year, month, logsSnapshot, changesSnapshot) {
        MonthlyAfterHeartPresentation.stories(logsSnapshot, changesSnapshot, report.period)
    }
    val ignoredRecords = remember(year, month, logsSnapshot) {
        MonthlyAfterHeartPresentation.ignoredLogs(logsSnapshot, report.period)
    }
    val overview = MonthlyDetailNarrative.overview(report)
    val summary = report.summary
    val details = report.details
    val showDetailedSections = MonthlyDetailNarrative.shouldShowDetailedSections(report)
    val crossFindings = remember(year, month, logsSnapshot) {
        MonthlyDetailCrossAnalysis.findings(logsSnapshot, report.period)
    }
    val heartAlbums = remember(year, month, logsSnapshot) {
        MonthlyDetailExperience.albums(logsSnapshot, report.period)
    }
    val gentleHints = remember(year, month, logsSnapshot, crossFindings, report.readiness.canOfferMonthlyDetail) {
        MonthlyDetailExperience.hints(
            logsSnapshot, report.period, crossFindings, report.readiness.canOfferMonthlyDetail
        )
    }

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
                "今月は、記録をそのまま振り返るためのレポートです。（テスト表示）",
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
                        DetailActionRow("この発見の記録を見る") {
                            selectedEvidenceIds = insight.evidenceLogIds
                        }
                    }
                }
                DetailFootnote("記録に見られた特徴です。感情の原因を推定したものではありません。")
            }
        }

        // 強い感情と繰り返し残されたテーマを、自分の言葉とともに読み返す。
        // アルバムは販売対象外の月でもプレビューで確認できるが、購入可能判定には使用しない。
        if (heartAlbums.isNotEmpty()) {
            DetailCard(DetailSectionBg) {
                DetailHeading("今月のココロアルバム", "📚")
                DetailFootnote("記録の中から、振り返りやすい形でココロを集めました。感情に良い・悪いの点数は付けていません。")
                heartAlbums.forEachIndexed { index, album ->
                    if (index > 0) DetailDivider()
                    Text(album.title, color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    DetailBody(album.description)
                    val albumEntries = album.evidenceLogIds.mapNotNull { albumId ->
                        logsSnapshot.firstOrNull { it.id == albumId }
                    }
                    albumEntries.take(3).forEach { log ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DetailContentBg)
                                .clickable(role = Role.Button) { onOpenLog(log.id) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${SimpleDateFormat("M/d", Locale.JAPAN).format(Date(log.createdAt))}  ${emotionIcon(log.emotion)} ${log.emotion}",
                                color = DetailInk, fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            val meta = listOfNotNull(log.category.takeIf { it.isNotBlank() },
                                MonthlyDetailRecordPreview.intensity(log.intensity)).joinToString("  ・  ")
                            if (meta.isNotBlank()) Text(meta, color = DetailMuted, fontSize = 12.sp)
                            MonthlyDetailRecordPreview.tags(log.tags).takeIf { it.isNotBlank() }?.let {
                                Text(it, color = Color(0xFF526A55), fontSize = 12.sp)
                            }
                            MonthlyDetailRecordPreview.memo(log.memo)?.let {
                                Text(it, color = DetailInk, fontSize = 13.sp, lineHeight = 19.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp)).background(DetailActionBg)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("このココロを開く", color = DetailActionInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("›", color = DetailActionInk, fontSize = 19.sp)
                            }
                        }
                    }
                    if (album.totalCount > 3) {
                        Text("ほか${album.totalCount - 3}件のココロがあります。", color = DetailMuted, fontSize = 12.sp)
                    }
                    DetailActionRow("このアルバムの記録をすべて見る（${album.totalCount}件）") {
                        selectedEvidenceIds = album.evidenceLogIds
                    }
                }
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
                DetailNote(MonthlyDetailNarrative.intensityText(top))
            }
            DetailInsight(report, { selectedEvidenceIds = it.evidenceLogIds }, ReportInsightKind.INTENSITY)
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
            DetailInsight(report, { selectedEvidenceIds = it.evidenceLogIds }, ReportInsightKind.WEEKDAY, ReportInsightKind.TIME_SLOT)
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
            if (crossFindings.isNotEmpty()) {
                DetailDivider()
                Text("組み合わせて見つけたこと", color = DetailInk,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                crossFindings.forEach { finding ->
                    DetailInsightCard {
                        Text("${if (finding.source == CrossSource.TAG) "#" else ""}${finding.groupName}｜${finding.title}",
                            color = DetailInsightInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(finding.message, color = DetailInsightInk, fontSize = 14.sp, lineHeight = 22.sp)
                        if (finding.evidenceLogIds.isNotEmpty()) {
                            DetailActionRow("このテーマの記録を見る") {
                                selectedEvidenceIds = finding.evidenceLogIds
                            }
                        }
                    }
                }
                DetailFootnote("同じテーマで一緒に記録された気持ちを並べています。出来事と感情の因果関係を示すものではありません。")
            }
            DetailInsight(report, { selectedEvidenceIds = it.evidenceLogIds }, ReportInsightKind.CATEGORY, ReportInsightKind.TAG)
        }

        DetailSection(4, "前月のココロと比べる", "先月から、記録された感情の割合はどう変わった？") {
            val comparison = details.comparison
            val previousMonth = periodMonth.minusMonths(1)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailMetric("${previousMonth.monthValue}月", "${comparison.previousTotalCount}件", Modifier.weight(1f))
                DetailMetric("${month}月", "${comparison.currentTotalCount}件", Modifier.weight(1f))
            }
            DetailNote(MonthlyDetailNarrative.comparisonText(report))
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
            DetailInsight(report, { selectedEvidenceIds = it.evidenceLogIds }, ReportInsightKind.PERIOD_COMPARISON)
        }

        } // showDetailedSections: 4つの分析セクションは、提供できる発見がある月だけ展開。

        // STEP4.3-D: 感情の「その後」を元の記録とともに時系列で振り返る。
        // 記録数が少ない月でも、事実として存在する履歴なら表示する。
        if (changeStories.isNotEmpty()) {
            DetailCard(DetailSectionBg) {
                DetailHeading("その後のココロ", "🌿")
                val eventCount = changeStories.sumOf { it.events.size }
                DetailCard(DetailContentBg) {
                    DetailBody("その後を残した記録 ${changeStories.size}件・変化の記録 ${eventCount}回")
                    changeStories.take(2).forEach { story ->
                        val last = story.events.last()
                        Text(
                            "${emotionIcon(story.original.emotion)} ${story.original.emotion} → ${emotionIcon(last.emotion)} ${last.emotion}",
                            color = DetailInk, fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    DetailFootnote("記録された気持ちの経過です。回復や改善を意味するものではありません。")
                    DetailActionRow("変化の経過を読み返す（${changeStories.size}件）") {
                        showAfterHeartDialog = true
                    }
                }
            }
        }
        // 「気にしない」は感情の変化とは異なる設定なので、別枠にする。
        if (ignoredRecords.isNotEmpty()) {
            DetailCard(DetailSectionBg) {
                DetailHeading("気にしないにしたココロ", "🍃")
                DetailCard(DetailContentBg) {
                    DetailBody("この月は${ignoredRecords.size}件のココロを「気にしない」にしました。")
                    DetailFootnote("設定した記録をそのまま振り返れます。気持ちが変わったと判断するものではありません。")
                    DetailActionRow("気にしないにした記録を見る（${ignoredRecords.size}件）") {
                        selectedEvidenceIds = ignoredRecords.map { it.id }
                    }
                }
            }
        }

        // 行動の指示ではなく、記録を基にした任意の問いを届ける。
        // 特徴の根拠がない月は、無理に「パーソナルな助言」を生成しない。
        if (gentleHints.isNotEmpty()) {
            DetailCard(DetailMint) {
                DetailHeading("来月の小さなヒント", "🌱")
                DetailFootnote("今月の記録を手掛かりにした、振り返りの提案です。何かを変える必要はありません。")
                gentleHints.forEachIndexed { index, hint ->
                    if (index > 0) DetailDivider()
                    Text(hint.title, color = Color(0xFF405847), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    DetailBody(hint.observation)
                    DetailNote(hint.question, DetailPaper)
                    DetailActionRow("ヒントの元になった記録を見る") {
                        selectedEvidenceIds = hint.evidenceLogIds
                    }
                }
                DetailFootnote("記録上の関連をもとにしています。出来事が感情の原因だと判断したり、未来を予測したりするものではありません。")
            }
        }

        DetailCard(DetailBeige) {
            DetailHeading("今月のココロを振り返って", "📖")
            MonthlyDetailNarrative.closing(report).forEach { DetailBody(it) }
            Text("またいつでも、ここに戻ってきてください。", color = DetailMuted,
                fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Text("あなたのココロを、そっと置いておく場所。", color = DetailMuted, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), textAlign = TextAlign.Center)
    }

    if (showAfterHeartDialog) {
        AlertDialog(
            onDismissRequest = { showAfterHeartDialog = false },
            containerColor = DetailPaper,
            title = { Text("その後のココロ", color = DetailInk, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailFootnote("最初のココロから、その後に記録した気持ちを順番に並べました。")
                    changeStories.forEach { story ->
                        DetailCard(DetailSectionBg) {
                            Text(
                                "${SimpleDateFormat("M/d HH:mm", Locale.JAPAN).format(Date(story.original.createdAt))}  ${emotionIcon(story.original.emotion)} ${story.original.emotion}",
                                color = DetailInk, fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                            DetailFootnote("強さ ${story.original.intensity}/5 ・ ${story.original.category}")
                            if (story.original.memo.isNotBlank()) DetailBody(story.original.memo)
                            story.events.forEach { change ->
                                DetailDivider()
                                Text(
                                    "↓  ${SimpleDateFormat("M/d HH:mm", Locale.JAPAN).format(Date(change.createdAt))}  ${emotionIcon(change.emotion)} ${change.emotion}",
                                    color = DetailInk, fontSize = 14.sp, fontWeight = FontWeight.Bold
                                )
                                DetailFootnote("強さ ${change.intensity}/5")
                                if (change.memo.isNotBlank()) DetailBody(change.memo)
                            }
                            DetailActionRow("元のココロを開く") {
                                showAfterHeartDialog = false
                                onOpenLog(story.original.id)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAfterHeartDialog = false }) { Text("閉じる", color = DetailInk) }
            }
        )
    }

    selectedEvidenceIds?.let { ids ->
        val evidence = logsSnapshot.filter { it.id > 0 && it.id in ids }
            .distinctBy { it.id }.sortedByDescending { it.createdAt }
        AlertDialog(
            onDismissRequest = { selectedEvidenceIds = null },
            containerColor = DetailPaper,
            title = { Text("関連するココロ", color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("この発見の根拠となった記録です。", color = DetailMuted, fontSize = 13.sp)
                    if (evidence.isEmpty()) {
                        DetailBody("読み返せる記録がありません。")
                    } else {
                        evidence.forEachIndexed { index, log ->
                            if (index > 0) DetailDivider()
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(DetailSectionBg)
                                    .clickable(role = Role.Button) { selectedEvidenceIds = null; onOpenLog(log.id) }
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("${SimpleDateFormat("M/d", Locale.JAPAN).format(Date(log.createdAt))}  ${emotionIcon(log.emotion)} ${log.emotion}",
                                        color = DetailInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("記録を開く ›", color = DetailActionInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                val info = listOfNotNull(
                                    "カテゴリー：${log.category}",
                                    MonthlyDetailRecordPreview.intensity(log.intensity)
                                ).joinToString("  ・  ")
                                Text(info, color = DetailMuted, fontSize = 12.sp)
                                MonthlyDetailRecordPreview.tags(log.tags).takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = Color(0xFF526A55), fontSize = 12.sp)
                                }
                                MonthlyDetailRecordPreview.memo(log.memo)?.let {
                                    Text(it, color = DetailInk, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEvidenceIds = null }) { Text("閉じる", color = DetailInk) }
            }
        )
    }
}

/** セクションとカードのデザインを統一し、今後分析項目を追加しやすくする。 */
@Composable
private fun DetailSection(number: Int, title: String, subtitle: String,
                          content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    DetailCard(DetailSectionBg) {
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
        DetailCard(DetailContentBg, content)
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
    Box(Modifier.fillMaxWidth().height(1.dp).background(DetailDividerColor))
}

@Composable private fun DetailNote(text: String, color: Color = DetailNoteBg) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color).padding(12.dp)) {
        Text(text, color = if (color == DetailNoteBg) DetailNoteInk else DetailInk,
            fontSize = 14.sp, lineHeight = 22.sp)
    }
}

/** 気づきと数値の事実を明確に区別する共通カード。 */
@Composable
private fun DetailInsightCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    DetailCard(DetailInsightBg, content)
}

/** 行全体が押せる、詳細レポート内の共通操作部品。 */
@Composable
private fun DetailActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DetailActionBg)
            .border(1.dp, DetailActionBorder, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DetailActionInk, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("›", color = DetailActionInk, fontSize = 22.sp)
    }
}

@Composable private fun DetailMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(DetailContentBg).padding(13.dp),
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
    // 冒頭の「今月の発見」と同じ文章をここで繰り返さない。
    if (insight in report.featuredInsights) return
    DetailInsightCard {
        Text("✨ 記録から見えてきたこと", color = DetailInsightInk,
            fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(insight.message, color = DetailInsightInk, fontSize = 14.sp, lineHeight = 22.sp)
        if (insight.evidenceLogIds.any { it > 0 }) {
            DetailActionRow("関連する記録を見る") { onSelect(insight) }
        }
    }
}
