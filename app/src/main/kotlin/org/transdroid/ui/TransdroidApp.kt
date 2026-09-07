/*
 * Copyright 2010-2026 Eric Kok et al.
 *
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transdroid. If not, see <https://www.gnu.org/licenses/>.
 */
package org.transdroid.ui

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.transdroid.ui.add.AddTorrentScreen
import org.transdroid.ui.rss.RssFeedsScreen
import org.transdroid.ui.rss.RssItemsScreen
import org.transdroid.ui.rss.RssViewModel
import org.transdroid.ui.search.SearchScreen
import org.transdroid.ui.search.SearchViewModel
import org.transdroid.ui.settings.EditServerScreen
import org.transdroid.ui.settings.SettingsScreen
import org.transdroid.ui.settings.SettingsViewModel
import org.transdroid.ui.torrents.TorrentDetailsScreen
import org.transdroid.ui.torrents.TorrentsScreen
import org.transdroid.ui.torrents.TorrentsViewModel

object Routes {
    const val TORRENTS = "torrents"
    const val TORRENT_DETAILS = "torrent/{id}"
    const val ADD = "add?url={url}"
    const val SETTINGS = "settings"
    const val EDIT_SERVER = "settings/server/{id}"
    const val RSS = "rss"
    const val RSS_ITEMS = "rss/{feedId}"
    const val SEARCH = "search"

    const val NEW_SERVER_ID = "new"

    fun torrentDetails(id: String) = "torrent/${Uri.encode(id)}"
    fun add(url: String?) = if (url == null) "add" else "add?url=${Uri.encode(url)}"
    fun editServer(id: String?) = "settings/server/${Uri.encode(id ?: NEW_SERVER_ID)}"
    fun rssItems(feedId: String) = "rss/${Uri.encode(feedId)}"
}

@Composable
fun TransdroidApp(
    useTwoPane: Boolean,
    pendingTorrentUrl: String?,
    onPendingTorrentUrlConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    val torrentsViewModel: TorrentsViewModel = viewModel(factory = TorrentsViewModel.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val rssViewModel: RssViewModel = viewModel(factory = RssViewModel.Factory)
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)

    LaunchedEffect(pendingTorrentUrl) {
        if (pendingTorrentUrl != null) {
            navController.navigate(Routes.add(pendingTorrentUrl))
            onPendingTorrentUrlConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.TORRENTS,
        // The standard Android push/pop slide, replacing Navigation-Compose's default cross-fade.
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
    ) {
        composable(Routes.TORRENTS) {
            TorrentsScreen(
                viewModel = torrentsViewModel,
                useTwoPane = useTwoPane,
                onOpenDetails = { id ->
                    torrentsViewModel.select(id)
                    if (!useTwoPane) navController.navigate(Routes.torrentDetails(id))
                },
                onAddTorrent = { navController.navigate(Routes.add(null)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRss = { navController.navigate(Routes.RSS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
            )
        }
        composable(Routes.RSS) {
            RssFeedsScreen(
                viewModel = rssViewModel,
                onOpenFeed = { feedId -> navController.navigate(Routes.rssItems(feedId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.RSS_ITEMS,
            arguments = listOf(navArgument("feedId") { type = NavType.StringType }),
        ) { entry ->
            RssItemsScreen(
                viewModel = rssViewModel,
                feedId = entry.arguments?.getString("feedId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = searchViewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.TORRENT_DETAILS,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            TorrentDetailsScreen(
                viewModel = torrentsViewModel,
                torrentId = id,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.ADD,
            arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            AddTorrentScreen(
                viewModel = torrentsViewModel,
                initialUrl = entry.arguments?.getString("url").orEmpty(),
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onEditServer = { id -> navController.navigate(Routes.editServer(id)) },
                onOpenFeed = { feedId -> navController.navigate(Routes.rssItems(feedId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EDIT_SERVER,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            EditServerScreen(
                viewModel = settingsViewModel,
                serverId = entry.arguments?.getString("id")?.takeIf { it != Routes.NEW_SERVER_ID },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
