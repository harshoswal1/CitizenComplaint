package org.nagrikvoice.app.ui.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.model.CreateComplaintRequest
import org.nagrikvoice.app.data.model.DuplicateCandidate
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.ComplaintRepository

sealed class SubmissionState {
    object Idle : SubmissionState()
    object CheckingDuplicates : SubmissionState()
    data class DuplicateFound(val candidate: DuplicateCandidate) : SubmissionState()
    object Submitting : SubmissionState()
    data class Success(val complaintId: String, val emailStatus: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class ReportViewModel(
    private val complaintRepo: ComplaintRepository = ComplaintRepository(),
    private val authRepo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    // Form fields
    var title = MutableStateFlow("")
    var description = MutableStateFlow("")
    var category = MutableStateFlow("ROADS_AND_INFRASTRUCTURE")
    var subtype = MutableStateFlow("Potholes")
    var landmark = MutableStateFlow("")
    var locality = MutableStateFlow("Tarabai Park")
    var latitude = MutableStateFlow(16.7112)
    var longitude = MutableStateFlow(74.2385)
    var photoUri = MutableStateFlow<String?>("https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800")
    var locationAccuracy = MutableStateFlow(4.5) // meters

    fun setCapturedPhoto(uri: String) {
        photoUri.value = uri
    }

    fun setLocation(lat: Double, lng: Double, accuracy: Double, area: String) {
        latitude.value = lat
        longitude.value = lng
        locationAccuracy.value = accuracy
        locality.value = area
    }

    /**
     * Step 1: Pre-submission Duplicate Check (Rule 1)
     */
    fun initiateSubmission() {
        if (title.value.isBlank() || description.value.isBlank()) {
            _submissionState.value = SubmissionState.Error("Title and description are required")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.CheckingDuplicates
            val dupRes = complaintRepo.checkDuplicate(
                latitude = latitude.value,
                longitude = longitude.value,
                category = category.value,
                subtype = subtype.value,
                radius = 100.0 // 100 meters
            )

            dupRes.onSuccess { res ->
                if (res.hasDuplicates && res.candidates.isNotEmpty()) {
                    // Trigger duplicate dialog
                    _submissionState.value = SubmissionState.DuplicateFound(res.candidates.first())
                } else {
                    // No duplicates, proceed directly
                    executeCreateComplaint()
                }
            }.onFailure {
                // Network failure checking duplicates, proceed with warning
                executeCreateComplaint()
            }
        }
    }

    /**
     * Step 2: Citizen confirms this is a separate problem
     */
    fun proceedDespiteDuplicate() {
        executeCreateComplaint()
    }

    /**
     * Citizen confirms this IS the same problem -> Support existing (Rule 1)
     */
    fun supportExistingDuplicate(candidateId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val userId = authRepo.currentUser.value?.id ?: "usr_citizen"
            complaintRepo.toggleSupport(candidateId, userId)
            _submissionState.value = SubmissionState.Idle
            onComplete()
        }
    }

    private fun executeCreateComplaint() {
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Submitting
            val userId = authRepo.currentUser.value?.id ?: "usr_citizen"

            val req = CreateComplaintRequest(
                reporterId = userId,
                title = title.value,
                description = description.value,
                category = category.value,
                subtype = subtype.value,
                photoUrl = photoUri.value ?: "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800",
                latitude = latitude.value,
                longitude = longitude.value,
                locationAccuracy = locationAccuracy.value,
                landmark = landmark.value.ifBlank { null },
                locality = locality.value,
                simulateDelivery = true,
                deliveryOutcome = "DELIVERED"
            )

            val res = complaintRepo.createComplaint(req)
            res.onSuccess { data ->
                val complaintObj = data["complaint"] as? Map<*, *>
                val complaintId = complaintObj?.get("id")?.toString() ?: "KMC-2026-SUBMITTED"
                val emailStatus = complaintObj?.get("email_status")?.toString() ?: "DELIVERED"
                _submissionState.value = SubmissionState.Success(complaintId, emailStatus)
            }.onFailure { e ->
                _submissionState.value = SubmissionState.Error(e.message ?: "Failed to submit complaint")
            }
        }
    }

    fun resetState() {
        _submissionState.value = SubmissionState.Idle
        title.value = ""
        description.value = ""
        landmark.value = ""
    }
}
