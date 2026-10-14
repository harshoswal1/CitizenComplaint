package org.nagrikvoice.app.ui.screens.report

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.model.CreateComplaintRequest
import org.nagrikvoice.app.data.model.DuplicateCandidate
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.ComplaintRepository
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    private val authRepo: AuthRepository = AuthRepository.instance
) : ViewModel() {

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState.asStateFlow()

    // Form fields
    var title = MutableStateFlow("")
    var description = MutableStateFlow("")
    var category = MutableStateFlow("ROADS_AND_INFRASTRUCTURE")
    var subtype = MutableStateFlow("Potholes")
    var landmark = MutableStateFlow("")
    var locality = MutableStateFlow("Current Area")
    var administrativeArea = MutableStateFlow(authRepo.selectedAuthorityName.value ?: "Municipal Corporation")
    var latitude = MutableStateFlow(0.0)
    var longitude = MutableStateFlow(0.0)
    var locationAccuracy = MutableStateFlow(0.0)
    var isGpsLocked = MutableStateFlow(false)
    var isAcquiringLocation = MutableStateFlow(false)

    // Camera Capture & Transparency (Section 5.2: In-app camera requirement, no gallery upload)
    var photoUri = MutableStateFlow<String?>(null)
    var cameraCaptureTimestamp = MutableStateFlow<String?>(null)

    // Authority selected by user
    val authorityId = MutableStateFlow(authRepo.selectedAuthorityId.value ?: "auth_kmc_kolhapur")
    val authorityName = MutableStateFlow(authRepo.selectedAuthorityName.value ?: "Kolhapur Municipal Corporation (KMC)")

    init {
        // Sync with user's selected municipal corporation
        viewModelScope.launch {
            authRepo.selectedAuthorityId.collect { id ->
                if (id != null) authorityId.value = id
            }
        }
        viewModelScope.launch {
            authRepo.selectedAuthorityName.collect { name ->
                if (name != null) {
                    authorityName.value = name
                    administrativeArea.value = name
                }
            }
        }
    }

    /**
     * Process photo captured directly from device camera via FileProvider.
     * Enforces in-app camera capture: downsamples, converts to base64 JPEG data URL,
     * and timestamps with current device time.
     */
    fun processCameraCapture(photoFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!photoFile.exists() || photoFile.length() == 0L) return@launch

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(photoFile.absolutePath, bounds)

                var sampleSize = 1
                while (bounds.outWidth / sampleSize > 1024 || bounds.outHeight / sampleSize > 1024) {
                    sampleSize *= 2
                }

                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options) ?: return@launch

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val bytes = stream.toByteArray()
                val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUrl = "data:image/jpeg;base64,$base64Str"

                val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                photoUri.value = dataUrl
                cameraCaptureTimestamp.value = isoTimestamp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Acquire live GPS location from device hardware.
     */
    fun fetchLiveLocation(context: Context, onPermissionRequired: () -> Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onPermissionRequired()
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val isGpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsOn && !isNetOn) {
            isGpsLocked.value = false
            return
        }

        isAcquiringLocation.value = true

        try {
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet

            if (best != null) {
                applyLocation(context, best)
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    applyLocation(context, loc)
                    lm.removeUpdates(this)
                }
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
                @Deprecated("Deprecated")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            }

            if (isGpsOn) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, listener)
            } else if (isNetOn) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 2f, listener)
            }
        } catch (_: SecurityException) {
            onPermissionRequired()
        }
    }

    private fun applyLocation(context: Context, loc: Location) {
        latitude.value = loc.latitude
        longitude.value = loc.longitude
        locationAccuracy.value = loc.accuracy.toDouble()
        isGpsLocked.value = true
        isAcquiringLocation.value = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                if (!results.isNullOrEmpty()) {
                    val addr = results[0]
                    val sub = addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: "Live Location"
                    val city = addr.locality ?: addr.subAdminArea ?: authorityName.value
                    locality.value = sub
                    administrativeArea.value = "$city, ${addr.adminArea ?: "India"}"
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Pre-submission Duplicate Check (Rule 1)
     */
    fun initiateSubmission() {
        if (photoUri.value == null) {
            _submissionState.value = SubmissionState.Error("A live photo captured with the in-app camera is required for transparency.")
            return
        }

        if (latitude.value == 0.0 && longitude.value == 0.0) {
            _submissionState.value = SubmissionState.Error("Please enable device location / GPS to geotag your complaint.")
            return
        }

        if (title.value.isBlank() || description.value.isBlank()) {
            _submissionState.value = SubmissionState.Error("Complaint title and detailed description are required.")
            return
        }

        viewModelScope.launch {
            _submissionState.value = SubmissionState.CheckingDuplicates
            val dupRes = complaintRepo.checkDuplicate(
                latitude = latitude.value,
                longitude = longitude.value,
                category = category.value,
                subtype = subtype.value,
                radius = 100.0
            )

            dupRes.onSuccess { res ->
                if (res.hasDuplicates && res.candidates.isNotEmpty()) {
                    _submissionState.value = SubmissionState.DuplicateFound(res.candidates.first())
                } else {
                    executeCreateComplaint()
                }
            }.onFailure {
                executeCreateComplaint()
            }
        }
    }

    fun proceedDespiteDuplicate() {
        executeCreateComplaint()
    }

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
                title = title.value.trim(),
                description = description.value.trim(),
                category = category.value,
                subtype = subtype.value.ifBlank { null },
                photoUrl = photoUri.value!!,
                latitude = latitude.value,
                longitude = longitude.value,
                locationAccuracy = locationAccuracy.value,
                landmark = landmark.value.ifBlank { null },
                locality = locality.value,
                administrativeArea = administrativeArea.value,
                authorityId = authorityId.value,
                simulateDelivery = true,
                deliveryOutcome = "DELIVERED",
                cameraCaptureTimestamp = cameraCaptureTimestamp.value
            )

            val res = complaintRepo.createComplaint(req)
            res.onSuccess { data ->
                val complaintObj = data["complaint"] as? Map<*, *>
                val complaintId = complaintObj?.get("id")?.toString() ?: "NAGRIK-2026-SUBMITTED"
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
        photoUri.value = null
        cameraCaptureTimestamp.value = null
    }
}
