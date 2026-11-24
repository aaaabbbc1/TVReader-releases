package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.data.repository.BookRepository
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SourcesScreen
import com.example.ui.viewmodel.AppViewModelFactory
import com.example.ui.viewmodel.BookmarksViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SourceTab
import com.example.ui.viewmodel.SourcesViewModel
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LibraryRoute

@Serializable
data class SourcesRoute(val initialTab: String = SourceTab.LOCAL.name)

@Serializable
data class ReaderRoute(
    val bookId: Long,
    val chapterIndex: Int? = null,
    val pageIndex: Int? = null
)

@Serializable
data object SettingsRoute

@Serializable
data object BookmarksRoute

@Serializable
data object SearchRoute

@Serializable
data object AboutRoute

@Composable
fun TvReaderNavGraph(
    navController: NavHostController,
    repository: BookRepository,
    libraryViewModel: LibraryViewModel,
    onLaunchFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier.fillMaxSize()
    ) {
        composable<HomeRoute> {
            val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = state,
                onOpenLibrary = { navController.navigate(LibraryRoute) },
                onBrowseFiles = onLaunchFilePicker,
                onOpenBook = { id -> navController.navigate(ReaderRoute(bookId = id)) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenBookmarks = { navController.navigate(BookmarksRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenSources = { navController.navigate(SourcesRoute()) },
                onOpenAbout = { navController.navigate(AboutRoute) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<LibraryRoute> {
            val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
            LibraryScreen(
                uiState = state,
                onBack = { navController.popBackStack() },
                onSortChange = libraryViewModel::setSort,
                onSearchQueryChange = libraryViewModel::setSearchQuery,
                onOpenBook = { id -> navController.navigate(ReaderRoute(bookId = id)) },
                onDeleteBook = libraryViewModel::deleteBook,
                onOpenSources = { navController.navigate(SourcesRoute()) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<SourcesRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SourcesRoute>()
            val vm: SourcesViewModel = viewModel(factory = AppViewModelFactory(repository))
            val state by vm.uiState.collectAsStateWithLifecycle()
            val webDav by vm.webDavSources.collectAsStateWithLifecycle()
            val ftp by vm.ftpSources.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(route.initialTab) {
                runCatching { SourceTab.valueOf(route.initialTab) }.getOrNull()?.let { tab ->
                    if (tab != SourceTab.LOCAL) vm.selectTab(tab)
                }
            }

            SourcesScreen(
                uiState = state,
                webDavSources = webDav,
                ftpSources = ftp,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onLaunchSystemPicker = onLaunchFilePicker,
                onOpenBook = { id -> navController.navigate(ReaderRoute(bookId = id)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<ReaderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReaderRoute>()
            val vm: ReaderViewModel = viewModel(
                factory = AppViewModelFactory(
                    repository = repository,
                    readerBookId = route.bookId,
                    initialChapter = route.chapterIndex,
                    initialPage = route.pageIndex
                )
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()

            ReaderScreen(
                uiState = state,
                settings = settings,
                bookmarks = bookmarks,
                viewModel = vm,
                onExit = { navController.popBackStack() },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<SettingsRoute> {
            val vm: SettingsViewModel = viewModel(factory = AppViewModelFactory(repository))
            val settings by vm.settings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = settings,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<BookmarksRoute> {
            val vm: BookmarksViewModel = viewModel(factory = AppViewModelFactory(repository))
            val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
            BookmarksScreen(
                bookmarks = bookmarks,
                onBack = { navController.popBackStack() },
                onOpenBookmark = { bm ->
                    navController.navigate(
                        ReaderRoute(
                            bookId = bm.bookId,
                            chapterIndex = bm.chapterIndex,
                            pageIndex = bm.pageIndex
                        )
                    )
                },
                onDeleteBookmark = vm::deleteBookmark,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<SearchRoute> {
            val vm: SearchViewModel = viewModel(factory = AppViewModelFactory(repository))
            val query by vm.query.collectAsStateWithLifecycle()
            val results by vm.results.collectAsStateWithLifecycle()
            val searching by vm.isSearching.collectAsStateWithLifecycle()
            SearchScreen(
                query = query,
                results = results,
                isSearching = searching,
                onQueryChange = vm::updateQuery,
                onSearchSubmit = vm::executeSearch,
                onBack = { navController.popBackStack() },
                onOpenResult = { match ->
                    navController.navigate(
                        ReaderRoute(
                            bookId = match.book.id,
                            chapterIndex = match.chapterIndex
                        )
                    )
                },
                onOpenGutenberg = {
                    navController.navigate(SourcesRoute(initialTab = SourceTab.GUTENBERG.name))
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }

        composable<AboutRoute> {
            AboutScreen(
                onBack = { navController.popBackStack() },
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}
