package com.kokolog.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 振り返りで使う「棚の一覧」を描画する共通部品。
 *
 * 受け取った logs のみ表示するため、フィルター条件をこの部品は知らない。
 * 従来のカテゴリー別一覧だけでなく、今後の「日付」「月×カテゴリー」からも再利用できる。
 *
 * @param logs すでに条件で絞り込まれたログ一覧（新しい順を想定）
 * @param onLogClick 背表紙を押したときの処理。遷移先の決定は呼び出し側が担当する
 */
@Composable
fun BookshelfList(
    logs: List<EmotionLog>,
    onLogClick: (EmotionLog) -> Unit
) {
    // 棚の順番は既存実装と同じ。未定義のカテゴリーは末尾に並べる。
    val categoryOrder = listOf(
        "仕事", "人間関係", "家族", "健康", "学校", "お金", "趣味", "その他"
    )

    // 既存のカテゴリー単位の棚分けと表示順を維持する。
    val groupedLogs = logs
        .groupBy { log -> log.category }
        .toList()
        .sortedBy { item ->
            val index = categoryOrder.indexOf(item.first)
            if (index == -1) Int.MAX_VALUE else index
        }

    if (logs.isEmpty()) {
        EmptyStateCard(
            title = "条件に合うココログがありません",
            message = "別の棚や期間も、そっと見てみましょう",
            topPadding = 4.dp,
            verticalPadding = 2.dp
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
                        // 各カテゴリー内の記録を、従来どおり横一列に配置する。
                        items(logsInCategory) { log ->
                            BookshelfSpineCard(
                                log = log,
                                onClick = {
                                    onLogClick(log)
                                },
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(250.dp)
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
