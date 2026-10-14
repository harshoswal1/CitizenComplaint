package org.nagrikvoice.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.model.Complaint
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.ComplaintRepository

class ComplaintDetailViewModel(
    private val complaintRepo: ComplaintRepository = ComplaintRepository(),
    private val authRepo: AuthRepository = AuthRepository.instance
) : ViewModel() {

    private val _complaint = MutableStateFlow<Complaint?>(null)
    val complaint: StateFlow<Complaint?> = _complaint.asStateFlow()

    private val _history = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val history: StateFlow<List<Map<String, Any>>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadComplaint(complaintId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = complaintRepo.getComplaintDetail(complaintId)
            res.onSuccess { data ->
                val cMap = data["complaint"] as? Map<*, *>
                if (cMap != null) {
                    _complaint.value = Complaint(
                        id = cMap["id"]?.toString() ?: complaintId,
                        reporterId = cMap["reporter_id"]?.toString() ?: "",
                        title = cMap["title"]?.toString() ?: "",
                        description = cMap["description"]?.toString() ?: "",
                        category = cMap["category"]?.toString() ?: "",
                        subtype = cMap["subtype"]?.toString(),
                        photoUrl = cMap["photo_url"]?.toString() ?: "",
                        latitude = (cMap["latitude"] as? Number)?.toDouble() ?: 16.7050,
                        longitude = (cMap["longitude"] as? Number)?.toDouble() ?: 74.2433,
                        locality = cMap["locality"]?.toString() ?: "Kolhapur",
                        administrativeArea = cMap["administrative_area"]?.toString() ?: "Kolhapur Municipal Corp",
                        authorityId = cMap["authority_id"]?.toString() ?: "auth_kmc_kolhapur",
                        authorityName = cMap["authority_name"]?.toString() ?: "Kolhapur Municipal Corp",
                        departmentName = cMap["department_name"]?.toString(),
                        emailStatus = cMap["email_status"]?.toString() ?: "PENDING",
                        resolutionStatus = cMap["resolution_status"]?.toString() ?: "PENDING_EMAIL",
                        supportCount = (cMap["support_count"] as? Number)?.toInt() ?: 0,
                        isOverdue = (cMap["is_overdue"] as? Number)?.toInt() ?: 0,
                        submissionTimestamp = cMap["submission_timestamp"]?.toString() ?: "",
                        deliveredTimestamp = cMap["delivered_timestamp"]?.toString(),
                        createdAt = cMap["created_at"]?.toString() ?: ""
                    )
                }
                val histList = data["history"] as? List<Map<String, Any>>
                if (histList != null) {
                    _history.value = histList
                }
            }
            _isLoading.value = false
        }
    }

    fun toggleSupport() {
        val current = _complaint.value ?: return
        viewModelScope.launch {
            val userId = authRepo.currentUser.value?.id ?: "usr_citizen"
            val newSupported = if (current.userHasSupported == 1) 0 else 1
            val newCount = if (current.userHasSupported == 1) current.supportCount - 1 else current.supportCount + 1
            _complaint.value = current.copy(userHasSupported = newSupported, supportCount = newCount)
            complaintRepo.toggleSupport(current.id, userId)
        }
    }

    fun retryEmail(onComplete: () -> Unit) {
        val current = _complaint.value ?: return
        viewModelScope.launch {
            complaintRepo.retryEmail(current.id)
            loadComplaint(current.id)
            onComplete()
        }
    }

    fun claimResolution(description: String, onComplete: () -> Unit) {
        val current = _complaint.value ?: return
        viewModelScope.launch {
            val userId = authRepo.currentUser.value?.id ?: "usr_citizen"
            complaintRepo.claimResolution(current.id, userId, description, null)
            loadComplaint(current.id)
            onComplete()
        }
    }
}
