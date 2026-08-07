package com.kokolog.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import java.util.Calendar

/**
 * 感情変化記録画面
 *
 * 既存のココログに対して、
 * 「その後、感情がどう変わったか」を記録する画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionChangeScreen(
    originalLogId: Long,
    originalCreatedAt: Long,
    onSave: (EmotionChangeLog) -> Unit,
    onBack: () -> Unit
) {
    val emotions = emotionUiList

    var selectedEmotion by remember {
        mutableStateOf("ふつう")
    }

    var intensity by remember {
        mutableStateOf(3f)
    }

    var memo by remember {
        mutableStateOf("")
    }

    var selectedDateTimeMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var dateTimeError by remember {
        mutableStateOf(false)
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    var showSaveDialog by remember {
        mutableStateOf(false)
    }

    val dateFormat = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    }

    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.JAPAN)
    }

    val dateText = dateFormat.format(Date(selectedDateTimeMillis))
    val timeText = timeFormat.format(Date(selectedDateTimeMillis))

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

    var dateTimeErrorMessage by remember {
        mutableStateOf("")
    }
    fun updateSelectedDateTime(newMillis: Long) {
        selectedDateTimeMillis = newMillis

        when {
            newMillis > System.currentTimeMillis() -> {
                dateTimeError = true
                dateTimeErrorMessage = "未来の日時は選べません。"
            }

            newMillis < originalCreatedAt -> {
                dateTimeError = true
                dateTimeErrorMessage = "元のココログより前の日時は選べません。"
            }

            else -> {
                dateTimeError = false
                dateTimeErrorMessage = ""
            }
        }
    }

    fun setAfterMinutes(minutes: Int) {
        val newMillis =
            originalCreatedAt + minutes * 60 * 1000L

        updateSelectedDateTime(newMillis)
    }

    fun setAfterHours(hours: Int) {
        val newMillis =
            originalCreatedAt + hours * 60 * 60 * 1000L

        updateSelectedDateTime(newMillis)
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
                title = "そのあとのココロ",
                onBack = onBack
            )
        }

        Text(
            text = "あのあと、ココロはどう変わりましたか？",
            color = KokologSubTextColor
        )

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
            )
        )

        Text(
            text = "変化した日時",
            fontWeight = FontWeight.Bold,
            color = KokologTextColor
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Button(
                    onClick = {
                        setAfterMinutes(5)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4),
                        contentColor = KokologTextColor
                    )
                ) {
                    Text("5分後")
                }
            }

            item {
                Button(
                    onClick = {
                        setAfterMinutes(30)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4),
                        contentColor = KokologTextColor
                    )
                ) {
                    Text("30分後")
                }
            }

            item {
                Button(
                    onClick = {
                        setAfterHours(1)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4),
                        contentColor = KokologTextColor
                    )
                ) {
                    Text("1時間後")
                }
            }

            item {
                Button(
                    onClick = {
                        setAfterHours(12)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4),
                        contentColor = KokologTextColor
                    )
                ) {
                    Text("半日後")
                }
            }

            item {
                Button(
                    onClick = {
                        setAfterHours(24)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8CFC4),
                        contentColor = KokologTextColor
                    )
                ) {
                    Text("翌日")
                }
            }
        }

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
                Text(dateText)
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
                Text(timeText)
            }
        }

        if (dateTimeError) {
            Text(
                text = dateTimeErrorMessage,
                color = Color(0xFFB05A5A),
                fontSize = 13.sp
            )
        }

        OutlinedTextField(
            value = memo,
            onValueChange = {
                memo = it
            },
            placeholder = {
                Text(
                    text = "変わったきっかけや、今の気持ちを少しだけ",
                    color = KokologLightTextColor
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            minLines = 4,
            maxLines = 4,
            singleLine = false,
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
            enabled =!dateTimeError,
            onClick = {
                showSaveDialog = true
            },
            modifier = Modifier
                .align(Alignment.End)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KokologButtonColor
            )
        ) {
            Text("そのあとのココロを残す")
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

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
            },
            title = {
                Text(
                    text = "そのあとのココロを残しますか？",
                    color = KokologTextColor
                )
            },
            text = {
                Text(
                    text = "変化したココロとして保存します。",
                    color = KokologSubTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dateTimeError) {
                            showSaveDialog = false
                            return@TextButton
                        }

                        val changeLog = EmotionChangeLog(
                            originalLogId = originalLogId,
                            emotion = selectedEmotion,
                            intensity = intensity.toInt(),
                            memo = memo,
                            createdAt = selectedDateTimeMillis
                        )

                        showSaveDialog = false
                        onSave(changeLog)
                    }
                ) {
                    Text(
                        text = "保存",
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
                        text = "キャンセル",
                        color = KokologButtonColor
                    )
                }
            },
            containerColor = Color(0xFFF8F5F7)
        )
    }
}