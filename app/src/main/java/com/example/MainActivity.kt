package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.BookRepository
import com.example.ui.navigation.ReaderRoute
import com.example.ui.navigation.TvReaderNavGraph
import com.example.ui.theme.TVReaderTheme
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.LibraryViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = BookRepository(applicationContext)

        val incomingUri: Uri? = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        setContent {
            TVReaderTheme {
                val navController = rememberNavController()
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = AppViewModelFactory(repository)
                )
                val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
                val settings by repository.settingsFlow.collectAsStateWithLifecycle(
                    initialValue = com.example.data.model.ReaderSettings()
                )
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(settings.keepScreenOn) {
                    if (settings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                LaunchedEffect(incomingUri) {
                    if (incomingUri != null) {
                        libraryViewModel.importBookFromUri(incomingUri) { bookId ->
                            navController.navigate(ReaderRoute(bookId = bookId))
                        }
                    }
                }

                LaunchedEffect(libraryState.errorMessage, libraryState.statusMessage) {
                    val msg = libraryState.errorMessage ?: libraryState.statusMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        libraryViewModel.clearMessages()
                    }
                }

                val openDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        libraryViewModel.importBookFromUri(uri) { bookId ->
                            navController.navigate(ReaderRoute(bookId = bookId))
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    TvReaderNavGraph(
                        navController = navController,
                        repository = repository,
                        libraryViewModel = libraryViewModel,
                        onLaunchFilePicker = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "application/epub+zip",
                                    "application/pdf",
                                    "application/x-fictionbook+xml",
                                    "application/x-cbz",
                                    "application/vnd.comicbook+zip",
                                    "text/plain",
                                    "text/markdown",
                                    "text/html",
                                    "*/*"
                                )
                            )
                        }
                    )

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
