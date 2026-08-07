package com.kokolog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.kokolog.app.ui.theme.HelloWorldTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Book
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import java.util.Calendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(
            "kokolog_settings",
            Context.MODE_PRIVATE
        )
        // Composeの画面描画開始
        setContent {

            HelloWorldTheme {

                val navController = rememberNavController()
                // 現在表示中の画面ルートを取得する
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                // Snackbarを表示するための状態
                val snackbarHostState = remember {
                    SnackbarHostState()
                }
                // Roomデータベースを取得
                val database = remember {
                    AppDatabase.getDatabase(applicationContext)
                }

                // 感情ログ用DAOを取得
                val emotionLogDao = database.emotionLogDao()
                // 感情変化ログ用DAOを取得
                val emotionChangeLogDao = database.emotionChangeLogDao()

                // suspend関数を呼ぶためのCoroutineScope
                val coroutineScope = rememberCoroutineScope()

                // 感情ログ一覧を保持する
                // mutableStateListOf を使うことで、
                // List更新時に画面も自動更新される
                val emotionLogs = remember {
                    mutableStateListOf<EmotionLog>()
                }
                // 感情変化ログ一覧を保持する
                val emotionChangeLogs = remember {
                    mutableStateListOf<EmotionChangeLog>()
                }

                // 最近のココロの表示非表示
                var showRecentLogs by remember {
                    mutableStateOf(
                        prefs.getBoolean("show_recent_logs", false)
                    )
                }
                // 保管後ホームに戻るか否か
                var returnHomeAfterSave by remember {
                    mutableStateOf(
                        prefs.getBoolean("return_home_after_save", true)
                    )
                }
                // 気にしない表示非表示
                var showIgnoredLogs by remember {
                    mutableStateOf(
                        prefs.getBoolean("show_ignored_logs", true)
                    )
                }

                // 通知ON/OFF
                var notificationEnabled by remember {
                    mutableStateOf(
                        prefs.getBoolean("notification_enabled", false)
                    )
                }

                // 通知時刻：時
                var notificationHour by remember {
                    mutableStateOf(
                        prefs.getInt("notification_hour", 21)
                    )
                }

                // 通知時刻：分
                var notificationMinute by remember {
                    mutableStateOf(
                        prefs.getInt("notification_minute", 0)
                    )
                }

                // 初回チュートリアル完了済み
                var tutorialDone by remember {
                    mutableStateOf(
                        prefs.getBoolean("tutorial_done", false)
                    )
                }

                // 編集
                var editingLog by remember {
                    mutableStateOf<EmotionLog?>(null)
                }

                val scope = rememberCoroutineScope()
                fun loadChangeLogs() {
                    scope.launch {
                        val loadedChangeLogs =
                            emotionChangeLogDao.getAllChangeLogs()

                        emotionChangeLogs.clear()
                        emotionChangeLogs.addAll(loadedChangeLogs)
                    }
                }
                // 初回表示時にRoomから保存済みデータを取得する
                LaunchedEffect(Unit) {
                    // 一旦一覧を空にする
                    emotionLogs.clear()
                    // Roomから取得したデータを一覧へ追加
                    emotionLogs.addAll(
                        emotionLogDao.getAllLogs()
                    )
                    loadChangeLogs()
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },

                    floatingActionButton = {
                        if (currentRoute == Routes.REFLECTION) {
                            FloatingActionButton(
                                onClick = {
                                    navController.navigate(Routes.RECORD)
                                },
                                containerColor = Color(0xFFB8A898)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "ココログを追加"
                                )
                            }
                        }
                    },

                    // 画面下部のタブメニュー
                    bottomBar = {

                        NavigationBar(
                            containerColor = Color(0xFFE7DDCF)
                        ) {
                            // ホーム
                            NavigationBarItem(
                                selected = currentRoute == Routes.HOME,
                                onClick = {
                                    navController.navigate(Routes.HOME)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "ホーム"
                                    )
                                },
                                label = { Text("ホーム") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KokologTextColor,
                                    selectedTextColor = KokologTextColor,
                                    unselectedIconColor = KokologSubTextColor,
                                    unselectedTextColor = KokologSubTextColor,
                                    indicatorColor = Color(0xFFD8C8B3)
                                )
                            )

                            // 振り返りタブ
                            NavigationBarItem(
                                selected = currentRoute == Routes.REFLECTION,
                                onClick = {
                                    navController.navigate(Routes.REFLECTION)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Book,
                                        contentDescription = "振り返る"
                                    )
                                },
                                label = { Text("振り返る") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KokologTextColor,
                                    selectedTextColor = KokologTextColor,
                                    unselectedIconColor = KokologSubTextColor,
                                    unselectedTextColor = KokologSubTextColor,
                                    indicatorColor = Color(0xFFD8C8B3)
                                )
                            )

                            // 設定タブ
                            NavigationBarItem(
                                selected = currentRoute == Routes.SETTINGS,
                                onClick = {
                                    navController.navigate(Routes.SETTINGS)
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "設定"
                                    )
                                },
                                label = { Text("設定") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KokologTextColor,
                                    selectedTextColor = KokologTextColor,
                                    unselectedIconColor = KokologSubTextColor,
                                    unselectedTextColor = KokologSubTextColor,
                                    indicatorColor = Color(0xFFD8C8B3)

                                )
                            )
                        }
                    }

                ) { innerPadding ->

                    // タブと画面が重ならないよう余白を適用
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = Color(0xFFF6F1E8)
                    ) {
                        val visibleEmotionLogs =
                            if (showIgnoredLogs) {
                                emotionLogs
                            } else {
                                emotionLogs.filter { log ->
                                    !log.isIgnored
                                }
                            }
                        // タブごとに表示画面を切り替える
                        // Navigationで画面切り替えを管理する
                        NavHost(
                            // どのNavigationを使うか
                            navController = navController,

                            // アプリ起動時に最初に表示する画面
                            startDestination = Routes.HOME
                        ) {
                            /**
                             * ホーム画面
                             */
                            composable(Routes.HOME) {
                                if (!tutorialDone) {
                                    TutorialScreen(
                                        onFinish = {

                                        tutorialDone = true

                                        prefs.edit()
                                            .putBoolean("tutorial_done", true)
                                            .apply()

                                        navController.navigate("record")
                                      }
                                    )
                                } else {
                                    /** チュートリアルテスト用コード
                                    prefs.edit()
                                        .putBoolean("tutorial_done", false)
                                        .apply()
                                    **/
                                    HomeScreen(
                                        logs = visibleEmotionLogs,
                                        showRecentLogs = showRecentLogs,
                                        onRecordClick = {
                                            navController.navigate(Routes.RECORD)
                                        },
                                        onLogClick = { log ->
                                            navController.navigate("${Routes.DETAIL}/${log.id}")
                                        }
                                    )
                                }
                            }

                            /**
                             * 記録画面
                             */
                            composable(Routes.RECORD) {
                                RecordScreen(
                                    onSave = { log ->
                                        coroutineScope.launch {
                                            emotionLogDao.insert(log)

                                            emotionLogs.clear()
                                            emotionLogs.addAll(
                                                emotionLogDao.getAllLogs()
                                            )

                                            snackbarHostState.showSnackbar("ココログをそっと保管しました")

                                            // 保存後はホームへ戻す
                                            if (returnHomeAfterSave) {
                                                navController.navigate(Routes.HOME)
                                            }
                                        }
                                    }
                                )
                            }

                            /**
                             * 振り返り画面
                             */
                            composable(Routes.REFLECTION) {
                                ReflectionScreen(
                                    // 一覧表示用ログ
                                    logs = visibleEmotionLogs,

                                    // ログタップ時
                                    onLogClick = { log ->

                                        // 詳細画面へ移動
                                        navController.navigate("${Routes.DETAIL}/${log.id}")
                                    }
                                )
                            }

                            /**
                             * 設定画面
                             */
                            @OptIn(ExperimentalMaterial3Api::class)
                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    showRecentLogs = showRecentLogs,
                                    onShowRecentLogsChange = { checked ->
                                        showRecentLogs = checked

                                        prefs.edit()
                                            .putBoolean("show_recent_logs", checked)
                                            .apply()
                                    },
                                    returnHomeAfterSave = returnHomeAfterSave,
                                    onReturnHomeAfterSaveChange = { checked ->
                                        returnHomeAfterSave = checked

                                        prefs.edit()
                                            .putBoolean("return_home_after_save", checked)
                                            .apply()
                                    },
                                    showIgnoredLogs = showIgnoredLogs,
                                    onShowIgnoredLogsChange = { checked ->
                                        showIgnoredLogs = checked

                                        prefs.edit()
                                            .putBoolean("show_ignored_logs", checked)
                                            .apply()
                                    },
                                    notificationEnabled = notificationEnabled,
                                    onNotificationEnabledChange = { checked ->
                                        notificationEnabled = checked

                                        prefs.edit()
                                            .putBoolean("notification_enabled", checked)
                                            .apply()

                                        if (checked) {
                                            scheduleKokologNotification(
                                                context = applicationContext,
                                                hour = notificationHour,
                                                minute = notificationMinute
                                            )
                                        } else {
                                            cancelKokologNotification(applicationContext)
                                        }
                                    },

                                    notificationHour = notificationHour,
                                    notificationMinute = notificationMinute,
                                    onNotificationTimeChange = { hour, minute ->
                                        notificationHour = hour
                                        notificationMinute = minute

                                        prefs.edit()
                                            .putInt("notification_hour", hour)
                                            .putInt("notification_minute", minute)
                                            .apply()

                                        if (notificationEnabled) {
                                            scheduleKokologNotification(
                                                context = applicationContext,
                                                hour = hour,
                                                minute = minute
                                            )
                                        }
                                    },
                                )
                            }
                            /**
                             * 詳細画面
                             *
                             * detail/{logId} の形でログIDを受け取る
                             */
                            composable("${Routes.DETAIL}/{logId}") { backStackEntry ->

                                // Navigationから渡されたログIDを取得
                                val logId = backStackEntry.arguments
                                    ?.getString("logId")
                                    ?.toLongOrNull()

                                // IDに一致するログを一覧から探す
                                val targetLog = emotionLogs.find { log ->
                                    log.id == logId
                                }

                                if (targetLog != null) {

                                    DetailScreen(
                                        // 表示対象ログ
                                        log = targetLog,
                                        changeLogs = emotionChangeLogs.filter {
                                            it.originalLogId == targetLog.id
                                        },
                                        // 戻るボタン押下時
                                        onBack = {
                                            navController.popBackStack()
                                        },
                                        onAddChange = { logId ->
                                            navController.navigate(
                                                "${Routes.CHANGE}/$logId"
                                            )
                                        },
                                        onDeleteChange = { changeLog ->
                                            coroutineScope.launch {
                                                emotionChangeLogDao.deleteChangeLog(changeLog)
                                                loadChangeLogs()
                                            }
                                        },
                                        // 削除ボタン押下時
                                        onDelete = { log ->

                                            coroutineScope.launch {

                                                // Roomから削除
                                                emotionLogDao.delete(log)

                                                // 最新状態取得
                                                emotionLogs.clear()
                                                emotionLogs.addAll(
                                                    emotionLogDao.getAllLogs()
                                                )

                                                // 削除後は前画面へ戻る
                                                navController.popBackStack()
                                            }
                                        },
                                        onToggleIgnored = { log ->
                                            coroutineScope.launch {
                                                emotionLogDao.updateIgnored(
                                                    logId = log.id,
                                                    isIgnored = !log.isIgnored
                                                )

                                                emotionLogs.clear()
                                                emotionLogs.addAll(
                                                    emotionLogDao.getAllLogs()
                                                )
                                            }
                                        },
                                        onEdit = {
                                            navController.navigate(
                                                "${Routes.EDIT}/${targetLog.id}"
                                            )
                                        }
                                    )

                                } else {

                                    // 対象ログが見つからない場合
                                    Text("ココログがみつかりません")
                                }
                            }
                            // そのあとのこころ
                            composable("${Routes.CHANGE}/{logId}") { backStackEntry ->

                                val logId = backStackEntry
                                    .arguments
                                    ?.getString("logId")
                                    ?.toLongOrNull()

                                // IDに一致するログを一覧から探す
                                val targetLog = emotionLogs.find { log ->
                                    log.id == logId
                                }

                                if (logId != null && targetLog != null) {

                                    EmotionChangeScreen(
                                        originalLogId = logId,
                                        originalCreatedAt = targetLog.createdAt,

                                        onSave = { changeLog ->
                                            scope.launch {

                                                emotionChangeLogDao
                                                    .insertChangeLog(changeLog)
                                                loadChangeLogs()
                                                navController.popBackStack()
                                            }
                                        },

                                        onBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }

                            /**
                             * 編集画面
                             */
                            composable(
                                route = "${Routes.EDIT}/{logId}"
                            ) { backStackEntry ->

                                val logId = backStackEntry.arguments
                                    ?.getString("logId")
                                    ?.toLongOrNull()

                                val targetLog = emotionLogs.find { log ->
                                    log.id == logId
                                }

                                if (targetLog != null) {

                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EditLogScreen(
                                            log = targetLog,
                                            onBack = {
                                                navController.popBackStack()
                                            },
                                            onSave = { updatedLog ->

                                                coroutineScope.launch {
                                                    emotionLogDao.updateLog(updatedLog)

                                                    val index =
                                                        emotionLogs.indexOfFirst { log ->
                                                            log.id == updatedLog.id
                                                        }

                                                    if (index != -1) {
                                                        emotionLogs[index] = updatedLog
                                                    }

                                                    navController.navigate(
                                                        "${Routes.DETAIL}/${updatedLog.id}"
                                                    ) {
                                                        popUpTo("${Routes.EDIT}/{logId}") {
                                                            inclusive = true
                                                        }

                                                        launchSingleTop = true
                                                    }
                                                    snackbarHostState.showSnackbar(
                                                        message = "変更を保管しました"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
/**
 * ココログ用の日時表示
 *
 * 画面ごとに日付表示がバラつかないように、
 * ここでまとめて管理する
 */
fun formatKokologDateTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    return formatter.format(Date(timeMillis))
}

/**
 * 未来日時かどうかを判定する
 */
fun isFutureDateTime(timeMillis: Long): Boolean {
    return timeMillis > System.currentTimeMillis()
}

/**
 * あのときのココロ候補を探す
 *
 * 1週間前 → 1か月前 → 半年前 → 1年前 の順で、
 * 指定日付の前後数日から近いログを探す
 */
fun findPastKokoroLog(
    logs: List<EmotionLog>
): Pair<String, EmotionLog>? {

    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L

    val candidates = listOf(
        "10年前" to Pair(3650, 90),
        "7年前" to Pair(2555, 90),
        "5年前" to Pair(1825, 60),
        "3年前" to Pair(1095, 45),
        "2年前" to Pair(730, 30),
        "1年前" to Pair(365, 30),
        "半年前" to Pair(180, 14),
        "1か月前" to Pair(30, 7),
        "1週間前" to Pair(7, 3)
    )

    for ((label, range) in candidates) {
        val targetDaysAgo = range.first
        val allowedDays = range.second

        val targetTime = now - targetDaysAgo * oneDayMillis
        val startTime = targetTime - allowedDays * oneDayMillis
        val endTime = targetTime + allowedDays * oneDayMillis

        val foundLog = logs
            .filter { log ->
                log.createdAt in startTime..endTime
            }
            .maxByOrNull { log ->
                log.createdAt
            }

        if (foundLog != null) {
            return label to foundLog
        }
    }

    return null
}

/**
 * 何日前かをざっくり表示する
 */
fun formatRelativeDay(timeMillis: Long): String {
    val now = System.currentTimeMillis()
    val oneDayMillis = 24L * 60L * 60L * 1000L

    val diffDays =
        ((now - timeMillis) / oneDayMillis).toInt()

    return when (diffDays) {
        0 -> "今日"
        1 -> "昨日"
        in 2..6 -> "${diffDays}日前"
        else -> "7日以上前"
    }
}

/**
 * 7日以内の最新ココログを探す
 */
fun findRecentKokoroLog(
    logs: List<EmotionLog>
): EmotionLog? {
    val now = System.currentTimeMillis()
    val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L

    return logs
        .filter { log ->
            now - log.createdAt <= sevenDaysMillis
        }
        .maxByOrNull { log ->
            log.createdAt
        }
}

/**
 * 記録画面
 *
 * 感情入力を行う画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(

    // 保存時に呼ばれる関数
    onSave: (EmotionLog) -> Unit

) {

    // 初期感情一覧
    val emotions = emotionUiList

    // 現在選択中の感情
    var selectedEmotion by remember {
        mutableStateOf("ふつう")
    }

    // 感情の強さ
    var intensity by remember {
        mutableStateOf(3f)
    }

    // メモ入力内容
    var memo by remember {
        mutableStateOf("")
    }

    // 保存処理中かどうか
    var isSaving by remember {
        mutableStateOf(false)
    }

    // 確認ダイアログ
    var showSaveDialog by remember {
        mutableStateOf(false)
    }

    // 記録日時
    var selectedDateTimeMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    // 未来日チェック
    var dateTimeError by remember {
        mutableStateOf(false)
    }

    // 日付ピッカー表示フラグ
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    // 時間ピッカー表示フラグ
    var showTimePicker by remember {
        mutableStateOf(false)
    }

    // 日時表示用
    val dateFormat = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.JAPAN)
    }

    val dateText = dateFormat.format(selectedDateTimeMillis)
    val timeText = timeFormat.format(selectedDateTimeMillis)

    val categoryList = listOf(
        "仕事",
        "人間関係",
        "家族",
        "健康",
        "学校",
        "お金",
        "趣味",
        "その他"
    )

    val categoryTagMap = mapOf(
        "仕事" to listOf("会議", "残業", "ミス", "プレッシャー", "締切"),
        "人間関係" to listOf("上司", "友達", "SNS", "LINE", "気疲れ"),
        "家族" to listOf("子ども", "パートナー", "親", "家事", "予定"),
        "健康" to listOf("睡眠不足", "疲労", "体調不良", "通院", "運動"),
        "学校" to listOf("勉強", "テスト", "友達", "先生", "進路"),
        "お金" to listOf("出費", "節約", "投資", "収入", "将来不安"),
        "趣味" to listOf("ゲーム", "読書", "音楽", "創作", "リフレッシュ"),
        "その他" to emptyList()
    )

    var selectedCategory by remember {
        mutableStateOf("その他")
    }
    val tagList = categoryTagMap[selectedCategory] ?: emptyList()
    val selectedTags = remember {
        mutableStateListOf<String>()
    }

    // 気にしない
    var isIgnored by remember {
        mutableStateOf(false)
    }
    // タグ自由入力
    var customTagText by remember {
        mutableStateOf("")
    }

    fun updateSelectedDateTime(newMillis: Long) {
        selectedDateTimeMillis = newMillis
        dateTimeError = newMillis > System.currentTimeMillis()
    }

    val dateTimeButtonColor =
        if (dateTimeError) {
            Color(0xFFFFEAEA)
        } else {
            Color(0xFFD8CFC4)
        }

    val dateTimeTextColor =
        if (dateTimeError) {
            Color(0xFFB05A5A)
        } else {
            KokologTextColor
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp,
                bottom = 200.dp
            ),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8DFC8)
            )
        ) {
            KokologHeader(
                title = "ココログを残す"
            )
        }
        Text(
            text = "今のココロに近いものを選んでください。",
            color = KokologSubTextColor
        )

        /**
         * 感情ボタン一覧
         */
        LazyVerticalGrid(

            columns = GridCells.Fixed(2),

            horizontalArrangement = Arrangement.spacedBy(12.dp),

            verticalArrangement = Arrangement.spacedBy(12.dp),

            modifier = Modifier.height(150.dp)

        ) {

            items(emotions) { emotionUi ->

                EmotionChip(
                    emotionUi = emotionUi,
                    isSelected = selectedEmotion == emotionUi.label,
                    onClick = {
                        selectedEmotion = emotionUi.label
                    }
                )
            }
        }
        Text(
            text = "スクロールすると下にもココロがあります",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = KokologLightTextColor
        )

        Text(
            text = "カテゴリ",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(categoryList) { category ->

                Button(
                    onClick = {
                        selectedCategory = category
                        // カテゴリが変わったら、前のカテゴリで選んだタグはリセットする
                        selectedTags.clear()
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (selectedCategory == category) {
                                KokologButtonColor
                            } else {
                                Color(0xFFD8CFC4)
                            }
                    )
                ) {

                    Text(category)
                }
            }
        }
        Text(
            text = "タグ",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        if (tagList.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tagList) { tag ->
                    val isSelected = selectedTags.contains(tag)

                    Button(
                        onClick = {
                            if (isSelected) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.add(tag)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (isSelected) {
                                    KokologButtonColor
                                } else {
                                    Color(0xFFD8CFC4)
                                }
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = if (isSelected) "✓ $tag" else tag,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "タグを追加",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customTagText,
                onValueChange = {
                    customTagText = it
                },
                placeholder = {
                    Text(
                        text = "タグ名",
                        color = KokologLightTextColor
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = KokologTextColor,
                    unfocusedTextColor = KokologTextColor,
                    focusedBorderColor = Color(0xFFD8CFC4),
                    unfocusedBorderColor = Color(0xFFE9E1D7),
                    cursorColor = KokologButtonColor,
                    focusedContainerColor = Color(0xFFF8F5F7),
                    unfocusedContainerColor = Color(0xFFF8F5F7)
                )
            )

            Button(
                onClick = {
                    val newTag = customTagText.trim()

                    if (newTag.isNotBlank() && !selectedTags.contains(newTag)) {
                        selectedTags.add(newTag)
                        customTagText = ""
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KokologButtonColor
                )
            ) {
                Text("追加")
            }
        }

        if (selectedTags.isNotEmpty()) {
            Text(
                text = "選択中のタグ",
                fontWeight = FontWeight.Bold,
                color = KokologTextColor
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTags) { tag ->
                    Button(
                        onClick = {
                            selectedTags.remove(tag)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD8CFC4)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = "× $tag",
                            fontSize = 13.sp,
                            color = KokologTextColor
                        )
                    }
                }
            }
        }
        /**
         * 感情強度表示
         */
        HeartSizeLabel_FULL(
            intensity = intensity.toInt()
        )

        /**
         * 感情強度スライダー
         *
         * 1〜5の範囲で選択
         */
        Slider(
            value = intensity,
            onValueChange = {
                intensity = it
            },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = KokologButtonColor,
                activeTrackColor = KokologButtonColor,
                inactiveTrackColor = KokologBottomBarColor
            ),
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "記録する日時",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        // 日付
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    showDatePicker = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = dateTimeButtonColor,
                    contentColor = dateTimeTextColor
                )
            ) {
                Text(
                    text = dateText,
                    color = dateTimeTextColor
                )
            }

            Button(
                onClick = {
                    showTimePicker = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = dateTimeButtonColor,
                    contentColor = dateTimeTextColor
                )
            ) {
                Text(
                    text = timeText,
                    color = dateTimeTextColor
                )
            }
        }

        if (dateTimeError) {
            Text(
                text = "未来の日時は選べません。",
                color = Color(0xFFB05A5A),
                fontSize = 13.sp
            )
        }


        /**
         * メモ入力欄
         */
        OutlinedTextField(

            value = memo,

            onValueChange = {
                memo = it
            },

            placeholder = {
                Text(
                    text = "今のココロを、少しだけ書いてみる",
                    color = KokologLightTextColor
                )
            },

            shape = RoundedCornerShape(16.dp),

            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),

            minLines = 4,
            maxLines = 4,
            singleLine = false,
            colors = OutlinedTextFieldDefaults.colors(

                // 入力文字
                focusedTextColor = KokologTextColor,
                unfocusedTextColor = KokologTextColor,

                // placeholder
                focusedPlaceholderColor = KokologLightTextColor,
                unfocusedPlaceholderColor = KokologLightTextColor,

                // 枠線
                focusedBorderColor = Color(0xFFD8CFC4),
                unfocusedBorderColor = Color(0xFFE9E1D7),

                // カーソル
                cursorColor = KokologButtonColor,

                // 背景
                focusedContainerColor = Color(0xFFF8F5F7),
                unfocusedContainerColor = Color(0xFFF8F5F7)
            ),
            textStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        )

        // 気にしないチェックボックス
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isIgnored = !isIgnored
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isIgnored,
                onCheckedChange = {
                    isIgnored = it
                }
            )

            Text(
                text = "このココロは、もう気にしない",
                color = KokologSubTextColor
            )
        }
        /**
         * 保存ボタン
         */
        Button(
            enabled = !isSaving && !dateTimeError,
            onClick = {
                showSaveDialog = true
            },
            modifier = Modifier
                .align(Alignment.End)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C8B7A)
            ),

            ) {

            Text(
                if (isSaving) {
                    "このココログを保管しています..."
                } else {
                    "このココログを残す"
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTimeMillis
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis =
                            datePickerState.selectedDateMillis

                        if (selectedMillis != null) {
                            val currentCalendar = Calendar.getInstance()
                            currentCalendar.timeInMillis = selectedDateTimeMillis

                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.timeInMillis = selectedMillis

                            selectedCalendar.set(
                                Calendar.HOUR_OF_DAY,
                                currentCalendar.get(Calendar.HOUR_OF_DAY)
                            )
                            selectedCalendar.set(
                                Calendar.MINUTE,
                                currentCalendar.get(Calendar.MINUTE)
                            )

                            updateSelectedDateTime(selectedCalendar.timeInMillis)

                        }

                        showDatePicker = false
                    }
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFFF8F5F7),
                    titleContentColor = KokologTextColor,
                    headlineContentColor = KokologTextColor,
                    weekdayContentColor = KokologSubTextColor,
                    subheadContentColor = KokologSubTextColor,
                    yearContentColor = KokologTextColor,
                    currentYearContentColor = KokologButtonColor,
                    selectedYearContainerColor = KokologButtonColor,
                    selectedYearContentColor = Color.White,
                    dayContentColor = KokologTextColor,
                    selectedDayContainerColor = KokologButtonColor,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = KokologButtonColor,
                    todayContentColor = KokologButtonColor
                )
            )
        }
    }
    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateTimeMillis
        }

        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDateTimeMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        updateSelectedDateTime(newCalendar.timeInMillis)
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "決定",
                        color = KokologButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7),
            title = {
                Text(
                    text = "時間を選ぶ",
                    color = KokologTextColor
                )
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFFF8F5F7),

                        selectorColor = KokologButtonColor,

                        containerColor = Color(0xFFF8F5F7),

                        timeSelectorSelectedContainerColor = KokologButtonColor,
                        timeSelectorSelectedContentColor = Color.White,

                        timeSelectorUnselectedContainerColor = Color(0xFFE9E1D7),
                        timeSelectorUnselectedContentColor = KokologTextColor,

                        // 追加
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = KokologTextColor
                    )
                )
            }
        )
    }

    // 確認ダイアログ→保存
    if (showSaveDialog) {

        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
            },

            title = {
                Text(
                    text = "ココログを残しますか？",
                    color = KokologTextColor
                )
            },

            text = {
                Text(
                    text = "入力した内容を保管します。",
                    color = KokologSubTextColor
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showSaveDialog = false

                        // すでに保存中なら何もしない
                        if (isSaving) {
                            return@TextButton
                        }

                        // 保存開始
                        isSaving = true

                        // 未来日チェック
                        if (isFutureDateTime(selectedDateTimeMillis)) {
                            dateTimeError = true
                            showSaveDialog = false
                            return@TextButton
                        }

                        // EmotionLog生成
                        val newLog = EmotionLog(
                            // 選択感情
                            emotion = selectedEmotion,
                            // カテゴリ
                            category = selectedCategory,
                            // タグ
                            tags = selectedTags.joinToString(","),
                            // 気にしない
                            isIgnored = isIgnored,
                            // 強度
                            intensity = intensity.toInt(),
                            // メモ
                            memo = memo,
                            // 作成日時
                            createdAt = selectedDateTimeMillis
                        )

                        // MainActivityへ保存通知
                        onSave(newLog)

                        // 入力内容を初期化
                        memo = ""
                        selectedEmotion = "ふつう"
                        intensity = 3f
                        selectedTags.clear()
                        selectedCategory = "その他"
                        customTagText = ""
                        isIgnored = false
                        selectedDateTimeMillis = System.currentTimeMillis()
                        dateTimeError = false
                        // 保存終了
                        isSaving = false
                    }
                ) {
                    Text(
                        "保管",
                        color = KokologButtonColor
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showSaveDialog = false
                    }
                ) {
                    Text(
                        "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },

            containerColor = Color(0xFFF8F5F7)
        )
    }
}


