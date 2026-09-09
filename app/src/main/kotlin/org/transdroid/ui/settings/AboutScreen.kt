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
package org.transdroid.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.transdroid.BuildConfig
import org.transdroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onOpenChangelog: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val donateAvailable = booleanResource(R.bool.donate_available)
    val websiteLinkAvailable = booleanResource(R.bool.website_link_available)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noBrowserAppMessage = stringResource(R.string.settings_no_browser_app)
    val gplUrl = stringResource(R.string.about_gpl_url)
    val githubUrl = stringResource(R.string.about_github_url)
    val websiteUrl = stringResource(R.string.about_website_url)
    val contributorsUrl = stringResource(R.string.about_contributors_url)
    val issuesUrl = stringResource(R.string.about_issues_url)
    val donateUrl = stringResource(R.string.donate_url)

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(noBrowserAppMessage) }
        }
    }

    val externalLinkIcon: @Composable RowScope.() -> Unit = {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.details_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF5F9130), Color(0xFF2F5A17)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        stringResource(R.string.settings_about, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp),
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.settings_about_section)) {
                    SettingsRow(
                        icon = Icons.Rounded.Gavel,
                        title = stringResource(R.string.about_license),
                        subtitle = stringResource(R.string.about_license_subtitle),
                        onClick = { openUrl(gplUrl) },
                        trailing = externalLinkIcon,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.NewReleases,
                        title = stringResource(R.string.about_whats_new),
                        subtitle = stringResource(R.string.about_whats_new_subtitle),
                        onClick = onOpenChangelog,
                    )
                }
            }

            item {
                SettingsSection(stringResource(R.string.about_project_section)) {
                    SettingsRow(
                        icon = Icons.Rounded.Code,
                        title = stringResource(R.string.about_source),
                        subtitle = stringResource(R.string.about_source_subtitle),
                        onClick = { openUrl(githubUrl) },
                        trailing = externalLinkIcon,
                    )
                    if (websiteLinkAvailable) {
                        SettingsRow(
                            icon = Icons.Rounded.Language,
                            title = stringResource(R.string.about_website),
                            subtitle = stringResource(R.string.about_website_subtitle),
                            onClick = { openUrl(websiteUrl) },
                            trailing = externalLinkIcon,
                        )
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Group,
                        title = stringResource(R.string.about_contributors),
                        subtitle = stringResource(R.string.about_contributors_subtitle),
                        onClick = { openUrl(contributorsUrl) },
                        trailing = externalLinkIcon,
                    )
                    SettingsRow(
                        icon = Icons.Rounded.BugReport,
                        title = stringResource(R.string.about_report_issue),
                        subtitle = stringResource(R.string.about_report_issue_subtitle),
                        onClick = { openUrl(issuesUrl) },
                        trailing = externalLinkIcon,
                    )
                }
            }

            if (donateAvailable) {
                item {
                    SettingsSection(stringResource(R.string.about_support_section)) {
                        SettingsRow(
                            icon = Icons.Rounded.VolunteerActivism,
                            title = stringResource(R.string.settings_donate),
                            subtitle = stringResource(R.string.settings_donate_subtitle),
                            onClick = { openUrl(donateUrl) },
                            trailing = externalLinkIcon,
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.about_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
