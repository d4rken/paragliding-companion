package eu.darken.pgc.main.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.darken.pgc.common.BuildConfigWrap
import eu.darken.pgc.common.coroutine.DispatcherProvider
import eu.darken.pgc.common.debug.logging.Logging.Priority.ERROR
import eu.darken.pgc.common.debug.logging.asLog
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.github.GithubReleaseCheck
import eu.darken.pgc.common.livedata.SingleLiveEvent
import eu.darken.pgc.common.uix.ViewModel3
import eu.darken.pgc.flights.core.FlightRepo
import eu.darken.pgc.flights.core.FlightStats
import eu.darken.pgc.flights.ui.FlightsGlobalDashCardVH
import eu.darken.pgc.importer.ui.ImporterDashCardVH
import eu.darken.pgc.xctrack.core.XCTrackManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import net.swiftzer.semver.SemVer
import javax.inject.Inject

@HiltViewModel
class DashboardFragmentVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    githubReleaseCheck: GithubReleaseCheck,
    private val xcTrackManager: XCTrackManager,
    private val flightRepo: FlightRepo,
    private val flightStats: FlightStats,
) : ViewModel3(dispatcherProvider = dispatcherProvider) {

    val events = SingleLiveEvent<DashboardEvents>()

    val newRelease = flow {
        val latestRelease = try {
            githubReleaseCheck.latestRelease("d4rken", "paragliding-companion")
        } catch (e: Exception) {
            log(TAG, ERROR) { "Release check failed: ${e.asLog()}" }
            null
        }
        emit(latestRelease)
    }
        .filterNotNull()
        .filter {
            val current = try {
                SemVer.parse(BuildConfigWrap.VERSION_NAME.removePrefix("v"))
            } catch (e: IllegalArgumentException) {
                log(TAG, ERROR) { "Failed to parse current version: ${e.asLog()}" }
                return@filter false
            }
            log(TAG) { "Current version is $current" }

            val latest = try {
                SemVer.parse(it.tagName.removePrefix("v")).nextMinor()
            } catch (e: IllegalArgumentException) {
                log(TAG, ERROR) { "Failed to parse current version: ${e.asLog()}" }
                return@filter false
            }
            log(TAG) { "Latest version is $latest" }
            current < latest
        }
        .asLiveData2()

    val state = combine(
        flightRepo.data,
        flightStats.data,
        flowOf("")
    ) { flightData, flightStats, _ ->
        val items = mutableListOf<DashboardAdapter.Item>()

        if (flightData.flights.isEmpty()) {
            ImporterDashCardVH.Item(
                onImport = { navEvents.postValue(DashboardFragmentDirections.actionDashboardFragmentToIngesterFragment()) },
            ).run { items.add(this) }
        } else {
            FlightsGlobalDashCardVH.Item(
                stats = flightStats,
                onView = {

                },
            ).run { items.add(this) }
        }

        State(items = items)
    }.asLiveData2()

    data class State(
        val items: List<DashboardAdapter.Item>
    )
}