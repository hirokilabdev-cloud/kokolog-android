package com.kokolog.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * STEP 2〜4.2 : 振り返りの入口。初期表示は「カレンダー」。
 *
 * 従来のフィルター付き本棚は ReflectionCategoryContent() に残す。
 * SaveableStateProvider によって、表示を切り替えても従来フィルターの選択状態を保持する。
 * Room・記録データ・既存の詳細画面には変更を加えない。
 */
@Composable
fun ReflectionScreen(
    logs: List<EmotionLog>,
    onLogClick: (EmotionLog) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf("calendar") }
    val stateHolder = rememberSaveableStateHolder()

    Column(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KokologHeader(title = "振り返り")

        // カレンダーは初期表示。カテゴリーへ切り替えると旧来の本棚UIがそのまま開く。
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("calendar" to "カレンダー", "category" to "カテゴリー").forEach { (key, label) ->
                val selected = selectedTab == key
                Button(
                    onClick = { selectedTab = key },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFFD9C6AF) else Color(0xFFF3ECE3),
                        contentColor = KokologTextColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == "calendar") {
                stateHolder.SaveableStateProvider("calendar_view") {
                    ReflectionCalendarContent(
                        logs = logs,
                        onLogClick = onLogClick
                    )
                }
            } else {
                stateHolder.SaveableStateProvider("category_view") {
                    ReflectionCategoryContent(
                        logs = logs,
                        onLogClick = onLogClick
                    )
                }
            }
        }
    }
}

/**
 * STEP 4.2：記録日を淡い緑＋件数で可視化し、日付タップで内訳を表示する。
 * 「すべて」または日別カテゴリー押下で本棚を開く（直接は開かない）。
 * 月別カテゴリーからの本棚表示は従来どおり維持する。
 * 表示する月・選択した日付は画面復帰後も保持する。
 * 記録日は端末のローカルタイムゾーンで判定する。
 * createdAt は Unix epoch milliseconds (Long) を想定。
 */
