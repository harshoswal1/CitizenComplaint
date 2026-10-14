package org.nagrikvoice.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.model.Complaint
import org.nagrikvoice.app.data.repository.AuthRepository
import org.nagrikvoice.app.data.repository.ComplaintRepository

class HomeViewModel(
    private val complaintRepo: ComplaintRepository = ComplaintRepository(),
    private val authRepo: AuthRepository = AuthRepository.instance
) : ViewModel() {

    private val _complaints = MutableStateFlow<List<Complaint>>(emptyList())
    val complaints: StateFlow<List<Complaint>> = _complaints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filter = MutableStateFlow("all")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedLocality = MutableStateFlow("Kolhapur Urban")
    val selectedLocality: StateFlow<String> = _selectedLocality.asStateFlow()

    init {
        loadFeed()
    }

    fun setFilter(newFilter: String) {
        _filter.value = newFilter
        loadFeed()
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        loadFeed()
    }

    fun setManualLocation(locality: String) {
        _selectedLocality.value = locality
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            val filterParam = if (_filter.value == "all") null else _filter.value
            val res = complaintRepo.getFeed(
                filter = filterParam,
                category = _selectedCategory.value,
                latitude = 16.7050, // Kolhapur default center
                longitude = 74.2433
            )
            res.onSuccess { feed ->
                _complaints.value = feed.complaints
            }.onFailure {
                // If offline or empty, provide mock items for smooth UI experience
                if (_complaints.value.isEmpty()) {
                    _complaints.value = listOf(
                        Complaint(
                            id = "KMC-2026-0001",
                            reporterId = "usr_1",
                            title = "Hazardous deep pothole near Circuit House gate",
                            description = "Road crater spanning 1.5m creating heavy traffic congestion and 2-wheeler skidding risks.",
                            category = "ROADS_AND_INFRASTRUCTURE",
                            subtype = "Potholes",
                            photoUrl = "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=800",
                            latitude = 16.7112,
                            longitude = 74.2385,
                            locality = "Tarabai Park",
                            administrativeArea = "Kolhapur Municipal Corporation",
                            authorityId = "auth_kmc_kolhapur",
                            authorityName = "Kolhapur Municipal Corp",
                            emailStatus = "DELIVERED",
                            resolutionStatus = "UNRESOLVED",
                            supportCount = 24,
                            isOverdue = 1,
                            userHasSupported = 0,
                            submissionTimestamp = "2026-10-10T10:00:00Z",
                            deliveredTimestamp = "2026-10-10T10:05:00Z",
                            createdAt = "2026-10-10T10:00:00Z"
                        ),
                        Complaint(
                            id = "KMC-2026-0002",
                            reporterId = "usr_2",
                            title = "Garbage pile overflow on 5th Lane junction",
                            description = "Solid waste bins overflowing for over 3 days causing public health and sanitation hazard.",
                            category = "WASTE_MANAGEMENT",
                            subtype = "Garbage accumulation",
                            photoUrl = "https://images.unsplash.com/photo-1605600659908-0ef719419d41?w=800",
                            latitude = 16.6920,
                            longitude = 74.2410,
                            locality = "Rajarampuri",
                            administrativeArea = "Kolhapur Municipal Corporation",
                            authorityId = "auth_kmc_kolhapur",
                            authorityName = "Kolhapur Municipal Corp",
                            emailStatus = "DELIVERED",
                            resolutionStatus = "UNRESOLVED",
                            supportCount = 18,
                            isOverdue = 0,
                            userHasSupported = 1,
                            submissionTimestamp = "2026-10-12T14:30:00Z",
                            deliveredTimestamp = "2026-10-12T14:35:00Z",
                            createdAt = "2026-10-12T14:30:00Z"
                        )
                    )
                }
            }
            _isLoading.value = false
        }
    }

    fun toggleSupport(complaint: Complaint) {
        viewModelScope.launch {
            val userId = authRepo.currentUser.value?.id ?: "usr_anonymous"
            val currentSupported = complaint.userHasSupported == 1
            val newSupported = if (currentSupported) 0 else 1
            val newCount = if (currentSupported) complaint.supportCount - 1 else complaint.supportCount + 1

            // Optimistic UI update
            _complaints.value = _complaints.value.map {
                if (it.id == complaint.id) {
                    it.copy(userHasSupported = newSupported, supportCount = newCount)
                } else it
            }

            complaintRepo.toggleSupport(complaint.id, userId)
        }
    }
}