// ココログ共通カラー
val KokologTextColor = Color(0xFF4E463F)
val KokologSubTextColor = Color(0xFF7A6A58)
val KokologLightTextColor = Color(0xFF9A8A7A)
val KokologButtonColor = Color(0xFFA8927B)
val KokologBottomBarColor = Color(0xFFE7DDCF)
val KokologBackgroundColor = Color(0xFFFAF7EF)

/**
 * ミリ秒の日時を表示用文字列へ変換する
 *
 * 例:
 * 2026/05/15 10:30
 */
fun formatDate(timestamp: Long): String {

    val formatter = SimpleDateFormat(
        "yyyy/MM/dd HH:mm",
        Locale.JAPAN
    )

    return formatter.format(Date(timestamp))
}

/**
 * ミリ秒の日時を日付だけの文字列へ変換する
 *
 * 例:
 * 2026/05/19
 */
fun formatDateOnly(timestamp: Long): String {

    val formatter = SimpleDateFormat(
        "yyyy/MM/dd",
        Locale.JAPAN
    )

    return formatter.format(Date(timestamp))
}


/**
 * こころの大きさをハートで表示する
 *
 * 例:
 * 3 → ●●●○○
 */
fun heartSizeText(intensity: Int): String {
    val filled = "🩷".repeat(intensity)
    val empty = "🤍".repeat(5 - intensity)

    return filled + empty
}