@Composable
private fun ReflectionCalendarContent(
    logs: List<EmotionLog>,
    onLogClick: (EmotionLog) -> Unit
) {
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    // 選択日と本棚の表示状態を分離。日付タップだけでは本棚へ遷移しない。
    // day_all: その日の全記録 / day_category: その日×カテゴリー
    // month_category: 表示中の月×カテゴリー
    var bookshelfMode by rememberSaveable { mutableStateOf<String?>(null) }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthKey)
    val today = LocalDate.now()
    val zoneId = ZoneId.systemDefault()

    // 表示中の月だけを抽出。日付の判定は端末のローカルタイムゾーンを使う。
    // 保存済みログが編集・削除された場合も最新の一覧から再集計する。
    val logsInMonth = logs.filter { log ->
        val localDate = Instant.ofEpochMilli(log.createdAt).atZone(zoneId).toLocalDate()
        YearMonth.from(localDate) == month
    }
    val dayCounts = mutableMapOf<Int, Int>()
    val categoryCounts = mutableMapOf<String, Int>()
    logsInMonth.forEach { log ->
        val day = Instant.ofEpochMilli(log.createdAt).atZone(zoneId).dayOfMonth
        dayCounts[day] = (dayCounts[day] ?: 0) + 1
        val category = log.category.ifBlank { "その他" }
        categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
    }
    val monthCount = logsInMonth.size

    // 日付選択時に、月内の該当日ログを抽出する。
    // この選択だけでは画面遷移せず、カレンダー下部の内訳を更新する。
    val selectedDayLogs = selectedDay?.let { selected ->
        logsInMonth.filter { log ->
            Instant.ofEpochMilli(log.createdAt).atZone(zoneId).dayOfMonth == selected
        }
    }.orEmpty()
    val dayCategoryCounts = selectedDayLogs.groupingBy { log ->
        log.category.ifBlank { "その他" }
    }.eachCount()

    // 本棚を開いているときだけシステムの戻る操作を受け取り、日付の選択は保持する。
    // これにより「本棚 → カレンダーで当日の内訳確認」が自然につながる。
    BackHandler(enabled = bookshelfMode != null) {
        bookshelfMode = null
        selectedCategory = null
    }

    if (bookshelfMode != null) {
        val selectedLogs = when (bookshelfMode) {
            "day_all" -> selectedDayLogs
            "day_category" -> selectedDayLogs.filter { log ->
                log.category.ifBlank { "その他" } == selectedCategory
            }
            "month_category" -> logsInMonth.filter { log ->
                log.category.ifBlank { "その他" } == selectedCategory
            }
            else -> emptyList()
        }
        val shelfTitle = when (bookshelfMode) {
            "day_all" -> "${month.year}年${month.monthValue}月${selectedDay}日のココロ"
            "day_category" -> "${month.year}年${month.monthValue}月${selectedDay}日・${selectedCategory}のココロ"
            else -> "${month.year}年${month.monthValue}月・${selectedCategory}のココロ"
        }
        CalendarFilteredBookshelf(
            title = shelfTitle,
            logs = selectedLogs,
            onBack = {
                bookshelfMode = null
                selectedCategory = null
            },
            onLogClick = onLogClick
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
                // ＜ ＞ で前月・翌月へ。月が変わったら選択日のハイライトを解除する。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        monthKey = month.minusMonths(1).toString()
                        selectedDay = null
                        selectedCategory = null
                    }) {
                        Text("〈", color = KokologTextColor, fontSize = 20.sp)
                    }
                    // 年月表示自体を押すと、任意の年月へ移動する選択ダイアログを開く。
                    // 前月・翌月ボタンは近い月への移動用として引き続き残す。
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { showMonthPicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${month.year}年${month.monthValue}月 ⌄",
                            color = KokologTextColor,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "この月のココロ ${monthCount}件",
                            color = KokologSubTextColor,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = {
                        monthKey = month.plusMonths(1).toString()
                        selectedDay = null
                        selectedCategory = null
                    }) {
                        Text("〉", color = KokologTextColor, fontSize = 20.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                val weekdays = listOf("日", "月", "火", "水", "木", "金", "土")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEachIndexed { index, dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (index) {
                                0 -> Color(0xFFAD716A)
                                6 -> Color(0xFF6E88A5)
                                else -> KokologSubTextColor
                            }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                // ISO曜日は月=1 ... 日=7。日曜始まりの列番号へ変換する。
                val leadingBlanks = month.atDay(1).dayOfWeek.value % 7
                val allCells: List<Int?> =
                    List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { it }
                val trailingBlanks = (7 - allCells.size % 7) % 7
                val paddedCells = allCells + List(trailingBlanks) { null }

                // STEP 4.2：カレンダーの状態は色で見分ける。
                // 記録あり＝淡い緑、選択中＝少し濃い緑＋緑枠、今日＝茶系の細枠。
                // 記録のない日は色を付けず、「未記録」を責める印象にしない。
                val recordedColor = Color(0xFFE5EEE0)
                val selectedColor = Color(0xFFC3D5BE)
                val selectedOutlineColor = Color(0xFF638A69)
                val countTextColor = Color(0xFF497451)
                val todayOutlineColor = Color(0xFFB8A898)

                paddedCells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val count = if (day == null) 0 else (dayCounts[day] ?: 0)
                            val hasRecords = count > 0
                            val isToday = day != null && month.year == today.year &&
                                month.monthValue == today.monthValue && day == today.dayOfMonth
                            val isSelected = day != null && selectedDay == day
                            val shape = RoundedCornerShape(11.dp)

                            // 選択中の色を最優先。今日と選択日が重なる場合も選択枠を優先。
                            val cellColor = when {
                                isSelected -> selectedColor
                                hasRecords -> recordedColor
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(shape)
                                    .background(cellColor)
                                    .then(
                                        when {
                                            isSelected -> Modifier.border(1.5.dp, selectedOutlineColor, shape)
                                            isToday -> Modifier.border(1.dp, todayOutlineColor, shape)
                                            else -> Modifier
                                        }
                                    )
                                    .clickable(enabled = day != null, role = Role.Button) {
                                        // 同じ日をもう一度押すと選択解除する。
                                        selectedDay = if (selectedDay == day) null else day
                                        selectedCategory = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (hasRecords || isToday || isSelected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            color = KokologTextColor
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        if (hasRecords) {
                                            // 点3つでは情報量が分からないため、実際の記録件数を表示。
                                            Text(
                                                text = "${count}件",
                                                fontSize = 10.sp,
                                                lineHeight = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = countTextColor,
                                                maxLines = 1
                                            )
                                        } else {
                                            // 0件の日も日付が上下に動かないように高さを確保。
                                            Spacer(Modifier.height(11.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 控えめな凡例。色の意味が初見でも分かるようにする。
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CalendarDayLegend(color = recordedColor, outline = null, label = "記録あり")
                    Spacer(Modifier.width(12.dp))
                    CalendarDayLegend(color = selectedColor, outline = selectedOutlineColor, label = "選択中")
                    Spacer(Modifier.width(12.dp))
                    CalendarDayLegend(color = Color.Transparent, outline = todayOutlineColor, label = "今日")
                }
            }
        }

        // 日付押下時の「ワンクッション」。カレンダーと月別集計の間に表示する。
        // 日付→件数・内訳を確認→本棚という2段階にし、意図せぬ画面遷移を防ぐ。
        selectedDay?.let { day ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3ECE3))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${month.monthValue}月${day}日のココロ",
                                color = KokologTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        TextButton(onClick = { selectedDay = null }) {
                            Text("閉じる", color = KokologSubTextColor, fontSize = 12.sp)
                        }
                    }

                    if (selectedDayLogs.isEmpty()) {
                        Text(
                            text = "この日はまだココロが記録されていません",
                            color = KokologSubTextColor,
                            fontSize = 13.sp
                        )
                    } else {
                        // 「すべて」を押すとその日の全カテゴリーを既存本棚で一覧表示する。
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFFBF4))
                                .clickable(role = Role.Button) {
                                    selectedCategory = null
                                    bookshelfMode = "day_all"
                                }
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("すべてのココロ", color = KokologTextColor, fontSize = 13.sp)
                            Text(
                                text = "${selectedDayLogs.size}件 ›",
                                color = KokologTextColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "カテゴリー別の内訳",
                            fontSize = 12.sp,
                            color = KokologSubTextColor
                        )
                        CalendarCategoryGrid(dayCategoryCounts) { category ->
                            // その日かつ指定カテゴリーのみを本棚に表示する。
                            selectedCategory = category
                            bookshelfMode = "day_category"
                        }
                    }
                }
            }
        }

        // 表示中の月のカテゴリー別件数。0件カテゴリーは表示しない。
        // 各カテゴリーを押すと、表示中の月で絞った本棚へ移動する。
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF4))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${month.monthValue}月のカテゴリー",
                        color = KokologTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "合計 ${monthCount}件",
                        color = KokologSubTextColor,
                        fontSize = 12.sp
                    )
                }

                if (categoryCounts.isEmpty()) {
                    Text(
                        text = "この月はまだ記録がありません",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = KokologSubTextColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    CalendarCategoryGrid(categoryCounts) { category ->
                        // 月別カテゴリは月全体が対象。選択日のハイライトは維持する。
                        selectedCategory = category
                        bookshelfMode = "month_category"
                    }
                }
            }
        }

    }

    if (showMonthPicker) {
        YearMonthPickerDialog(
            currentMonth = month,
            onDismiss = { showMonthPicker = false },
            onMonthSelected = { picked ->
                monthKey = picked.toString()
                selectedDay = null  // 年月変更では日付・カテゴリー選択を解除。
                selectedCategory = null
                bookshelfMode = null
                showMonthPicker = false
            }
        )
    }
}



