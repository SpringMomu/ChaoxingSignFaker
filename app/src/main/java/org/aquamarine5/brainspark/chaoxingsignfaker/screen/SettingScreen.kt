/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CustomizeClientCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SnackbarAlertDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.RecommendHabit
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.FontGilroy
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.OnlyAppDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.disableComposableCode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode

@Serializable
object SettingGraphDestination

@Serializable
object SettingDestination

@OnlyAppDevelopedMode
private const val COMMAND_ALWAYS_FORCE_SIGN_PREFIX = "alwaysForceSign "

var isAlwaysForceSign by mutableStateOf(false)

@Composable
fun SettingScreen(
    naviToLoginScreen: () -> Unit,
    naviToFavoriteLocationSetting: () -> Unit = {}
) {
    val imageLoader = LocalImageLoader.current
    Column(
        modifier = Modifier
            .padding(16.dp, 4.dp, 16.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        var isRecommendEnabled by remember { mutableStateOf(true) }
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val userEntity = remember { ChaoxingHttpClient.instance!!.userEntity }
        val displayUserEntity =
            (ChaoxingHttpClient.cloneInstance ?: ChaoxingHttpClient.instance!!).userEntity
        var isShowSignoffDialog by remember { mutableStateOf(false) }
        val allRecommendHabits = remember { mutableStateListOf<RecommendHabit>() }
        var isCommandDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            context.chaoxingDataStore.data.first().apply {
                isRecommendEnabled = disableRecommend.not()
                allRecommendHabits.addAll(recommendHabitsList)
            }
        }
        if (isCommandDialog) {
            var inputPassword by remember { mutableStateOf("") }
            SnackbarAlertDialog(onDismissRequest = {
                isCommandDialog = false
            }, title = {
                Text("输入开发命令：")
            }, text = {
                TextField(inputPassword, onValueChange = {
                    inputPassword = it
                }, label = {
                    Text("命令")
                })
            }, dismissButton = {
                OutlinedButton(onClick = {
                    isCommandDialog = false
                }) {
                    Text("取消")
                }
            }, confirmButton = {
                Button(onClick = {
                    if (inputPassword.startsWith(COMMAND_ALWAYS_FORCE_SIGN_PREFIX)) {
                        inputPassword.substringAfter(COMMAND_ALWAYS_FORCE_SIGN_PREFIX)
                            .toBooleanStrictOrNull()
                            ?.let { value ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setPreferences(
                                            it.preferences.toBuilder()
                                                .setAlwaysForceSign(value)
                                        ).build()
                                    }
                                    snackbarHostState.displaySnackbar(
                                        "已设置${if (value) "总是强制签到" else "不总是强制签到"}",
                                        coroutineScope
                                    )
                                }
                            }
                    } else {
                        snackbarHostState.displaySnackbar(
                            "无法识别的命令",
                            coroutineScope
                        )
                    }
                }) {
                    Text("确认")
                }
            })
        }
        if (isShowSignoffDialog) {
            SnackbarAlertDialog(
                onDismissRequest = { isShowSignoffDialog = false },
                title = { Text("确定要登出吗？") },
                text = {
                    Text("当你登出时，你的代签用户不会丢失。")
                },
                dismissButton = {
                    OutlinedButton(onClick = { isShowSignoffDialog = false }) {
                        Text("取消")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            coroutineScope.launch {
                                ChaoxingHttpClient.cloneInstance = null
                                context.chaoxingDataStore.updateData {
                                    it.toBuilder()
                                        .clearLoginSession()
                                        .build()
                                }

                                naviToLoginScreen()
                            }
                        }
                    ) {
                        Text("登出")
                    }
                }
            )
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0ADA0))
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp, 8.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    displayUserEntity.pic,
                    imageLoader = imageLoader,
                    contentDescription = "头像",
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .clip(
                            RoundedCornerShape(5.dp)
                        )
                )
                Text(
                    "登录用户：${displayUserEntity.name}",
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                        .weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                )
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowSignoffDialog = true
                    }
                ) { Icon(painterResource(R.drawable.ic_log_out), null, tint = Color.White) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        disableComposableCode {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    3.5.dp, Brush.linearGradient(
                        listOf(
                            Color(0xFF76E4F4),
                            Color(0xFF9E6FCD),
                            Color(0xFFC777A9)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .padding(22.dp, 8.dp, 10.dp, 8.dp)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_brain_cog), null)
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "推测签到活动功能",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 21.sp
                                )
                                Text(
                                    "根据日常的签到时间，在打开应用时推测可能的签到课程和事件（测试中）",
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(isRecommendEnabled, onCheckedChange = { value ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isRecommendEnabled = value
                                coroutineScope.launch {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setDisableRecommend(value.not())
                                            .build()
                                    }
                                }
                            }, modifier = Modifier.padding(start = 8.dp))
                        }
                        AnimatedVisibility(
                            isRecommendEnabled,
                            enter = slideInVertically(),
                            exit = slideOutVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(3.dp))
                                if (!allRecommendHabits.isEmpty()) {
                                    Text("已经学习的签到习惯：", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    allRecommendHabits.forEachIndexed { index, item ->
                                        key(index) {
                                            Card(
                                                elevation = CardDefaults.cardElevation(4.dp),
                                                modifier = Modifier.padding(8.dp, 4.dp, 3.dp, 4.dp)
                                            ) {
                                                Row {
                                                    Text(buildAnnotatedString {
                                                        append("星期${ChaoxingRecommendHelper.dayOfWeekTextList[item.dayOfWeek]}的 ")
                                                        withStyle(SpanStyle(fontFamily = FontGilroy)) {
                                                            append(
                                                                "${item.minuteOfDay.div(60)}:${
                                                                    (item.minuteOfDay % 60).toString()
                                                                        .padStart(2, '0')
                                                                }"
                                                            )
                                                        }
                                                        append(" 在${item.className}的签到活动")
                                                    }, modifier = Modifier.weight(1f))
                                                    IconButton(onClick = {
                                                        allRecommendHabits.removeAt(index)
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            context.chaoxingDataStore.updateData { dataStore ->
                                                                dataStore.toBuilder().apply {
                                                                    removeRecommendHabits(index)
                                                                }.build()
                                                            }
                                                        }
                                                    }) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_delete),
                                                            null,
                                                            tint = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        "还没有学习到任何签到习惯，继续更多的使用随地大小签吧~",
                                        fontSize = 13.sp,
                                        lineHeight = 15.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                naviToFavoriteLocationSetting()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FB0DC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_map_pinned),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "收藏的签到位置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Text(
                        "在地图上收藏常用的签到位置，位置签到时可以直接选用。",
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(modifier = Modifier.height(8.dp))
        CustomizeClientCard()
        Text(
            "ChaoxingSignFaker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})," +
                    " buildDate: ${BuildConfig.releaseDate}," +
                    " " +
                    "developed by @aquamarine5, All Rights Reserved.",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.Gray,
            modifier = Modifier.clickable { isCommandDialog = true }
        )
        Spacer(modifier = Modifier.height(8.dp))
        var isUiDevelopedMode by remember { mutableStateOf(isDevelopedMode) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(isUiDevelopedMode, onCheckedChange = { value ->
                isUiDevelopedMode = value
                isDevelopedMode = value
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsDevelopedMode(value).build()
                        ).build()
                    }
                }
            })
            Text("启用开发模式", modifier = Modifier.clickable {
                isUiDevelopedMode = !isUiDevelopedMode
                isDevelopedMode = !isDevelopedMode
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsDevelopedMode(isDevelopedMode).build()
                        ).build()
                    }
                }
            })
        }

        @OnlyAppDevelopedMode AnimatedVisibility(
            isUiDevelopedMode,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            FlowColumn() {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().clearLearntTooltips().build()
                        }
                    }
                }) {
                    Text("ResetAllStoredLearntTooltips")
                }
            }
        }
    }
}
