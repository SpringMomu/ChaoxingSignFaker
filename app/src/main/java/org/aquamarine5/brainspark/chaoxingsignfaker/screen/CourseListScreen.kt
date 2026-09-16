/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NewFeatureTipsCard
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingCourseClass
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.RecommendActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.FontGilroy
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.disableCode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.disableComposableCode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class CourseListDestination(
    val isCloneSession: Boolean = false
)

@Serializable
object SignGraphDestination

private const val SORT_TOP = 100
private const val SORT_STAR = 10
private const val SORT_UNFAVOURED = 5
private const val SORT_COMMON = 0

@Composable
fun CourseListScreen(
    destination: CourseListDestination,
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
    navToSettingDestination: () -> Unit,
    navToSignActivityDestination: (SignDestination) -> Unit,
    navToLoginDestination: () -> Unit,
    navToGroupDestination: (isCloneSession: Boolean) -> Unit,
) {
    val imageLoader = LocalImageLoader.current
    val activitiesData =
        rememberSaveable(saver = ChaoxingCourseEntity.Saver) { mutableStateListOf() }
    val preferredClassIds = rememberSaveable {
        mutableListOf<Int>()
    }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    var recommendActivities by remember { mutableStateOf<List<RecommendActivityEntity>?>(null) }
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val isCaptchaAutoResolveLearntTooltip = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            disableCode {
                recommendActivities =
                    ChaoxingRecommendHelper.checkRecommendedActivities(context)
            } //TODO: recommend
            if (activitiesData.isEmpty()) {
                isFetchedFailure = runCatching {
                    val datastoreData = context.chaoxingDataStore.data.first()
                    disableCode {
                        datastoreData.apply {
                            if (version <= 0) {
                                ChaoxingHttpClient.instance?.let { httpClient ->
                                    ChaoxingCourseHelper.getAllCourse(
                                        httpClient,
                                        context,
                                        destination.isCloneSession,
                                        navToLoginDestination
                                    ).let { data ->
                                        context.chaoxingDataStore.updateData { dataStore ->
                                            dataStore.toBuilder().apply {
                                                addAllPreferCourseClass(preferClassIdList.map { classId ->
                                                    ChaoxingCourseClass.newBuilder()
                                                        .setClassId(classId)
                                                        .setCourseId(data.first { it.classId == classId }.courseId)
                                                        .build()
                                                })
                                                setVersion(1)
                                            }.build()
                                        }
                                    }
                                }
                            }
                        }
                    } //TODO: Recommend preferred class
                    isCaptchaAutoResolveLearntTooltip.value =
                        !datastoreData.learntTooltips.sliderCaptchaAutoResolveByModel
                    preferredClassIds.addAll(
                        datastoreData.preferClassIdList.reversed()
                    )
                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                        ?.let { httpClient ->
                            ChaoxingCourseHelper.getAllCourse(
                                httpClient,
                                context,
                                destination.isCloneSession,
                                navToLoginDestination
                            )
                                .apply {
                                    activitiesData.addAll(this.filter {
                                        preferredClassIds.contains(it.classId)
                                    }.map { it.apply { isPreferred.value = true } } + this.filter {
                                        !preferredClassIds.contains(it.classId)
                                    })
                                }
                        }
                }.onFailure {
                    it.snackbarReport(
                        snackbarHost,
                        coroutineScope,
                        "获取课程列表失败",
                        hapticFeedback
                    )
                }
            }
        }
    }

        Column(
            modifier = Modifier
                .padding(16.dp, 0.dp, 16.dp, 0.dp)
        ) {
            Crossfade(isFetchedFailure) { v ->
                if (activitiesData.isNotEmpty()) {
                    var pullToRefreshState by remember { mutableStateOf(false) }
                    PullToRefreshBox(
                        isRefreshing = pullToRefreshState,
                        onRefresh = {
                            pullToRefreshState = true
                            coroutineScope.launch(Dispatchers.IO) {
                                isFetchedFailure = runCatching {
                                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                                        ?.let { httpClient ->
                                            ChaoxingCourseHelper.getAllCourse(
                                                httpClient,
                                                context,
                                                destination.isCloneSession,
                                                navToLoginDestination
                                            )
                                                .apply {
                                                    val newActivities = this.filter {
                                                        preferredClassIds.contains(it.classId)
                                                    }.map {
                                                        it.apply {
                                                            isPreferred.value = true
                                                        }
                                                    } + this.filter {
                                                        !preferredClassIds.contains(it.classId)
                                                    }
                                                    activitiesData.clear()
                                                    activitiesData.addAll(newActivities)
                                                }
                                        }
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "获取课程列表失败",
                                        hapticFeedback
                                    )
                                }
                                delay(500.milliseconds)
                                pullToRefreshState = false
                            }
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            disableComposableCode {
                                AnimatedVisibility(
                                    recommendActivities != null,
                                    enter = fadeIn() + slideInVertically(),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    recommendActivities?.forEachIndexed { index, item ->
                                        Card(
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                navToSignActivityDestination(item.destination)
                                            },
                                            shape = RoundedCornerShape(18.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(24.dp, 8.dp)
                                                    .padding(3.dp)
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.ic_brain_circuit),
                                                    null
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("根据平时的签到习惯推断出可能会点击的签到活动：")
                                                    Text(buildAnnotatedString {
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                            append(item.className)
                                                        }
                                                        append(" 在 ")
                                                        withStyle(
                                                            SpanStyle(
                                                                fontFamily = FontGilroy
                                                            )
                                                        ) {
                                                            append(
                                                                LocalDateTime.from(
                                                                    Instant.ofEpochMilli(
                                                                        item.startTime
                                                                    )
                                                                ).run {
                                                                    "$hour:$minute:$second"
                                                                })
                                                        }
                                                        append(" 的 ")
                                                        append(item.activityName)
                                                    })

                                                }
                                            }
                                        }
                                        if (index != recommendActivities?.lastIndex) {
                                            Spacer(modifier = Modifier.padding(vertical = 8.dp))
                                        }

                                    }
                                }
                            } //TODO: Recommend

                            var debouncePreviousTime by remember { mutableLongStateOf(0L) }
                            var searchQuery by rememberSaveable { mutableStateOf("") }
                            val filteredActivities =
                                if (searchQuery.isBlank()) activitiesData
                                else activitiesData.filter {
                                    it.courseName.contains(searchQuery, ignoreCase = true) ||
                                            (it.teacherName?.contains(
                                                searchQuery,
                                                ignoreCase = true
                                            ) == true) ||
                                            (it.schools?.contains(
                                                searchQuery,
                                                ignoreCase = true
                                            ) == true)
                                }
                            LazyColumn {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                item {
                                    Card(modifier = Modifier.zIndex(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp, 8.dp)
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.ic_circle_question_mark),
                                                null
                                            )
                                            Column(
                                                modifier = Modifier.padding(
                                                    start = 12.dp
                                                )
                                            ) {
                                                Text(
                                                    "找不到要签到的班级或者签到的活动？可能老师是在群聊里面发起的签到",
                                                    fontSize = 14.sp,
                                                    lineHeight = 17.sp,
                                                    style = TextStyle.Default.copy(
                                                        lineBreak = LineBreak(
                                                            strategy = LineBreak.Strategy.HighQuality,
                                                            strictness = LineBreak.Strictness.Strict,
                                                            wordBreak = LineBreak.WordBreak.Default
                                                        )
                                                    )
                                                )
                                                OutlinedButton(
                                                    onClick = {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.ContextClick
                                                        )
                                                        navToGroupDestination(destination.isCloneSession)
                                                    },
                                                    shape = RoundedCornerShape(18.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_users_round),
                                                            null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            "从群聊列表查找签到",
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                item {
                                    NewFeatureTipsCard(
                                        isCaptchaAutoResolveLearntTooltip,
                                        tipsContent = {
                                            val brainIconId = "brain_icon"
                                            Text(
                                                buildAnnotatedString {
                                                    append("现在验证码会通过内置的识别模型自动滑动完成了，自动完成的验证码会以")
                                                    appendInlineContent(
                                                        brainIconId,
                                                        "[模型自动识别]"
                                                    )
                                                    append("显示，感谢 @0x77786d 提供的模型支持。")
                                                },
                                                inlineContent = mapOf(
                                                    brainIconId to InlineTextContent(
                                                        Placeholder(
                                                            width = 16.sp,
                                                            height = 16.sp,
                                                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                                        )
                                                    ) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_brain_circuit),
                                                            contentDescription = "模型自动识别"
                                                        )
                                                    }
                                                ),
                                                fontSize = 14.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    ) {
                                        context.chaoxingDataStore.updateData {
                                            it.toBuilder()
                                                .setLearntTooltips(
                                                    it.learntTooltips.toBuilder()
                                                        .setSliderCaptchaAutoResolveByModel(
                                                            true
                                                        ).build()
                                                ).build()
                                        }
                                    }
                                }
                                stickyHeader(key = "course_search") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.background,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("搜索课程名称 / 老师 / 学校") },
                                                leadingIcon = {
                                                    Icon(
                                                        painterResource(R.drawable.ic_search),
                                                        contentDescription = "搜索"
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.ContextClick
                                                            )
                                                            searchQuery = ""
                                                        }) {
                                                            Icon(
                                                                painterResource(R.drawable.ic_x),
                                                                contentDescription = "清除搜索"
                                                            )
                                                        }
                                                    }
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                        }
                                    }
                                }
                                if (searchQuery.isNotBlank() && filteredActivities.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.ic_circle_question_mark),
                                                null
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("没有找到与“$searchQuery”匹配的课程")
                                        }
                                    }
                                }
                                items(filteredActivities, key = { it.classId }) { data ->
                                    Column(
                                        modifier = Modifier.animateItem(
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                visibilityThreshold = IntOffset.VisibilityThreshold
                                            ),
                                            fadeInSpec = spring(
                                                stiffness = Spring.StiffnessMedium),
                                            fadeOutSpec = spring(
                                                stiffness = Spring.StiffnessMedium)
                                        )
                                    ) {
                                        CourseInfoColumnCard(
                                            data,
                                            imageLoader,
                                            onPreferredResort = { isPreferred ->
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                if (isPreferred)
                                                    coroutineScope.launch {
                                                        context.chaoxingDataStore.updateData {
                                                            it.toBuilder()
                                                                .addPreferClassId(data.classId)
                                                                .build()
                                                        }
                                                        preferredClassIds.add(data.classId)
                                                        activitiesData.sortByDescending {
                                                            if (it.classId == data.classId)
                                                                return@sortByDescending SORT_TOP
                                                            if (preferredClassIds.contains(
                                                                    it.classId
                                                                )
                                                            ) return@sortByDescending SORT_STAR
                                                            else return@sortByDescending SORT_COMMON
                                                        }
                                                    }
                                                else {
                                                    coroutineScope.launch {
                                                        context.chaoxingDataStore.updateData { dataStore ->
                                                            dataStore.toBuilder().apply {
                                                                val newList =
                                                                    preferClassIdList.filterNot { it == data.classId }
                                                                clearPreferClassId()
                                                                addAllPreferClassId(newList)
                                                            }.build()
                                                        }
                                                        preferredClassIds.remove(data.classId)
                                                        activitiesData.sortByDescending {
                                                            if (it.classId == data.classId)
                                                                return@sortByDescending SORT_UNFAVOURED
                                                            if (preferredClassIds.contains(
                                                                    it.classId
                                                                )
                                                            ) return@sortByDescending SORT_STAR
                                                            else return@sortByDescending SORT_COMMON
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - debouncePreviousTime < 1000)
                                                return@CourseInfoColumnCard
                                            debouncePreviousTime = currentTime
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            navToDetailDestination(data)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (v == null) {
                    CenterCircularProgressIndicator()
                } else if (v.isFailure) {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                                    ?.let { httpClient ->
                                        ChaoxingCourseHelper.getAllCourse(
                                            httpClient,
                                            context,
                                            destination.isCloneSession,
                                            navToLoginDestination
                                        )
                                            .apply {
                                                activitiesData.addAll(this.filter {
                                                    preferredClassIds.contains(it.classId)
                                                }.map {
                                                    it.apply {
                                                        isPreferred.value = true
                                                    }
                                                } + this.filter {
                                                    !preferredClassIds.contains(it.classId)
                                                })
                                            }
                                    }
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "获取课程列表失败",
                                    hapticFeedback
                                )
                            }
                        }
                        isFetchedFailure = null
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.align(Alignment.Center)) {
                            Icon(painterResource(R.drawable.ic_circle_question_mark), null)
                            Text("暂无课程，请检查登录的学习通账号是否正确。")
                        }
                    }
                }
            }
        }

}