/**
 * カレンダーの色の意味を示すコンパクトな凡例。
 * 日付セルと同じ配色で、記録日・選択中・今日の違いを伝える。
 */
@Composable
private fun CalendarDayLegend(color: Color, outline: Color?, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(shape)
                .background(color)
                .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = KokologSubTextColor)
    }
}

/**
 * 日別・月別で共用するカテゴリー件数グリッド。
 * 記録画面と同じカテゴリー順で並べ、未知のカテゴリーも末尾に表示する。
 */
@Composable
private fun CalendarCategoryGrid(
    counts: Map<String, Int>,
    onCategoryClick: (String) -> Unit
) {
    val defaultCategories = listOf("仕事", "人間関係", "家族", "健康", "学校", "お金", "趣味", "その他")
    val visibleCategories = defaultCategories.filter { (counts[it] ?: 0) > 0 } +
        counts.keys.filter { it !in defaultCategories && (counts[it] ?: 0) > 0 }.sorted()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visibleCategories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    Row(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFFBF4))
                            .clickable(role = Role.Button) { onCategoryClick(category) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = KokologTextColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${counts[category] ?: 0}件 ›",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KokologTextColor
                        )
                    }
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * STEP 4.1：カレンダー内の集計から開く、条件で絞り込まれた本棚画面。
 *
 * 新しいナビゲーションルートは作らずに同じ振り返り画面内で切り替える。
 * これにより本棚 → 詳細 → 戻るで年月・カテゴリー選択を保持できる。
 */
