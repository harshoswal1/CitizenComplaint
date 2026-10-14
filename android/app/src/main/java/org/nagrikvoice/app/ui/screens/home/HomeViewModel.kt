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
            val authorityId = authRepo.selectedAuthorityId.value
            val res = complaintRepo.getFeed(
                filter = filterParam,
                category = _selectedCategory.value,
                authorityId = authorityId
            )
            res.onSuccess { feed ->
                _complaints.value = feed.complaints
            }.onFailure {
                // If network failure and list is empty, keep as emptyList()
                if (_complaints.value.isEmpty()) {
                    _complaints.value = emptyList()
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
