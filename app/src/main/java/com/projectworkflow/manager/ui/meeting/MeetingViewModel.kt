package com.projectworkflow.manager.ui.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.repository.MeetingRepository
import com.projectworkflow.manager.domain.repository.ProjectRepository
import com.projectworkflow.manager.reminder.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MeetingViewModel(
    private val meetingRepository: MeetingRepository,
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    val allMeetings: StateFlow<List<Meeting>> = meetingRepository.getAllMeetingsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun scheduleMeeting(meeting: Meeting, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val meetingId = meetingRepository.scheduleMeeting(meeting)
            val fullMeeting = meeting.copy(id = meetingId)
            // 设定系统精准闹钟与通知调度
            alarmScheduler.schedule(fullMeeting)
            onSuccess()
        }
    }

    fun toggleMeetingComplete(meeting: Meeting) {
        viewModelScope.launch {
            val newStatus = !meeting.isCompleted
            meetingRepository.markMeetingCompleted(meeting.id, newStatus)
            if (newStatus) {
                alarmScheduler.cancel(meeting.id)
            } else {
                alarmScheduler.schedule(meeting)
            }
        }
    }

    fun deleteMeeting(meetingId: Long) {
        viewModelScope.launch {
            alarmScheduler.cancel(meetingId)
            meetingRepository.deleteMeeting(meetingId)
        }
    }

    class Factory(
        private val meetingRepository: MeetingRepository,
        private val projectRepository: ProjectRepository,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MeetingViewModel(meetingRepository, projectRepository, alarmScheduler) as T
        }
    }
}