/**
 * スリムカード共通部品
 */
@Composable
fun HomeSlimKokoroCard(
    label: String,
    log: EmotionLog,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBF5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KokologSubTextColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${emotionIcon(log.emotion)} ${log.emotion}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KokologTextColor,
                    maxLines = 1
                )

                if (log.category.isNotBlank()) {
                    Text(
                        text = log.category,
                        fontSize = 11.sp,
                        color = KokologSubTextColor,
                        maxLines = 1
                    )
                }
            }

            if (log.memo.isNotBlank()) {
                Text(
                    text = log.memo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = KokologSubTextColor,
                    fontSize = 11.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                Text(
                    text = "詳細を見る 🔍",
                    fontSize = 12.sp,
                    color = KokologSubTextColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    logs: List<EmotionLog>,
    showRecentLogs: Boolean,
    onRecordClick: () -> Unit,
    onLogClick: (EmotionLog) -> Unit
) {

    val welcomeMessages = listOf(
        "今のココロを、そっと残してみましょう",
        "今日もおつかれさまです",
        "どんな気持ちも、そのままで大丈夫",
        "少しだけ、今の気持ちを書いてみませんか",
        "うれしい日も、そうでない日も",
        "ココロの足あとを残してみましょう",
        "今日のあなたは、どんな感じですか？",
        "ふり返るためじゃなく、置いておくために",
        "今の気持ちに、名前をつけてみる",
        "ここは、ココロを置いておく場所"
    )
    val randomMessage = remember {
        welcomeMessages.random()
    }

    val pastKokoro = findPastKokoroLog(logs)
    val recentKokoro = findRecentKokoroLog(logs)
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 120.dp,
                bottom = 120.dp
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ココログ",
            fontSize = 32.sp,
            color = Color(0xFF4E463F),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = randomMessage,
            color = KokologSubTextColor,
            fontSize = 14.sp
        )

        Button(
            onClick = onRecordClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C8B7A)
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(60.dp)
        ) {
            Text(
                text = "ココログを残す",
                fontSize = 16.sp
            )
        }

        // あのころのココロ
        pastKokoro?.let { (label, log) ->

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌱",
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "あのときのココロ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KokologTextColor
                )
            }

            HomeSlimKokoroCard(
                label = "$label のココロ",
                log = log,
                onClick = {
                    onLogClick(log)
                }
            )
        }

        if (showRecentLogs && recentKokoro != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌱",
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "さいきん残したココログ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = KokologTextColor
                )
            }

            HomeSlimKokoroCard(
                label = formatRelativeDay(recentKokoro.createdAt),
                log = recentKokoro,
                onClick = {
                    onLogClick(recentKokoro)
                }
            )
        }
    }
}

