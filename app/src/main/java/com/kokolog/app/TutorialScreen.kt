package com.kokolog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialScreen(
    onFinish: () -> Unit
) {
    var page by remember { mutableStateOf(0) }

    val titles = listOf(
        "ようこそ",
        "ココロを残す",
        "振り返るためじゃなく",
        "はじめましょう"
    )

    val messages = listOf(
        "ココログへようこそ。\n\nここは今の気持ちを\nそっと残しておく場所です。",
        "うれしい日も\nもやもやする日も。\n\nどんな気持ちでも\nそのまま残して大丈夫です。",
        "あとから見返すためだけではなく\n\n今の気持ちを\n少しだけ置いておくために。",
        "では、最初のココログを\n残してみましょう。\n\n正解も間違いもありません。\n今の気持ちのままで大丈夫です。"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KokologBackgroundColor)
            .padding(24.dp)
    ) {
        TextButton(
            onClick = onFinish,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = "スキップ",
                color = KokologSubTextColor,
                fontWeight = FontWeight.Medium
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌱",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = titles[page],
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = KokologTextColor
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = messages[page],
                fontSize = 17.sp,
                lineHeight = 28.sp,
                color = KokologTextColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(titles.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                if (index == page) 12.dp else 8.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (index == page) {
                                    KokologButtonColor
                                } else {
                                    Color(0xFFE0D6C8)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    if (page < titles.lastIndex) {
                        page++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KokologButtonColor
                )
            ) {
                Text(
                    text = if (page < titles.lastIndex) {
                        "次へ"
                    } else {
                        "今の気持ちを残す"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}