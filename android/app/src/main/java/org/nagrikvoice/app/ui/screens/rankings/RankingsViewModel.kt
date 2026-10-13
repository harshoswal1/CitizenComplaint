package org.nagrikvoice.app.ui.screens.rankings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nagrikvoice.app.data.model.Authority
import org.nagrikvoice.app.data.repository.AuthorityRepository

enum class RankingSortBy {
    UNRESOLVED_COUNT,
    RESOLUTION_RATE,
    OVERDUE_COUNT
}

class RankingsViewModel(
    private val authorityRepo: AuthorityRepository = AuthorityRepository()
) : ViewModel() {

    private val _rankings = MutableStateFlow<List<Authority>>(emptyList())
    val rankings: StateFlow<List<Authority>> = _rankings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortBy = MutableStateFlow(RankingSortBy.UNRESOLVED_COUNT)
    val sortBy: StateFlow<RankingSortBy> = _sortBy.asStateFlow()

    init {
        loadRankings()
    }

    fun setSortBy(sort: RankingSortBy) {
        _sortBy.value = sort
        sortCurrentRankings()
    }

    fun loadRankings() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = authorityRepo.getRankings()
            res.onSuccess { data ->
                _rankings.value = data.rankings
                sortCurrentRankings()
            }.onFailure {
                // Fallback mock initial state showing Kolhapur starting at 0 if offline
                if (_rankings.value.isEmpty()) {
                    _rankings.value = listOf(
                        Authority(
                            id = "auth_kmc_kolhapur",
                            name = "Kolhapur Municipal Corporation",
                            code = "KMC",
                            state = "Maharashtra",
                            district = "Kolhapur",
                            type = "MUNICIPAL_CORPORATION",
                            unresolvedCount = 2,
                            resolvedVerifiedCount = 1,
                            awaitingEmailCount = 0,
                            failedDeliveryCount = 0,
                            overdueCount = 1,
                            totalSupports = 42,
                            resolutionRatePercent = 33.3
                        ),
                        Authority(
                            id = "auth_pmc_pune",
                            name = "Pune Municipal Corporation",
                            code = "PMC",
                            state = "Maharashtra",
                            district = "Pune",
                            type = "MUNICIPAL_CORPORATION",
                            unresolvedCount = 0,
                            resolvedVerifiedCount = 0,
                            awaitingEmailCount = 0,
                            failedDeliveryCount = 0,
                            overdueCount = 0,
                            totalSupports = 0,
                            resolutionRatePercent = 0.0
                        )
                    )
                }
            }
            _isLoading.value = false
        }
    }

    private fun sortCurrentRankings() {
        val list = _rankings.value.toMutableList()
        when (_sortBy.value) {
            RankingSortBy.UNRESOLVED_COUNT -> list.sortByDescending { it.unresolvedCount }
            RankingSortBy.RESOLUTION_RATE -> list.sortByDescending { it.resolutionRatePercent }
            RankingSortBy.OVERDUE_COUNT -> list.sortByDescending { it.overdueCount }
        }
        _rankings.value = list
    }
}
