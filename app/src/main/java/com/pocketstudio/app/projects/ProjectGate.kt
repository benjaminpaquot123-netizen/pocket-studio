package com.pocketstudio.app.projects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.OpenedProject
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Reads the project to open off the main thread ([ProjectOpening.prepare]) before the screens are
 * made on it. Lives with them (the activity's ViewModels, cleared when another project opens), so
 * a turn of the phone does not read it again.
 */
class ProjectOpeningViewModel(app: Application) : AndroidViewModel(app) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    private var opened: OpenedProject? = null

    init {
        viewModelScope.launch {
            opened = try {
                ProjectOpening.prepare(app)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The screens then read it themselves (as before), rather than never open.
                AppLog.add("Ouverture en arrière-plan impossible : ${e.message}")
                null
            }
            _ready.value = true
        }
    }

    override fun onCleared() {
        ProjectOpening.forget(opened)
    }
}

/** The screens once the project is read ([ProjectOpeningViewModel]); « Ouverture… » until then. */
@Composable
fun ProjectGate(content: @Composable () -> Unit) {
    val opening: ProjectOpeningViewModel = viewModel()
    val ready by opening.ready.collectAsStateWithLifecycle()
    if (ready) content() else OpeningScreen()
}

@Composable
private fun OpeningScreen() {
    Box(Modifier.fillMaxSize().background(Neon.Background).testTag("project_opening"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(Modifier.size(36.dp), color = Neon.Cyan)
            Text(tr("Ouverture du projet…"), style = MaterialTheme.typography.bodyLarge, color = Neon.TextSecondary)
        }
    }
}

/**
 * The app's own ViewModels, kept when another project opens (audit 16: the activity's were all
 * cleared then, which checked for an update on GitHub again and cut the MIDI keyboard off): held
 * by the application, cleared when the activity really ends (not when it is rebuilt).
 */
class AppViewModels : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    companion object {
        private val stores = java.util.WeakHashMap<android.content.Context, AppViewModels>()

        fun of(app: android.content.Context): AppViewModels = synchronized(stores) { stores.getOrPut(app.applicationContext) { AppViewModels() } }
    }
}

/**
 * A ViewModel of the app itself (not of the open project): see [AppViewModels]. Made with the
 * activity's own factory.
 */
@Composable
inline fun <reified T : ViewModel> appViewModel(): T {
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current) { "no ViewModelStoreOwner" }
    val context = LocalContext.current
    val owner = remember(context) { AppViewModels.of(context) }
    KeepAppViewModels(owner)
    return viewModel(
        viewModelStoreOwner = owner,
        factory = (activityOwner as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory,
        extras = (activityOwner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
            ?: androidx.lifecycle.viewmodel.CreationExtras.Empty,
    )
}

/** The app's ViewModels cleared when the activity ends for good (back out, finished), not when rebuilt. */
@PublishedApi
@Composable
internal fun KeepAppViewModels(owner: AppViewModels) {
    val activity = LocalContext.current.findComponentActivity() ?: return
    DisposableEffect(activity, owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY && !activity.isChangingConfigurations) owner.viewModelStore.clear()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
}

private tailrec fun android.content.Context.findComponentActivity(): androidx.activity.ComponentActivity? = when (this) {
    is androidx.activity.ComponentActivity -> this
    is android.content.ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
