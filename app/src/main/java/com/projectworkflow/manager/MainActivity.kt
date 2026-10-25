package com.projectworkflow.manager

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.projectworkflow.manager.ui.dashboard.DashboardScreen
import com.projectworkflow.manager.ui.dashboard.DashboardViewModel
import com.projectworkflow.manager.ui.meeting.CreateMeetingSheet
import com.projectworkflow.manager.ui.meeting.MeetingScreen
import com.projectworkflow.manager.ui.meeting.MeetingViewModel
import com.projectworkflow.manager.ui.navigation.Screen
import com.projectworkflow.manager.ui.project.CreateProjectSheet
import com.projectworkflow.manager.ui.project.ProjectDetailScreen
import com.projectworkflow.manager.ui.project.ProjectListScreen
import com.projectworkflow.manager.ui.project.ProjectViewModel
import com.projectworkflow.manager.ui.theme.ProjectWorkflowTheme
import com.projectworkflow.manager.ui.theme.SurfaceBackground

import com.projectworkflow.manager.ui.update.UpdateDialog
import com.projectworkflow.manager.update.AppUpdateInfo
import com.projectworkflow.manager.update.AppUpdateManager
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    // 自动检测更新地址 (已配置为您专属的 GitHub 仓库，并采用 CDN 加速确保手机秒开)
    companion object {
        const val CHECK_UPDATE_URL = "https://cdn.jsdelivr.net/gh/Hcccdong/xiaozao-planner@main/version.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ProjectManagerApp

        setContent {
            ProjectWorkflowTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { }
                    )
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val navController = rememberNavController()

                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(app.projectRepository, app.meetingRepository)
                )
                val projectViewModel: ProjectViewModel = viewModel(
                    factory = ProjectViewModel.Factory(app.projectRepository)
                )
                val meetingViewModel: MeetingViewModel = viewModel(
                    factory = MeetingViewModel.Factory(app.meetingRepository, app.projectRepository, app.alarmScheduler)
                )

                var showCreateProjectSheet by remember { mutableStateOf(false) }
                var showCreateMeetingSheet by remember { mutableStateOf(false) }
                var meetingTargetProjectId by remember { mutableStateOf<Long?>(null) }
                var meetingTargetStageId by remember { mutableStateOf<Long?>(null) }

                val allProjects by projectViewModel.allProjects.collectAsState()

                val context = LocalContext.current
                var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

                // 启动时自动静默检查新版本
                LaunchedEffect(Unit) {
                    val info = AppUpdateManager.checkUpdate(context, CHECK_UPDATE_URL)
                    if (info != null) {
                        updateInfo = info
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceBackground
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route
                    ) {
                        // 1. 首页仪表盘
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToProjectDetail = { projectId ->
                                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                                },
                                onNavigateToAllProjects = {
                                    navController.navigate(Screen.ProjectList.route)
                                },
                                onOpenCreateProject = {
                                    showCreateProjectSheet = true
                                }
                            )
                        }

                        // 2. 全部项目列表
                        composable(Screen.ProjectList.route) {
                            ProjectListScreen(
                                viewModel = projectViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { projectId ->
                                    navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                                },
                                onOpenCreateProject = {
                                    showCreateProjectSheet = true
                                }
                            )
                        }

                        // 3. 项目深度详情页 (包含楼栋拆解、造价阶段推进与关键会议)
                        composable(
                            route = Screen.ProjectDetail.route,
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                            ProjectDetailScreen(
                                projectId = projectId,
                                viewModel = projectViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenCreateMeeting = { pId, sId ->
                                    meetingTargetProjectId = pId
                                    meetingTargetStageId = sId
                                    showCreateMeetingSheet = true
                                }
                            )
                        }

                        // 4. 会议日程看板
                        composable(Screen.MeetingSchedule.route) {
                            MeetingScreen(
                                viewModel = meetingViewModel,
                                onOpenCreateMeeting = {
                                    meetingTargetProjectId = null
                                    meetingTargetStageId = null
                                    showCreateMeetingSheet = true
                                }
                            )
                        }
                    }

                    // 新建工程造价项目抽屉
                    if (showCreateProjectSheet) {
                        CreateProjectSheet(
                            onDismiss = { showCreateProjectSheet = false },
                            onCreateProject = { name, desc, planDays, template, subTasks ->
                                projectViewModel.createCostProjectFromTemplate(
                                    name = name,
                                    description = desc,
                                    priority = "HIGH",
                                    planDays = planDays,
                                    template = template,
                                    subTasks = subTasks,
                                    onSuccess = { newProjectId ->
                                        showCreateProjectSheet = false
                                        navController.navigate(Screen.ProjectDetail.createRoute(newProjectId))
                                    }
                                )
                            }
                        )
                    }

                    // 排会抽屉
                    if (showCreateMeetingSheet) {
                        CreateMeetingSheet(
                            projects = allProjects,
                            defaultProjectId = meetingTargetProjectId,
                            defaultStageId = meetingTargetStageId,
                            onDismiss = { showCreateMeetingSheet = false },
                            onScheduleMeeting = { meeting ->
                                meetingViewModel.scheduleMeeting(meeting) {
                                    showCreateMeetingSheet = false
                                }
                            }
                        )
                    }

                    // 自动版本更新弹窗
                    updateInfo?.let { info ->
                        UpdateDialog(
                            updateInfo = info,
                            onDismiss = { updateInfo = null },
                            onConfirmUpdate = {
                                AppUpdateManager.downloadAndInstallApk(context, info)
                                updateInfo = null
                            }
                        )
                    }
                }
            }
        }
    }
}
