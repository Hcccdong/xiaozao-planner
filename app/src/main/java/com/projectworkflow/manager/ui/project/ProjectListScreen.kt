package com.projectworkflow.manager.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.ui.components.ModernProgressBar
import com.projectworkflow.manager.ui.components.StatusPill
import com.projectworkflow.manager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onOpenCreateProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "全部工程项目",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenCreateProject) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "新建项目", tint = BluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(SurfaceBackground)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects) { project ->
                ProjectItemCard(
                    project = project,
                    onClick = { onNavigateToDetail(project.id) }
                )
            }
        }
    }
}

@Composable
private fun ProjectItemCard(
    project: Project,
    onClick: () -> Unit
) {
    val tagType = when (project.status.name) {
        "DELAYED" -> TagType.URGENT_RED
        "COMPLETED" -> TagType.SUCCESS_GREEN
        else -> TagType.WARNING_AMBER
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            StatusPill(text = project.status.label, tagType = tagType)
        }

        if (!project.description.isNullOrBlank()) {
            Text(
                text = project.description,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "进度: ${project.progress}% (${project.stages.size}个阶段)",
                fontSize = 11.sp,
                color = TextSecondaryDark
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        ModernProgressBar(progress = project.progress)
    }
}