@Composable
private fun CalendarFilteredBookshelf(
    title: String,
    logs: List<EmotionLog>,
    onBack: () -> Unit,
    onLogClick: (EmotionLog) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("〈", fontSize = 22.sp, color = KokologTextColor)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = KokologTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2
                )
                Text(
                    text = "${logs.size}件のココロ",
                    color = KokologSubTextColor,
                    fontSize = 12.sp
                )
            }
        }

        // STEP 1の本棚共通部品を使用。背表紙の横並び・詳細への遷移は既存と同じ。
        // 記録0件の場合もBookshelfListの既存空状態UIを使用する。
        Box(modifier = Modifier.weight(1f)) {
            BookshelfList(logs = logs, onLogClick = onLogClick)
        }
    }
}


/**
 * 年月の直接選択ダイアログ。
 * 年は直接入力（西暦1〜9999年）も、左右ボタンでの移動も可能。
 * 月は12個からタップして選べるので、過去の年月へすばやくジャンプできる。
 */
@Composable
private fun YearMonthPickerDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onMonthSelected: (YearMonth) -> Unit
) {
    var yearText by androidx.compose.runtime.remember(currentMonth) {
        mutableStateOf(currentMonth.year.toString())
    }
    var pickedMonth by androidx.compose.runtime.remember(currentMonth) {
        mutableStateOf(currentMonth.monthValue)
    }
    val inputYear = yearText.toIntOrNull()
    val validYear = inputYear != null && inputYear in 1..9999

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFBF4),
        shape = RoundedCornerShape(22.dp),
        title = {
            Text("年月を選択", color = KokologTextColor, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 年：左右で1年移動、中央の数字を押せば直接入力可能。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = {
                        val year = (inputYear ?: currentMonth.year)
                        if (year > 1) yearText = (year - 1).toString()
                    }) {
                        Text("〈", color = KokologTextColor, fontSize = 18.sp)
                    }
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { value ->
                            yearText = value.filter { it.isDigit() }.take(4)
                        },
                        modifier = Modifier.width(125.dp),
                        singleLine = true,
                        label = { Text("西暦（年）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !validYear
                    )
                    IconButton(onClick = {
                        val year = (inputYear ?: currentMonth.year)
                        if (year < 9999) yearText = (year + 1).toString()
                    }) {
                        Text("〉", color = KokologTextColor, fontSize = 18.sp)
                    }
                }
                if (!validYear) {
                    Text("1〜9999の西暦を入力してください", color = Color(0xFFAD716A), fontSize = 12.sp)
                }

                // 月：選択状態が分かるように色を変える（3列 × 4行）。
                (1..12).chunked(3).forEach { months ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        months.forEach { option ->
                            val selected = option == pickedMonth
                            Button(
                                onClick = { pickedMonth = option },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFFD9C6AF) else Color(0xFFF3ECE3),
                                    contentColor = KokologTextColor
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("${option}月", fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validYear) onMonthSelected(YearMonth.of(inputYear!!, pickedMonth))
                },
                enabled = validYear
            ) {
                Text("移動", color = if (validYear) KokologTextColor else KokologSubTextColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", color = KokologTextColor)
            }
        }
    )
}
