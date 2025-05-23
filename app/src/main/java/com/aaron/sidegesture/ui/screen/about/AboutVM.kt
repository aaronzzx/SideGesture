package com.aaron.sidegesture.ui.screen.about

import android.graphics.drawable.Drawable
import androidx.lifecycle.viewModelScope
import com.aaron.compose.base.BaseComposeVM
import com.aaron.sidegesture.App
import com.aaron.sidegesture.BuildConfig
import com.aaron.sidegesture.R
import com.aaron.sidegesture.ui.screen.about.AboutVM.UiEvent
import com.aaron.sidegesture.ui.screen.about.AboutVM.UiState
import kotlinx.coroutines.launch

/**
 * @author aaronzzxup@gmail.com
 * @since 2024/11/23
 */
class AboutVM : BaseComposeVM<UiState, UiEvent>() {

    override val initialState: UiState = UiState()

    init {
        loadSelfPackageInfo()
    }

    fun showMyAppsDialog(show: Boolean) {
        updateUiState {
            it.copy(showMyAppsDialog = show)
        }
    }

    fun showDonateDialog(show: Boolean) {
        updateUiState {
            it.copy(showDonateDialog = show)
        }
    }

    private fun loadSelfPackageInfo() {
        viewModelScope.launch {
            val context = App.getContext()
            val pm = context.packageManager
            val appInfo = context.applicationInfo
            val appName = appInfo?.loadLabel(pm)?.toString() ?: ""
            val icon = appInfo?.loadIcon(pm)
            val versionName = context.getString(R.string.version_placeholder, BuildConfig.VERSION_NAME)
            updateUiState {
                it.copy(
                    appName = appName,
                    icon = icon,
                    versionName = versionName
                )
            }
        }
    }

    data class UiState(
        val appName: String = "",
        val icon: Drawable? = null,
        val versionName: String = "",
        val showMyAppsDialog: Boolean = false,
        val showDonateDialog: Boolean = false
    )

    sealed interface UiEvent
}