/**
 * 振り返り画面
 *
 * 保存された感情ログ一覧を表示する
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionScreen(

    // 表示対象の感情ログ一覧
    logs: List<EmotionLog>,
    onLogClick: (EmotionLog) -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        val filterEmotions = listOf("すべて") + emotionUiList.map { it.label }
        var selectedFilter by rememberSaveable {
            mutableStateOf("すべて")
        }
        var selectedCategoryFilter by rememberSaveable {
            mutableStateOf("すべて")
        }
        var selectedDateFilter by rememberSaveable {
            mutableStateOf("すべて")
        }
        val dateFilters = listOf(
            "すべて",
            "今日",
            "7日",
            "30日"
        )
        var summaryDialogType by remember   {
            mutableStateOf<String?>(null)
        }
        var ignoredFilter by rememberSaveable {
            mutableStateOf("すべて")
        }

        val filterUnselectedColor = Color(0xFFF3ECE3)
        val filterSelectedBeigeColor = Color(0xFFD9C6AF)
        val filterTextColor = KokologTextColor
        KokologHeader(
            title = "振り返り"
        )
        val chunkedFilters = filterEmotions.chunked(2)
        FilterRowWithLabel(
            label = "ココロで絞る"
        ) {
            items(filterEmotions) { emotion ->

                val isSelected =
                    selectedFilter == emotion

                val emotionColor =
                    emotionUiList
                        .firstOrNull { it.label == emotion }
                        ?.color
                        ?: filterSelectedBeigeColor

                val count =
                    if (emotion == "すべて") {
                        logs.count { log ->
                            selectedCategoryFilter == "すべて" ||
                                    log.category == selectedCategoryFilter
                        }
                    } else {
                        logs.count { log ->
                            val categoryMatched =
                                selectedCategoryFilter == "すべて" ||
                                        log.category == selectedCategoryFilter

                            categoryMatched && log.emotion == emotion
                        }
                    }

                Button(
                    onClick = {
                        selectedFilter = emotion
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) {
                            emotionColor
                        } else {
                            filterUnselectedColor
                        },
                        contentColor = filterTextColor
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(28.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 0.dp
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                ) {
                    Text(
                        text = "$emotion ($count)",
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = filterTextColor
                    )
                }
            }
        }
        // ログが0件の場合
        if (logs.isEmpty()) {
            EmptyStateCard(
                title = "まだ残したココログはありません",
                message = "今の“ココロ”を、そっと残してみましょう",
                topPadding = 4.dp,
                verticalPadding = 5.dp
            )
            return
        } else {
            val categoryOrder = listOf(
                "仕事",
                "人間関係",
                "家族",
                "健康",
                "学校",
                "お金",
                "趣味",
                "その他"
            )

            val categoryFilters = listOf(
                "すべて",
                "仕事",
                "人間関係",
                "家族",
                "健康",
                "学校",
                "お金",
                "趣味",
                "その他"
            )

            val now =
                System.currentTimeMillis()

            val oneDayMillis =
                24L * 60L * 60L * 1000L
            val allCount = logs.size

            val todayCount =
                logs.count { log ->
                    log.createdAt >= now - oneDayMillis
                }

            val sevenDaysCount =
                logs.count { log ->
                    log.createdAt >= now - oneDayMillis * 7
                }

            val thirtyDaysCount =
                logs.count { log ->
                    log.createdAt >= now - oneDayMillis * 30
                }

            val filteredLogs =
                logs.filter { log ->

                    val emotionMatched =
                        selectedFilter == "すべて" ||
                                log.emotion == selectedFilter

                    val categoryMatched =
                        selectedCategoryFilter == "すべて" ||
                                log.category == selectedCategoryFilter

                    val dateMatched =
                        when (selectedDateFilter) {
                            "今日" -> {
                                log.createdAt >= now - oneDayMillis
                            }

                            "7日" -> {
                                log.createdAt >= now - oneDayMillis * 7
                            }

                            "30日" -> {
                                log.createdAt >= now - oneDayMillis * 30
                            }

                            else -> {
                                true
                            }
                        }
                    val ignoredMatch =
                        when (ignoredFilter) {
                            "すべて" -> true
                            "通常" -> !log.isIgnored
                            "そっと置いた" -> log.isIgnored
                            else -> true
                        }

                    emotionMatched && categoryMatched && dateMatched && ignoredMatch
                }

            val groupedLogs =
                filteredLogs
                    .groupBy { log ->
                        log.category
                    }
                    .toList()
                    .sortedBy { item ->
                        val category = item.first
                        val index = categoryOrder.indexOf(category)

                        if (index == -1) {
                            Int.MAX_VALUE
                        } else {
                            index
                        }
                    }

            FilterRowWithLabel(
                label = "棚で絞る"
            ) {
                items(categoryFilters) { category ->

                    val isSelected =
                        selectedCategoryFilter == category

                    val count =
                        if (category == "すべて") {
                            logs.count { log ->
                                selectedFilter == "すべて" ||
                                        log.emotion == selectedFilter
                            }
                        } else {
                            logs.count { log ->
                                val emotionMatched =
                                    selectedFilter == "すべて" ||
                                            log.emotion == selectedFilter

                                emotionMatched && log.category == category
                            }
                        }

                    Button(
                        onClick = {
                            selectedCategoryFilter = category
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                filterSelectedBeigeColor
                            } else {
                                filterUnselectedColor
                            },
                            contentColor = filterTextColor
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(28.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            disabledElevation = 0.dp
                        ),
                    ) {
                        Text(
                            text = "$category ($count)",
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = filterTextColor
                        )
                    }
                }
            }

            FilterRowWithLabel(
                label = "期間で絞る"
            ) {
                items(dateFilters) { dateFilter ->

                    val isSelected =
                        selectedDateFilter == dateFilter

                    Button(
                        onClick = {
                            selectedDateFilter = dateFilter
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                filterSelectedBeigeColor
                            } else {
                                filterUnselectedColor
                            },
                            contentColor = filterTextColor
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(28.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            disabledElevation = 0.dp
                        ),
                    ) {
                        val count =
                            when (dateFilter) {
                                "すべて" -> allCount
                                "今日" -> todayCount
                                "7日" -> sevenDaysCount
                                "30日" -> thirtyDaysCount
                                else -> 0
                            }
                        Text(

                            text = "$dateFilter($count)",
                            fontSize = 11.sp,
                            color = filterTextColor
                        )
                    }
                }
            }


            FilterRowWithLabel(
                label = "状態で絞る"
            ) {
                items(
                    listOf("すべて", "通常", "そっと置いた")
                ) { filter ->

                    val isSelected = ignoredFilter == filter

                    Button(
                        onClick = {
                            ignoredFilter = filter
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                filterSelectedBeigeColor
                            } else {
                                filterUnselectedColor
                            },
                            contentColor = filterTextColor
                        ),
                        modifier = Modifier
                            .height(28.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp,
                            disabledElevation = 0.dp
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            color = filterTextColor
                        )
                    }
                }
            }

            FilterRowWithLabel(
                label = "振り返る"
            ) {
                item {
                    Button(
                        onClick = {
                            summaryDialogType = "week"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDCE8D2),
                            contentColor = KokologTextColor
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = "📚 今週のココロ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            summaryDialogType = "month"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDCE8D2),
                            contentColor = KokologTextColor
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            text = "📚 今月のココロ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (filteredLogs.isEmpty()) {
                EmptyStateCard(
                    title = "条件に合うココログがありません",
                    message = "別の棚や期間も、そっと見てみましょう",
                    topPadding = 4.dp,
                    verticalPadding = 2
                        .dp
                )
            } else {
                // 新しい順に表示
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 18.dp,
                            end = 18.dp,
                            top = 14.dp,
                            bottom = 8.dp
                        ),

                    verticalArrangement = Arrangement.spacedBy(12.dp),

                    contentPadding = PaddingValues(
                        bottom = 20.dp
                    )
                ) {

                    items(groupedLogs) { item ->

                        val category = item.first
                        val logsInCategory = item.second

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),

                            shape = RoundedCornerShape(24.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFFBF4)
                            ),

                            border = BorderStroke(
                                1.dp,
                                Color(0xFFE0D4C3)
                            ),

                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 14.dp,
                                        end = 14.dp,
                                        top = 14.dp,
                                        bottom = 8.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // 左線
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(
                                            Color(0xFFE0D4C3)
                                        )
                                )

                                Spacer(
                                    modifier = Modifier.width(10.dp)
                                )

                                // ラベル
                                Surface(
                                    modifier = Modifier.offset(y = (-8).dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFE6D9C9)
                                ) {
                                    Text(
                                        text = "${category}の棚(${logsInCategory.size})",

                                        modifier = Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        ),

                                        fontWeight = FontWeight.Bold,

                                        color = KokologTextColor
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.width(10.dp)
                                )

                                // 右線
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(
                                            Color(0xFFE0D4C3)
                                        )
                                )
                            }
                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(
                                    start = 6.dp,
                                    end = 18.dp
                                )
                            ) {
                                itemsIndexed(logsInCategory) { index, log ->

                                    val isFocus = index == 1

                                    BookshelfSpineCard(
                                        log = log,
                                        onClick = {
                                            onLogClick(log)
                                        },
                                        modifier = Modifier
                                            .width(
                                                //if (isFocus) 104.dp else
                                                76.dp
                                            )
                                            .height(
                                                //if (isFocus) 190.dp else
                                                250.dp
                                            )
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .height(6.dp)
                                        .background(
                                            Color(0xFFD6C6B8),
                                            RoundedCornerShape(12.dp)
                                        )
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                        }
                    }
                }
            }
        }
        summaryDialogType?.let { type ->
            val summary = createKokoroSummary(
                logs = logs,
                type = type
            )

            AlertDialog(
                onDismissRequest = {
                    summaryDialogType = null
                },
                containerColor = KokologBackgroundColor,
                title = {
                    Text(
                        text = "📚 ${summary.title} 📚",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = KokologTextColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        if (summary.topEmotion != null) {
                            val emotionUi =
                                emotionUiList.firstOrNull {
                                    it.label == summary.topEmotion
                                }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = emotionUi?.color ?: Color(0xFFE8DFC8)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${summary.topEmotionIcon} ${summary.topEmotion}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = KokologTextColor,
                                    fontSize = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = "📖 ${summary.totalCount}件のココログを残しました",
                            color = KokologTextColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        if (summary.ignoredCount > 0) {
                            Text(
                                text = "📦 ${summary.ignoredCount}件のココロをそっと置きました",
                                color = KokologSubTextColor,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRankingText(
                            title = "多かったココロ",
                            items = summary.emotionCounts,
                            showEmotionIcon = false,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SummaryRankingText(
                            title = "多かった棚",
                            items = summary.categoryCounts
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ひとこと",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = KokologTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = summary.message,
                            color = KokologSubTextColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            summaryDialogType = null
                        }
                    ) {
                        Text(
                            text = "閉じる",
                            color = KokologButtonColor
                        )
                    }
                }
            )
        }
    }
}

/**
 * フィルター生成ラベル固定の横スクロール
 */
