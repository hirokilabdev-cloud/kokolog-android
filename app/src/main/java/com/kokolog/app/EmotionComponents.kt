package com.kokolog.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

/**
 * 感情表示用コンポーネント
 */
@Composable
fun EmotionLabel(
    emotion: String,
    fontSize: TextUnit = 22.sp,
    color: Color = Color(0xFF4E463F)
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${emotionIcon(emotion)} $emotion",

            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = color
            ),
            modifier = Modifier.padding(start = 15.dp)
        )
    }
}

/**
 * こころの大きさ表示用コンポーネント
 */
@Composable
fun HeartSizeLabel(
    intensity: Int,
    fontSize: TextUnit = 18.sp,
    color: Color = Color(0xFF4E463F)
) {
    Text(
        text = "大きさ：${heartSizeText(intensity)}",
        fontSize = fontSize,
        color = color,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/**
 * こころの大きさ表示用コンポーネント
 */
@Composable
fun HeartSizeLabel_FULL(
    intensity: Int,
    fontSize: TextUnit = 18.sp,
    color: Color = Color(0xFF4E463F)
) {
    Text(
        text = "ココロの大きさ：${heartSizeText(intensity)}",
        fontSize = fontSize,
        color = color
    )
}



/**
 * ココログ表示用カード
 */
@Composable
fun KokologCard(
    log: EmotionLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(20.dp),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),

        colors = CardDefaults.cardColors(
            containerColor =
                if (log.isIgnored) {
                    Color(0xFFE0E0E0)
                } else {
                    emotionColor(log.emotion)
                }
        )
    ) {
        Row {
            /*Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(190.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp
                        )
                    )
                    .background(
                        color =
                            if (log.isIgnored) {
                                Color(0xFF9E9E9E)
                            } else {
                                emotionAccentColor(log.emotion)
                            }
                    )
            )*/
            Column(
                modifier = Modifier.padding(
                    horizontal = 22.dp,
                    vertical = 16.dp
                )
            ) {

                // 上部帯
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

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // 日付
                    Text(
                        text = formatDate(log.createdAt),

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
                    modifier = Modifier.height(12.dp)
                )

                // 感情
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            "${emotionIcon(log.emotion)} ${log.emotion}",

                        fontSize = 28.sp,

                        fontWeight = FontWeight.Bold,

                        color = KokologTextColor
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // タグ
                HomeTagSection(
                    tags = log.tags
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                // 下部行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "♡${log.intensity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = KokologButtonColor
                    )

                    Text(
                        text = "このココロを見る 🔍",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = KokologButtonColor
                    )
                }
            }
        }
    }
}
/**
 * 感情選択用チップ
 *
 * 記録画面で感情を選ぶための共通UI
 */
@Composable
fun EmotionChip(
    emotionUi: EmotionUi,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,

        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) KokologTextColor else Color.Transparent,
                shape = RoundedCornerShape(50)
            ),

        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (isSelected) {
                    emotionUi.color
                } else {
                    emotionUi.color.copy(alpha = 0.5f)
                }
        )
    ) {
        Text(
            text =
                if (isSelected) {
                    "✓ ${emotionUi.icon} ${emotionUi.label}"
                } else {
                    "${emotionUi.icon} ${emotionUi.label}"
                },

            color =
                if (isSelected) {
                    Color(0xFF3A3A3A)
                } else {
                    Color(0xFF666666)
                }
        )
    }
}

/**
 * ホーム画面以外で使う共通ヘッダー
 *
 * 戻るボタンや削除ボタンが必要な画面にも使える
 */
@Composable
fun KokologHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8DFC8)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = Color(0xFF4E433B)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KokologTextColor
            )

            if (onEdit != null) {
                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "編集",
                        tint = Color(0xFF4E433B)
                    )
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = Color(0xFF4E433B)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

/**
 * 0件時のカード
 */
@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    verticalPadding: Dp = 5.dp
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBF4)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = verticalPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.empty_bookshelf),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KokologTextColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                fontSize = 15.sp,
                color = KokologSubTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = KokologSubTextColor
    )
}


/**
 * 背表紙カードを作る
 */
@Composable
fun BookshelfSpineCard(
    log: EmotionLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val isPressed by interactionSource.collectIsPressedAsState()
    Card(
        modifier = modifier
            .width(96.dp)
           // .height(180.dp)
            .graphicsLayer {

                scaleX =
                    if (isPressed) 0.96f else 1f

                scaleY =
                    if (isPressed) 0.96f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (log.isIgnored) {
                    Color(0xFFE7E1D8)
                } else {
                    emotionColor(log.emotion)
                }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            // 上部の色帯
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color =
                            if (log.isIgnored) {
                                Color(0xFFE7E1D8)
                            } else {
                                emotionAccentColor(log.emotion)
                            },
                        shape = RoundedCornerShape(12.dp)
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            // 日付エリア
            Column(
                modifier = Modifier.height(50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatYear(log.createdAt),
                    fontSize = 8.sp,
                    color = KokologSubTextColor
                )

                Text(
                    text = formatMonthDay(log.createdAt),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KokologTextColor
                )
            }

            // 感情マーク
            Text(
                text = emotionIcon(log.emotion),
                fontSize = 22.sp,
                modifier = Modifier.height(28.dp)
            )

            // 縦書き風ラベル
            Column(
                modifier = Modifier.height(70.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                log.emotion.forEach { char ->
                    Text(
                        text = char.toString(),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KokologTextColor
                    )
                }
            }

            // こころの大きさ
            Text(
                text = "♡${log.intensity}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KokologTextColor,
                modifier = Modifier.height(18.dp)
            )

            // タグ表示
            Box(
                modifier = Modifier.height(42.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                BookshelfTagText(
                    tags = log.tags
                )
            }
        }
    }
}

/**
 * タグ表示用
 */
@Composable
fun BookshelfTagText(
    tags: String
) {
    if (tags.isBlank()) {
        Spacer(
            modifier = Modifier.height(8.dp)
        )
        return
    }

    val tagList =
        tags.split(",")
            .filter { it.isNotBlank() }

    val displayTags =
        tagList.take(1)

    val remainingCount =
        tagList.size - displayTags.size

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        displayTags.forEach { tag ->
            Text(
                text = tag,
                fontSize = 9.sp,
                color = KokologSubTextColor,
                maxLines = 1
            )
        }

        if (remainingCount > 0) {
            Text(
                text = "+$remainingCount",
                fontSize = 9.sp,
                color = KokologSubTextColor,
                maxLines = 1
            )
        }
    }
}

