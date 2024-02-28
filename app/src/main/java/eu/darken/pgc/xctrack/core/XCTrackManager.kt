package eu.darken.pgc.xctrack.core

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import eu.darken.pgc.common.datastore.value
import eu.darken.pgc.common.debug.logging.log
import eu.darken.pgc.common.debug.logging.logTag
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XCTrackManager @Inject constructor(
    private val settings: XCTrackSettings,
    private val contentResolver: ContentResolver,
) {
    suspend fun getAccessIntent(): Intent {
//        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
//            putExtra("android.content.extra.SHOW_ADVANCED", true)
//            val provclie = contentResolver.acquireContentProviderClient("org.xcontest.XCTrack.allfiles")
//            log(TAG) { "$provclie" }
////        //  content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2Forg.xcontest.XCTrack
////        val base = "content://com.android.externalstorage.documents/tree/primary"
////        val dataPath = Uri.encode("Android/data/$PKG/files")
//////        val intialUri = DocumentsContract.buildDocumentUriUsingTree(navUri, DocumentsContract.getTreeDocumentId(navUri))
////        val intialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2Forg.xcontest.XCTrack%2Ffiles/document/primary%3AAndroid%2Fdata%2Forg.xcontest.XCTrack%2Ffiles")
////        putExtra(DocumentsContract.EXTRA_INITIAL_URI, intialUri)
//        }
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "*/*" // Use the appropriate MIME type for the files you want to access.
        }
    }

    suspend fun setSetupDismiss(dismissed: Boolean) {
        log(TAG) { "setSetupDismiss(dismissed=$dismissed)" }
        settings.isSetupDismissed.value(dismissed)
    }

    suspend fun takeAccess(uri: Uri) {
        log(TAG) { "takeAccess($uri)" }
//        val before = contentResolver.persistedUriPermissions
//        log(TAG) { "takeAccess(...) before ${before.size}" }
//        before.forEach { log(TAG, VERBOSE) { "takeAccess(...): before - $it" } }
//
//        val takeFlags: Int = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//        contentResolver.takePersistableUriPermission(uri, takeFlags)
//
//        val after = contentResolver.persistedUriPermissions
//        log(TAG) { "takeAccess(...) after ${after.size}" }
//        after.forEach { log(TAG, VERBOSE) { "takeAccess(...): after - $it" } }
    }

    val state = combine(
        settings.isSetupDismissed.flow,
        flowOf("")
    ) { isSetupDismissed, _ ->

        State(
            isSetupDone = true,
            isSetupDismissed = isSetupDismissed
        )
    }

    data class State(
        val isSetupDone: Boolean = false,
        val isSetupDismissed: Boolean = false,
    )

    companion object {
        private const val PKG = "org.xcontest.XCTrack"
        internal val TAG = logTag("XCTrack", "Manager")
    }
}