@Composable
fun FilterRowWithLabel(
    label: String,
    content: LazyListScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = KokologTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(90.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * 記録詳細画面
 *
 * 振り返り一覧で選択した感情ログの詳細を表示する
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    log: EmotionLog,
    onBack: () -> Unit,
    onDelete: (EmotionLog) -> Unit,
    onAddChange: (Long) -> Unit,
    changeLogs: List<EmotionChangeLog>,
    onDeleteChange: (EmotionChangeLog) -> Unit,
    onToggleIgnored: (EmotionLog) -> Unit,
    onEdit: () -> Unit
) {
    // 削除確認ダイアログを表示するかどうか
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    // 気にしない確認ダイアログを表示するかどうか
    var showIgnoreDialog by remember {
        mutableStateOf(false)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp),

        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 120.dp
        )
    ) {
        item {
            // 画面上部ヘッダー
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8DFC8)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    /*IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = Color(0xFF4E433B)
                        )
                    }*/

                    KokologHeader(
                        title = "あのときのココロ",
                        onBack = onBack,
                        onEdit = onEdit,
                        onDelete = {
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
        item {

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = emotionColor(log.emotion)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(
                                horizontal = 28.dp,
                                vertical = 32.dp
                            )
                            .background(Color(0xFFFFFBF4)),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .background(
                                    emotionAccentColor(log.emotion)
                                )
                        )
                        // 気にしないマーキング
                        if (log.isIgnored) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFE9E1D7)
                            ) {
                                Text(
                                    text = "\uD83D\uDCE6 そっと置いたココロ",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    color = KokologSubTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "日時：${formatDate(log.createdAt)}",
                                fontSize = 12.sp,
                                color = KokologSubTextColor
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFF3E8D7)
                            ) {
                                Text(
                                    text = log.category,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 6.dp
                                    ),
                                    fontSize = 12.sp,
                                    color = KokologTextColor
                                )
                            }

                        }
                        Spacer(
                            modifier = Modifier.height(15.dp)
                        )
                        EmotionLabel(
                            emotion = log.emotion,
                            fontSize = 28.sp
                        )
                        if (log.tags.isNotBlank()) {
                            Text(
                                text = log.tags
                                    .split(",")
                                    .joinToString(" ") { "#$it" },
                                color = KokologSubTextColor,
                                modifier = Modifier.padding(start = 4.dp),
                                lineHeight = 28.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HeartSizeLabel(
                            intensity = log.intensity
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (log.memo.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 15.dp)
                            ) {
                                Text(
                                    text = log.memo,
                                    color = Color(0xFF4E463F),
                                    fontSize = 18.sp,
                                    lineHeight = 28.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (log.isIgnored) {
                            // すでに「気にしない」済みの場合は、そのまま戻す
                            onToggleIgnored(log)
                        } else {
                            // まだ気にしているココロの場合は、確認ダイアログを出す
                            showIgnoreDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (log.isIgnored) {
                                KokologButtonColor
                            } else {
                                Color(0xFF9E9E9E)
                            }
                    )
                ) {
                    Text(
                        text =
                            if (log.isIgnored) {
                                "また気にする"
                            } else {
                                "気にしない"
                            },
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        onAddChange(log.id)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KokologButtonColor
                    )
                ) {
                    Text(
                        text = "そのあとを残す",
                        fontSize = 13.sp
                    )
                }
            }
        }
        if (changeLogs.isNotEmpty()) {
            item {
                KokologHeader(
                    title = "そのあとのココロ"
                )
            }

            items(changeLogs) { changeLog ->
                var showDeleteChangeDialog by remember(changeLog.id) {
                    mutableStateOf(false)
                }
                val isLatest =
                    changeLog == changeLogs.last()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // 左の点と線
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = Color(0xFFB7A18D),
                                    shape = CircleShape
                                )
                        )

                        if (!isLatest) {
                            Spacer(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(84.dp)
                                    .background(Color(0xFFD8CCBE))
                            )
                        }
                    }

                    // 右のカード
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = emotionColor(changeLog.emotion)
                        ),
                        border =
                            if (isLatest)
                                BorderStroke(
                                    1.dp,
                                    KokologButtonColor
                                )
                            else null
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text =
                                        if (isLatest) {
                                            "${
                                                formatElapsedTime(
                                                    originalTime = log.createdAt,
                                                    changeTime = changeLog.createdAt
                                                )
                                            }のココロ（さいしん）"
                                        } else {
                                            "${
                                                formatElapsedTime(
                                                    originalTime = log.createdAt,
                                                    changeTime = changeLog.createdAt
                                                )
                                            }のココロ"
                                        },
                                    fontSize = 14.sp,
                                    color = KokologSubTextColor
                                )

                                IconButton(
                                    onClick = {
                                        showDeleteChangeDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "そのあとのココロを削除",
                                        tint = KokologSubTextColor
                                    )
                                }
                            }

                            EmotionLabel(
                                emotion = changeLog.emotion,
                                fontSize = 20.sp
                            )

                            HeartSizeLabel(
                                intensity = changeLog.intensity
                            )

                            if (changeLog.memo.isNotBlank()) {
                                Text(
                                    text = changeLog.memo,
                                    color = Color(0xFF4E463F),
                                    fontSize = 18.sp,
                                    lineHeight = 28.sp,
                                )
                            }
                        }
                    }
                    if (showDeleteChangeDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showDeleteChangeDialog = false
                            },
                            title = {
                                Text(
                                    text = "この変化を手放しますか？",
                                    color = KokologTextColor
                                )
                            },
                            text = {
                                Text(
                                    text = "この操作は元に戻せません。",
                                    color = KokologSubTextColor
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteChangeDialog = false
                                        onDeleteChange(changeLog)
                                    }
                                ) {
                                    Text(
                                        text = "手放す",
                                        color = Color(0xFFB05A5A)
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteChangeDialog = false
                                    }
                                ) {
                                    Text(
                                        text = "キャンセル",
                                        color = KokologButtonColor
                                    )
                                }
                            },
                            containerColor = Color(0xFFF8F5F7)
                        )
                    }
                }
            }
        }
    }
    // showDeleteDialog が true のときだけ削除確認ダイアログを表示する
    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text(
                    text = "このココログを手放しますか？",
                    color = KokologTextColor
                )
            },

            text = {
                Text(
                    text = "手放したココログは、あとから戻せません。",
                    color = KokologSubTextColor
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(log)
                    }
                ) {
                    Text(
                        text = "手放す",
                        color = Color(0xFFB05A5A)
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },

            containerColor = Color(0xFFF8F5F7)
        )
    }
    if (showIgnoreDialog) {
        AlertDialog(
            onDismissRequest = {
                showIgnoreDialog = false
            },
            title = {
                Text(
                    text = "このココロを、そっと置いておきますか？",
                    color = KokologTextColor
                )
            },
            text = {
                Text(
                    text = "今はもう気にしないココロとして、振り返りでは少し控えめに表示されます。",
                    color = KokologSubTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 気にしない状態に変更
                        onToggleIgnored(log)

                        // ダイアログを閉じる
                        showIgnoreDialog = false

                        // 詳細画面から振り返り画面へ戻る
                        onBack()
                    }
                ) {
                    Text(
                        text = "そっと置く",
                        color = KokologButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIgnoreDialog = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7)
        )
    }
}

/**
 * 編集画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogScreen(
    log: EmotionLog,
    onBack: () -> Unit,
    onSave: (EmotionLog) -> Unit
) {
    val emotions = emotionUiList

    var selectedEmotion by remember {
        mutableStateOf(log.emotion)
    }

    var intensity by remember {
        mutableStateOf(log.intensity.toFloat())
    }

    var memo by remember {
        mutableStateOf(log.memo)
    }

    var selectedCategory by remember {
        mutableStateOf(log.category)
    }

    val selectedTags = remember {
        mutableStateListOf<String>().apply {
            addAll(
                log.tags
                    .split(",")
                    .filter { it.isNotBlank() }
            )
        }
    }

    var isIgnored by remember {
        mutableStateOf(log.isIgnored)
    }

    // 編集中の記録日時
    var selectedDateTimeMillis by remember {
        mutableStateOf(log.createdAt)
    }

    var dateTimeError by remember {
        mutableStateOf(false)
    }

    // 日付ピッカー
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    // 確認ダイアログ
    var showDiscardDialog by remember {
        mutableStateOf(false)
    }

    // 変更確認ダイアログ
    var showUpdateDialog by remember {
        mutableStateOf(false)
    }
    val categoryList = listOf(
        "仕事",
        "人間関係",
        "家族",
        "健康",
        "学校",
        "お金",
        "趣味",
        "その他"
    )

    val categoryTagMap = mapOf(
        "仕事" to listOf("会議", "残業", "ミス", "プレッシャー", "締切"),
        "人間関係" to listOf("上司", "友達", "SNS", "LINE", "気疲れ"),
        "家族" to listOf("子ども", "パートナー", "親", "家事", "予定"),
        "健康" to listOf("睡眠不足", "疲労", "体調不良", "通院", "運動"),
        "学校" to listOf("勉強", "テスト", "友達", "先生", "進路"),
        "お金" to listOf("出費", "節約", "投資", "収入", "将来不安"),
        "趣味" to listOf("ゲーム", "読書", "音楽", "創作", "リフレッシュ"),
        "その他" to emptyList()
    )

    val tagList = categoryTagMap[selectedCategory] ?: emptyList()
    // タグ自由入力
    var customTagText by remember {
        mutableStateOf("")
    }
    // 変更有無を判定
    val hasChanges =
        selectedEmotion != log.emotion ||
                selectedCategory != log.category ||
                selectedTags.joinToString(",") != log.tags ||
                intensity.toInt() != log.intensity ||
                memo != log.memo ||
                isIgnored != log.isIgnored ||
                selectedDateTimeMillis != log.createdAt
    val dateTimeButtonColor =
        if (dateTimeError) {
            Color(0xFFFFEAEA)
        } else {
            Color(0xFFD8CFC4)
        }

    val dateTimeTextColor =
        if (dateTimeError) {
            Color(0xFFB05A5A)
        } else {
            KokologTextColor
        }
    fun updateSelectedDateTime(newMillis: Long) {
        selectedDateTimeMillis = newMillis
        dateTimeError = newMillis > System.currentTimeMillis()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 20.dp,
                bottom = 200.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8DFC8)
            )
        ) {
            KokologHeader(
                title = "ココログを編集",
                onBack = {
                    if (hasChanges) {
                        showDiscardDialog = true
                    } else {
                        onBack()
                    }
                }
            )
        }
        Text(
            text = "残したココロを、少しだけ整えてみましょう。",
            color = KokologSubTextColor
        )

        // 日付
        Text(
            text = "このココログの日時",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        /*Text(
            text = formatKokologDateTime(selectedDateTimeMillis),
            color = if (dateTimeError) {
                Color(0xFFB05A5A)
            } else {
                KokologSubTextColor
            },
            fontSize = 14.sp
        )*/
        // 日時表示用
        val dateFormat = remember {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        }

        val timeFormat = remember {
            SimpleDateFormat("HH:mm", Locale.JAPAN)
        }

        val dateText = dateFormat.format(Date(selectedDateTimeMillis))
        val timeText = timeFormat.format(Date(selectedDateTimeMillis))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    showDatePicker = true
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dateTimeButtonColor,
                    contentColor = dateTimeTextColor
                ),
            ) {
                Text(
                    text = dateText,
                    color = dateTimeTextColor
                )
            }

            Button(
                onClick = {
                    showTimePicker = true
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dateTimeButtonColor,
                    contentColor = dateTimeTextColor
                ),
            ) {
                Text(
                    text = timeText,
                    color = dateTimeTextColor
                )
            }
        }
        // クイックボタン
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    "今日" to 0,
                    "昨日" to 1,
                    "一昨日" to 2
                )
            ) { item ->

                Button(
                    onClick = {
                        selectedDateTimeMillis  = quickDateTimeText(item.second)
                        dateTimeError = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4)
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
                ) {
                    Text(
                        text = item.first,
                        fontSize = 13.sp,
                        color = KokologTextColor
                    )
                }
            }
        }
        if (dateTimeError) {
            Text(
                text = "未来の日時は選べません。",
                color = Color(0xFFB05A5A),
                fontSize = 13.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(150.dp)
        ) {
            items(emotions) { emotionUi ->
                EmotionChip(
                    emotionUi = emotionUi,
                    isSelected = selectedEmotion == emotionUi.label,
                    onClick = {
                        selectedEmotion = emotionUi.label
                    }
                )
            }
        }

        Text(
            text = "スクロールすると下にもココロがあります",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = KokologLightTextColor
        )

        Text(
            text = "カテゴリ",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryList) { category ->
                Button(
                    onClick = {
                        selectedCategory = category
                        selectedTags.clear()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (selectedCategory == category) {
                                KokologButtonColor
                            } else {
                                Color(0xFFD8CFC4)
                            }
                    )
                ) {
                    Text(category)
                }
            }
        }

        Text(
            text = "タグ",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        if (tagList.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tagList) { tag ->
                    val isSelected = selectedTags.contains(tag)

                    Button(
                        onClick = {
                            if (isSelected) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.add(tag)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (isSelected) {
                                    KokologButtonColor
                                } else {
                                    Color(0xFFD8CFC4)
                                }
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = if (isSelected) "✓ $tag" else tag,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "タグを追加",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customTagText,
                onValueChange = {
                    customTagText = it
                },
                placeholder = {
                    Text(
                        text = "タグ名",
                        color = KokologLightTextColor
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = KokologTextColor,
                    unfocusedTextColor = KokologTextColor,
                    focusedBorderColor = Color(0xFFD8CFC4),
                    unfocusedBorderColor = Color(0xFFE9E1D7),
                    cursorColor = KokologButtonColor,
                    focusedContainerColor = Color(0xFFF8F5F7),
                    unfocusedContainerColor = Color(0xFFF8F5F7)
                )
            )

            Button(
                onClick = {
                    val newTag = customTagText.trim()

                    if (newTag.isNotBlank() && !selectedTags.contains(newTag)) {
                        selectedTags.add(newTag)
                        customTagText = ""
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KokologButtonColor
                )
            ) {
                Text("追加")
            }
        }

        if (selectedTags.isNotEmpty()) {
            Text(
                text = "選択中のタグ",
                fontWeight = FontWeight.Bold,
                color = KokologTextColor
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTags) { tag ->
                    Button(
                        onClick = {
                            selectedTags.remove(tag)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD8CFC4)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = "× $tag",
                            fontSize = 13.sp,
                            color = KokologTextColor
                        )
                    }
                }
            }
        }


        HeartSizeLabel_FULL(
            intensity = intensity.toInt()
        )

        Slider(
            value = intensity,
            onValueChange = {
                intensity = it
            },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = KokologButtonColor,
                activeTrackColor = KokologButtonColor,
                inactiveTrackColor = KokologBottomBarColor
            ),
            modifier = Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = memo,
            onValueChange = {
                memo = it
            },
            placeholder = {
                Text(
                    text = "今のココロを、少しだけ書いてみる",
                    color = KokologLightTextColor
                )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            minLines = 4,
            maxLines = 4,
            singleLine = false,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = KokologTextColor,
                unfocusedTextColor = KokologTextColor,
                focusedPlaceholderColor = KokologLightTextColor,
                unfocusedPlaceholderColor = KokologLightTextColor,
                focusedBorderColor = Color(0xFFD8CFC4),
                unfocusedBorderColor = Color(0xFFE9E1D7),
                cursorColor = KokologButtonColor,
                focusedContainerColor = Color(0xFFF8F5F7),
                unfocusedContainerColor = Color(0xFFF8F5F7)
            ),
            textStyle = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isIgnored = !isIgnored
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isIgnored,
                onCheckedChange = {
                    isIgnored = it
                }
            )

            Text(
                text = "このココロは、もう気にしない",
                color = KokologSubTextColor
            )
        }

        Button(
            enabled = !dateTimeError,
            onClick = {
                showUpdateDialog = true
            },
            modifier = Modifier
                .align(Alignment.End)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C8B7A)
            )
        ) {
            Text("へんこうを保管")
        }
    }
    if (showDatePicker) {

        val currentMillis = selectedDateTimeMillis

        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = currentMillis
            )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis =
                            datePickerState.selectedDateMillis

                        if (selectedMillis != null) {
                            val currentCalendar = Calendar.getInstance()
                            currentCalendar.timeInMillis = selectedDateTimeMillis

                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.timeInMillis = selectedMillis

                            selectedCalendar.set(
                                Calendar.HOUR_OF_DAY,
                                currentCalendar.get(Calendar.HOUR_OF_DAY)
                            )
                            selectedCalendar.set(
                                Calendar.MINUTE,
                                currentCalendar.get(Calendar.MINUTE)
                            )

                            updateSelectedDateTime(selectedCalendar.timeInMillis)

                        }

                        showDatePicker = false
                    }
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                    }
                ) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFFF8F5F7),
                    titleContentColor = KokologTextColor,
                    headlineContentColor = KokologTextColor,
                    weekdayContentColor = KokologSubTextColor,
                    subheadContentColor = KokologSubTextColor,
                    yearContentColor = KokologTextColor,
                    currentYearContentColor = KokologButtonColor,
                    selectedYearContainerColor = KokologButtonColor,
                    selectedYearContentColor = Color.White,
                    dayContentColor = KokologTextColor,
                    selectedDayContainerColor = KokologButtonColor,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = KokologButtonColor,
                    todayContentColor = KokologButtonColor
                )
            )
        }
    }
    if (showTimePicker) {

        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = selectedDateTimeMillis

        val timePickerState =
            rememberTimePickerState(
                initialHour = currentCalendar.get(Calendar.HOUR_OF_DAY),
                initialMinute = currentCalendar.get(Calendar.MINUTE),
                is24Hour = true
            )
        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newCalendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDateTimeMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        updateSelectedDateTime(newCalendar.timeInMillis)
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "決定",
                        color = KokologButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7),
            title = {
                Text(
                    text = "時間を選ぶ",
                    color = KokologTextColor
                )
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFFF8F5F7),

                        selectorColor = KokologButtonColor,

                        containerColor = Color(0xFFF8F5F7),

                        timeSelectorSelectedContainerColor = KokologButtonColor,
                        timeSelectorSelectedContentColor = Color.White,

                        timeSelectorUnselectedContainerColor = Color(0xFFE9E1D7),
                        timeSelectorUnselectedContentColor = KokologTextColor,

                        // 追加
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = KokologTextColor
                    )
                )
            }
        )
    }
    // 確認ダイアログ
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
            },
            title = {
                Text("へんこうを破棄しますか？")
            },
            text = {
                Text("保管していないへんこうがあります。このまま戻ると、編集内容は保管されません。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text("破棄して戻る")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                    }
                ) {
                    Text("編集を続ける")
                }
            }
        )
    }
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                showUpdateDialog = false
            },
            title = {
                Text(
                    text = if (isIgnored) {
                        "このココロを、そっと置いて保管しますか？"
                    } else {
                        "へんこうを保管しますか？"
                    },
                    color = KokologTextColor
                )
            },
            text = {
                Text(
                    text = if (isIgnored) {
                        "今はもう気にしないココロとして、振り返りでは少し控えめに表示されます。"
                    } else {
                        "編集した内容を保管します。"
                    },
                    color = KokologSubTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false

                        // 今の「変更を保管」ボタン内の保存処理をここへ移動
                        if (isFutureDateTime(selectedDateTimeMillis)) {
                            dateTimeError = true
                            showUpdateDialog = false
                            return@TextButton
                        }

                        val updatedLog = log.copy(
                            emotion = selectedEmotion,
                            category = selectedCategory,
                            tags = selectedTags.joinToString(","),
                            isIgnored = isIgnored,
                            intensity = intensity.toInt(),
                            memo = memo,
                            createdAt = selectedDateTimeMillis
                        )

                        onSave(updatedLog)
                    }
                ) {
                    Text(
                        text = "保管する",
                        color = KokologButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7)
        )
    }
}

/**
 * 設定画面
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showRecentLogs: Boolean,
    onShowRecentLogsChange: (Boolean) -> Unit,
    returnHomeAfterSave: Boolean,
    onReturnHomeAfterSaveChange: (Boolean) -> Unit,
    showIgnoredLogs: Boolean,
    onShowIgnoredLogsChange: (Boolean) -> Unit,
    notificationEnabled: Boolean,
    onNotificationEnabledChange: (Boolean) -> Unit,
    notificationHour: Int,
    notificationMinute: Int,
    onNotificationTimeChange: (Int, Int) -> Unit
) {
    var showNotificationTimePicker by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        KokologHeader(
            title = "設定"
        )

        SettingsSectionTitle(
            title = "表示設定"
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "最近のココログを表示",
                    color = KokologTextColor
                )

                Switch(
                    checked = showRecentLogs,
                    onCheckedChange = onShowRecentLogsChange
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "気にしないココログを表示",
                    color = KokologTextColor
                )

                Switch(
                    checked = showIgnoredLogs,
                    onCheckedChange = onShowIgnoredLogsChange
                )
            }
        }

        SettingsSectionTitle(
            title = "記録設定"
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "保存後にホームへ戻る",
                    color = KokologTextColor
                )

                Switch(
                    checked = returnHomeAfterSave,
                    onCheckedChange = onReturnHomeAfterSaveChange
                )
            }


        }
        SettingsSectionTitle(
            title = "通知設定"
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "通知する",
                    color = KokologTextColor
                )

                Switch(
                    checked = notificationEnabled,
                    onCheckedChange = { checked ->
                        onNotificationEnabledChange(checked)
                    }
                )
            }
            if (notificationEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showNotificationTimePicker = true
                        }
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "通知時刻",
                        color = KokologTextColor
                    )

                    Text(
                        text = "%02d:%02d".format(
                            notificationHour,
                            notificationMinute
                        ),
                        color = KokologSubTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SettingsSectionTitle(
            title = "アプリ情報"
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFFBF4)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "ココログ",
                        fontWeight = FontWeight.Bold,
                        color = KokologTextColor
                    )

                    Text(
                        text = "Version 1.0.0",
                        color = KokologSubTextColor
                    )
                }
            }
        }
    }
    if (showNotificationTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = {
                showNotificationTimePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNotificationTimeChange(
                            timePickerState.hour,
                            timePickerState.minute
                        )

                        showNotificationTimePicker = false

                    }
                ) {
                    Text(
                        text = "決定",
                        color = KokologButtonColor
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationTimePicker = false
                    }
                ) {
                    Text(
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7),
            title = {
                Text(
                    text = "通知時刻を選ぶ",
                    color = KokologTextColor
                )
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFFF8F5F7),

                        selectorColor = KokologButtonColor,

                        containerColor = Color(0xFFF8F5F7),

                        timeSelectorSelectedContainerColor = KokologButtonColor,
                        timeSelectorSelectedContentColor = Color.White,

                        timeSelectorUnselectedContainerColor = Color(0xFFE9E1D7),
                        timeSelectorUnselectedContentColor = KokologTextColor,

                        // 追加
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = KokologTextColor
                    )
                )
            }
        )
    }
}

fun formatElapsedTime(
    originalTime: Long,
    changeTime: Long
): String {

    val diffMillis = changeTime - originalTime

    val minutes = diffMillis / (1000 * 60)
    val hours = diffMillis / (1000 * 60 * 60)
    val days = diffMillis / (1000 * 60 * 60 * 24)

    return when {

        minutes < 60 ->
            "${minutes}分後"

        hours < 24 ->
            "${hours}時間後"

        else ->
            "${days}日後"
    }
}

/**
 * 本棚カード上部の帯色
 * カード本体色より少し濃くして視認性を上げる
 */
fun emotionAccentColor(
    emotion: String
): Color {
    return when (emotion) {
        "うれしい" ->
            Color(0xFFE7C85C)

        "あんしん" ->
            Color(0xFF8EBB8E)

        "すっきり" ->
            Color(0xFF7EC7B8)

        "ふつう" ->
            Color(0xFFB7A98A)

        "もやもや" ->
            Color(0xFF9B8FB3)

        "ふあん" ->
            Color(0xFF7E93C7)

        "かなしい" ->
            Color(0xFF6FA7D8)

        "あせり" ->
            Color(0xFFD89A6F)

        "いらいら" ->
            Color(0xFFD86F6F)

        "つかれた" ->
            Color(0xFF9E9E9E)

        "しんぱい" ->
            Color(0xFFB8B1CC)

        "たのしい" ->
            Color(0xFFFFC089)

        else ->
            Color.Gray
    }
}

/**
 * Navigationで使う画面名
 */
object Routes {
    const val HOME = "home"
    const val RECORD = "record"
    const val REFLECTION = "reflection"
    const val SETTINGS = "settings"

    // 詳細画面
    const val DETAIL = "detail"
    const val CHANGE = "change"
    const val EDIT = "edit"
}

fun formatYear(
    timeMillis: Long
): String {
    val formatter =
        java.text.SimpleDateFormat(
            "yyyy",
            java.util.Locale.JAPAN
        )

    return formatter.format(
        java.util.Date(timeMillis)
    )
}

fun formatMonthDay(
    timeMillis: Long
): String {
    val formatter =
        java.text.SimpleDateFormat(
            "M/d",
            java.util.Locale.JAPAN
        )

    return formatter.format(
        java.util.Date(timeMillis)
    )
}

@Composable
fun HomeTagSection(
    tags: String
) {

    if (tags.isBlank()) {
        return
    }

    val tagList =
        tags.split(",")
            .filter { it.isNotBlank() }

    FlowRow(
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),

        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        tagList.take(3).forEach { tag ->

            Surface(
                shape = RoundedCornerShape(50),

                color = Color(0xFFF3E8D7)
            ) {

                Text(
                    text = tag,

                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),

                    fontSize = 11.sp,

                    color = KokologTextColor
                )
            }
        }
    }
}

/**
 * 日時変換
 */
fun formatEditableDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat(
        "yyyy/MM/dd HH:mm",
        Locale.getDefault()
    )

    return sdf.format(Date(timestamp))
}

fun parseEditableDateTime(text: String): Long? {
    return try {
        val sdf = SimpleDateFormat(
            "yyyy/MM/dd HH:mm",
            Locale.getDefault()
        )

        sdf.isLenient = false

        sdf.parse(text)?.time
    } catch (e: Exception) {
        null
    }
}

/**
 * カレンダー
 */

fun quickDateTimeText(daysAgo: Int): Long {
    val calendar = Calendar.getInstance()

    calendar.add(Calendar.DATE, -daysAgo)

    return calendar.timeInMillis
}

data class KokoroSummary(
    val title: String,
    val totalCount: Int,
    val ignoredCount: Int,
    val topEmotion: String?,
    val topEmotionIcon: String,
    val emotionCounts: List<Pair<String, Int>>,
    val categoryCounts: List<Pair<String, Int>>,
    val message: String
)

/**
 * サマリ集計機能
 */
fun createKokoroSummary(
    logs: List<EmotionLog>,
    type: String
): KokoroSummary {
    val nowCalendar = Calendar.getInstance()

    val startCalendar = Calendar.getInstance().apply {
        when (type) {
            "week" -> {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            "month" -> {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    val startMillis = startCalendar.timeInMillis
    val nowMillis = nowCalendar.timeInMillis

    val targetLogs = logs.filter { log ->
        log.createdAt in startMillis..nowMillis
    }

    val ignoredCount = targetLogs.count { it.isIgnored }

    val emotionCounts =
        targetLogs
            .groupingBy { it.emotion }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

    val categoryCounts =
        targetLogs
            .groupingBy { it.category }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

    val topEmotion = emotionCounts.firstOrNull()?.first

    val topEmotionIcon =
        emotionUiList
            .firstOrNull { it.label == topEmotion }
            ?.icon ?: "🌿"

    val message = createSummaryMessage(
        totalCount = targetLogs.size,
        topEmotion = topEmotion,
        topCategory = categoryCounts.firstOrNull()?.first
    )

    return KokoroSummary(
        title = if (type == "week") {
            "今週のココロ"
        } else {
            "今月のココロ"
        },
        totalCount = targetLogs.size,
        ignoredCount = ignoredCount,
        topEmotion = topEmotion,
        topEmotionIcon = topEmotionIcon,
        emotionCounts = emotionCounts,
        categoryCounts = categoryCounts,
        message = message
    )
}

fun createSummaryMessage(
    totalCount: Int,
    topEmotion: String?,
    topCategory: String?
): String {
    if (totalCount == 0) {
        return listOf(
            "まだココロは残っていません。まずはひとつ、そっと置いてみましょう。",
            "記録はまだありません。気が向いたときに、今のココロを残してみてください。",
            "まだ白紙の期間です。これから少しずつ、ココロの足あとを残していけます。",
            "今はまだ記録がありません。最初のひとつを、やさしく始めてみましょう。",
            "ココロはまだ置かれていません。無理なく、書けるときに残せば大丈夫です。"
        ).random()
    }

    val emotionMessages = mapOf(
        "たのしい" to listOf(
            "楽しい時間がココロに残っていました。",
            "ワクワクする出来事があった期間かもしれません。",
            "気分の上がる瞬間を、ちゃんと残せていたようです。",
            "楽しさを味わえた記録が残っています。",
            "笑顔になれる時間が、少し多かったようです。",
            "楽しい時間が日々を彩っていました。",
            "心が弾む出来事があったようです。",
            "わくわくする瞬間を残せていました。",
            "楽しい気持ちは明日の元気になります。",
            "思わず笑顔になる出来事があったようですね。"
        ),
        "うれしい" to listOf(
            "うれしいココロが多い期間でした。",
            "小さなよかったことが積み重なっていたようです。",
            "喜べる出来事を、ちゃんと受け取れていたようです。",
            "うれしい気持ちを残せているのは素敵なことです。",
            "前向きなココロが多く残っていました。"
        ),
        "あんしん" to listOf(
            "安心できる時間が多かったようです。",
            "ほっとできる瞬間がココロに残っています。",
            "落ち着いて過ごせる場面があったのかもしれません。",
            "穏やかなココロを残せていた期間です。",
            "安心できる場所や人が、支えになっていたのかもしれません。",
            "心をゆるめられる出来事があったようです。",
            "落ち着いたココロが印象的でした。",
            "安心感を感じられる毎日は大切ですね。"
        ),
        "すっきり" to listOf(
            "すっきりしたココロが多かったようです。",
            "整理できたことがあった期間かもしれません。",
            "気持ちを切り替えられた場面が残っています。",
            "少し軽くなったココロを記録できていました。",
            "前に進む感覚があったのかもしれません。",
            "気持ちを整理できた場面があったようです。",
            "前に進めた感覚が残っています。",
            "すっきりしたココロが印象的でした。",
            "モヤモヤを手放せた瞬間があったようです。",
            "軽やかな気持ちの日がありました。"
        ),
        "ふつう" to listOf(
            "大きく揺れすぎず、穏やかに過ごせていた期間かもしれません。",
            "ふつうの日々も、ちゃんと大切な記録です。",
            "安定したココロが多く残っていました。",
            "何気ない日常を残せているのも、ココログらしい記録です。",
            "大きな波は少なく、落ち着いた期間だったのかもしれません。"
        ),
        "もやもや" to listOf(
            "もやもやしたココロが少し多かったようです。",
            "言葉にしにくい気持ちを、ちゃんと残せていました。",
            "はっきりしない気持ちも、置いておくだけで意味があります。",
            "考えがまとまりにくい期間だったのかもしれません。",
            "もやもやを記録できたこと自体が、ひとつの整理になっています。"
        ),
        "ふあん" to listOf(
            "不安なココロが残っていました。",
            "先のことを考える時間が多かったのかもしれません。",
            "不安を抱えながらも、ちゃんと日々を過ごしていたようです。",
            "気がかりなことを、少しずつ整理していけるとよさそうです。",
            "不安を消そうとせず、残せているだけでも十分です。"
        ),
        "しんぱい" to listOf(
            "誰かや何かを気にかけるココロが多かったようです。",
            "大切に思っているものがあるからこその心配かもしれません。",
            "心配な気持ちを、ちゃんと残せていました。",
            "自分以外のことにも気持ちを向けていた期間のようです。",
            "気にかける優しさが、記録にも表れているようです。"
        ),
        "かなしい" to listOf(
            "かなしいココロが残っていました。",
            "無理に消さず、そっと置いておいて大丈夫です。",
            "つらさを言葉にできたことも、大切な記録です。",
            "少し重たい気持ちを抱えていた期間かもしれません。",
            "かなしい気持ちも、ちゃんとココログに置けています。"
        ),
        "あせり" to listOf(
            "焦るココロが多かったようです。",
            "急がなきゃと思う場面が多かったのかもしれません。",
            "少し立ち止まる時間も、大切にできるとよさそうです。",
            "やることや考えることが重なっていた期間かもしれません。",
            "焦りを残せたことで、自分のペースに気づけるかもしれません。"
        ),
        "いらいら" to listOf(
            "いらいらしたココロが残っていました。",
            "疲れや我慢がたまっていたのかもしれません。",
            "気持ちが尖りやすい場面があったようです。",
            "いらいらも、ちゃんと理由のあるココロです。",
            "少し休む時間を取れると、ココロがほどけるかもしれません。"
        ),
        "つかれた" to listOf(
            "疲れたココロが多かったようです。",
            "がんばった分だけ、疲れも残っていたのかもしれません。",
            "休むことも、ちゃんと前に進むための時間です。",
            "体やココロが休憩を求めていた期間かもしれません。",
            "疲れを記録できたことは、自分を守るヒントになります。"
        )
    )

    val categoryMessages = mapOf(
        "仕事" to listOf(
            "仕事のことに向き合う時間が多かったようです。",
            "仕事がココロを動かす場面が多かったのかもしれません。",
            "がんばった記録が、しっかり残っています。",
            "仕事に意識が向いていた期間だったようです。",
            "忙しさの中でも、ココロを残せていました。"
        ),
        "人間関係" to listOf(
            "人との関わりの中で、ココロが動いていたようです。",
            "誰かとのやり取りが印象に残る期間だったのかもしれません。",
            "人間関係の中で感じたことを、ちゃんと残せています。",
            "人との距離感を考える場面があったのかもしれません。",
            "関わりの中で生まれたココロが記録されています。"
        ),
        "家族" to listOf(
            "家族のことを大切に思う気持ちが表れているようです。",
            "家族との時間がココロに残っていました。",
            "身近な人との出来事が多かった期間かもしれません。",
            "家族に向いたココロが、記録にも残っています。",
            "日々の家族との関わりが、ちゃんと足あとになっています。"
        ),
        "健康" to listOf(
            "体や調子に意識が向いていたようです。",
            "無理せず整える時間も持てるとよさそうです。",
            "健康のことを気にかける記録が残っています。",
            "体調や疲れに向き合う期間だったのかもしれません。",
            "自分のコンディションに気づけている記録です。"
        ),
        "学校" to listOf(
            "学校に関するココロが多かったようです。",
            "学びや人との関わりが残る期間だったのかもしれません。",
            "学校での出来事がココロに残っていました。",
            "がんばった自分も、少し認めてあげてください。",
            "日々の積み重ねが、記録にも表れています。"
        ),
        "お金" to listOf(
            "お金に関することを考える時間が多かったようです。",
            "将来や生活のことに意識が向いていたのかもしれません。",
            "焦らず、少しずつ整理していきましょう。",
            "お金のことを記録できるのは、大事な自己理解です。",
            "不安や期待も含めて、ちゃんと残せています。"
        ),
        "趣味" to listOf(
            "好きなことに触れる時間が、ココロにも残っているようです。",
            "趣味の時間が気持ちを動かしていたのかもしれません。",
            "自分の好きなものを大切にできていた期間です。",
            "リフレッシュできる時間が記録されています。",
            "楽しみの足あとが、ちゃんと残っています。"
        ),
        "その他" to listOf(
            "いろいろな出来事が、今のココロにつながっていたようです。",
            "分類しきれない気持ちも、大切な記録です。",
            "日々の中の小さな出来事が残っています。",
            "いろいろなココロを、そっと置けていました。",
            "何気ない記録にも、自分らしさが表れています。"
        )
    )

    return emotionMessages[topEmotion]?.random()
        ?: categoryMessages[topCategory]?.random()
        ?: "残したココロを眺めることで、少しずつ自分の傾向が見えてきます。"
}

@Composable
fun SummaryRankingText(
    title: String,
    items: List<Pair<String, Int>>,
    showEmotionIcon: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = KokologTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        if (items.isEmpty()) {
            Text(
                text = "まだ保管したココログがありません",
                color = KokologSubTextColor,
                fontSize = 13.sp,
            )
        } else {
            items.take(3).forEachIndexed { index, item ->
                val medals = listOf("🥇", "🥈", "🥉")

                val iconText =
                    if (showEmotionIcon) {
                        emotionUiList
                            .firstOrNull { it.label == item.first }
                            ?.icon
                            ?.let { "$it " } ?: ""
                    } else {
                        ""
                    }

                Text(
                    text = "${medals[index]} $iconText${item.first} ${item.second}件",
                    color = KokologSubTextColor,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}