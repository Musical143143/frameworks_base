package com.android.systemui.axdynamicbar.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.compose.theme.PlatformTheme
import com.android.systemui.shared.recents.utilities.Utilities
import com.android.systemui.axdynamicbar.domain.LocalLunarisBarConfig
import com.android.systemui.axdynamicbar.domain.LunarisBarConfig
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.ExpandedMaxWidth
import com.android.systemui.axdynamicbar.shared.allowsExpandedCard
import com.android.systemui.axdynamicbar.shared.bounceDampingRatio
import com.android.systemui.axdynamicbar.shared.scaledDurationMillis
import com.android.systemui.axdynamicbar.ui.compose.ExpandedIslandContent
import com.android.systemui.axdynamicbar.ui.compose.NotificationAlertCard
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Main
import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import android.view.WindowInsets
import androidx.compose.ui.input.pointer.PointerEvent

private const val EXIT_ANIM_DURATION = 300L

@SysUISingleton
class AxDynamicBarExpandedPanel
@Inject
constructor(
    @Application private val context: Context,
    private val windowManager: WindowManager,
    @Application private val applicationScope: CoroutineScope,
    @Main private val mainHandler: Handler,
    private val viewModel: AxDynamicBarChipViewModel,
) {
    private var overlayView: ComposeView? = null
    private var panelLifecycleOwner: PanelLifecycleOwner? = null
    private var hideOverlayJob: Job? = null

    fun init() {
        viewModel.interactor.onCollapseRequested = { viewModel.statusBarExpansion.collapse() }
        viewModel.interactor.onFocusableRequested = { focusable -> setOverlayFocusable(focusable) }

        val needsOverlay =
            combine(
                viewModel.isExpanded,
                viewModel.interactor.uiState.map { it.notificationAlert },
                viewModel.isOnKeyguard,
            ) { expanded, alert, onKeyguard ->
                !onKeyguard && (expanded || alert != null)
            }

        needsOverlay
            .onEach { needed ->
                if (needed) {
                    hideOverlayJob?.cancel()
                    hideOverlayJob = null
                    showOverlay()
                } else {
                    
                    hideOverlayJob?.cancel()
                    hideOverlayJob =
                        applicationScope.launch {
                            delay(400)
                            hideOverlay()
                        }
                }
            }
            .launchIn(applicationScope)

        viewModel.isExpanded
            .onEach { expanded ->
                updateOverlay(expanded)
            }
            .launchIn(applicationScope)
    }

    private fun ensureMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else mainHandler.post(action)
    }

    private fun showOverlay() {
        ensureMainThread { showOverlayInternal() }
    }

    private fun showOverlayInternal() {
        if (overlayView != null) return

        val lifecycleOwner = PanelLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val windowMetrics = windowManager.currentWindowMetrics
        val statusBarTop = windowMetrics.windowInsets
            .getInsets(WindowInsets.Type.statusBars())
            .top
        val hasCutout = windowMetrics.windowInsets
            .getInsets(WindowInsets.Type.displayCutout())
            .top > 0

        val view = ComposeView(context).apply {
            setContent { PlatformTheme { OverlayContent(viewModel, statusBarTop, hasCutout) } }
            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                    viewModel.statusBarExpansion.collapse()
                    true
                } else {
                    false
                }
            }
        }

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        view.setViewTreeOnBackPressedDispatcherOwner(lifecycleOwner)
        panelLifecycleOwner = lifecycleOwner

        val isCurrentlyExpanded = viewModel.isExpanded.value
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            if (isCurrentlyExpanded) 0
            else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL,
                flags,
                PixelFormat.TRANSLUCENT,
            )
        params.gravity = Gravity.TOP or Gravity.FILL_HORIZONTAL
        params.title = "AxDynamicBarExpanded"

        windowManager.addView(view, params)
        overlayView = view
    }

    private fun hideOverlay() {
        ensureMainThread { hideOverlayInternal() }
    }

    private fun hideOverlayInternal() {
        shrinkRunnable?.let { mainHandler.removeCallbacks(it) }
        shrinkRunnable = null
        overlayView?.let { view ->
            panelLifecycleOwner?.apply {
                handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
            windowManager.removeViewImmediate(view)
        }
        overlayView = null
        panelLifecycleOwner = null
    }

    private var shrinkRunnable: Runnable? = null

    private fun updateOverlay(expanded: Boolean) = ensureMainThread {
        shrinkRunnable?.let { mainHandler.removeCallbacks(it) }
        shrinkRunnable = null
        val view = overlayView ?: return@ensureMainThread
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return@ensureMainThread
        if (expanded) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(view, params)
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager.updateViewLayout(view, params)
            val runnable = Runnable {
                val v = overlayView ?: return@Runnable
                val p = v.layoutParams as? WindowManager.LayoutParams ?: return@Runnable
                p.height = WindowManager.LayoutParams.WRAP_CONTENT
                windowManager.updateViewLayout(v, p)
            }
            shrinkRunnable = runnable
            mainHandler.postDelayed(runnable, EXIT_ANIM_DURATION)
        }
    }

    private fun setOverlayFocusable(focusable: Boolean) = ensureMainThread {
        val view = overlayView ?: return@ensureMainThread
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return@ensureMainThread
        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(view, params)
    }
}

@Composable
private fun OverlayContent(viewModel: AxDynamicBarChipViewModel, statusBarHeightPx: Int, hasCutout: Boolean) {
    val lunaris by viewModel.lunarisBar.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLunarisBarConfig provides lunaris) {
        OverlayBody(viewModel, statusBarHeightPx, hasCutout, lunaris)
    }
}

// Clock cards (alarm and stopwatch) are always allowed here. The shared default of cardClock is
// false, so filtering them would hide them for every user. Once that default is true, route these
// through lunaris.allowsExpandedCard(event) as well.
private fun LunarisBarConfig.showsExpandedCard(event: IslandEvent): Boolean =
    allowsExpandedCard(event)

@Composable
private fun OverlayBody(
    viewModel: AxDynamicBarChipViewModel,
    statusBarHeightPx: Int,
    hasCutout: Boolean,
    lunaris: LunarisBarConfig,
) {
    val density = LocalDensity.current
    val isLargeScreen = Utilities.isLargeScreen(LocalContext.current)

    val largeScreenExtra = if (isLargeScreen) 4.dp else 0.dp
    val topPad = if (hasCutout) largeScreenExtra
        else if (lunaris.expansionPlacement == 1) 0.dp
        else with(density) { statusBarHeightPx.toDp() } + largeScreenExtra
    val chipState by viewModel.chipState.collectAsStateWithLifecycle()
    val isExpanded by viewModel.isExpanded.collectAsStateWithLifecycle()
    val uiState by viewModel.interactor.uiState.collectAsStateWithLifecycle()
    val isOnKeyguard by viewModel.isOnKeyguard.collectAsStateWithLifecycle()
    val notifAlert = uiState.notificationAlert
    val compactNotifs by viewModel.interactor.settings.compactNotifications.collectAsStateWithLifecycle()

    val lastAlert = remember { mutableStateOf<IslandEvent.Notification?>(null) }
    if (notifAlert != null) lastAlert.value = notifAlert

    val expandedVisible = remember { MutableTransitionState(false) }
    val showNotif = !isExpanded && notifAlert != null
    val notifVisible = remember { MutableTransitionState(false) }

    LaunchedEffect(isExpanded) {
        expandedVisible.targetState = isExpanded
    }

    LaunchedEffect(showNotif) {
        notifVisible.targetState = showNotif
    }

    LaunchedEffect(chipState, lunaris) {
        val filtered = chipState?.allEvents?.filter {
            it !is IslandEvent.AospChip && lunaris.showsExpandedCard(it)
        }
        if (filtered.isNullOrEmpty() && isExpanded) {
            viewModel.statusBarExpansion.collapse()
        }
    }

    val origin = TransformOrigin(0.5f, 0f)
    // Overlay motion. Default (tween, 100%, no reduce) gives the same specs as before.
    val bounceRatio = lunaris.bounceDampingRatio() ?: Spring.DampingRatioMediumBouncy
    fun motion(ms: Int): FiniteAnimationSpec<Float> = when {
        lunaris.reduceAnimations -> snap<Float>()
        lunaris.expansionStyle == 0 -> spring<Float>(dampingRatio = bounceRatio)
        else -> tween<Float>(lunaris.scaledDurationMillis(ms))
    }
    val scaleEnabled = lunaris.originalPopupAnimation

    AnimatedVisibility(
        visibleState = expandedVisible,
        enter = fadeIn(motion(250)) + (if (scaleEnabled) scaleIn(
            animationSpec = motion(350),
            initialScale = 0.4f,
            transformOrigin = origin,
        ) else EnterTransition.None),
        exit = fadeOut(motion(200)) + (if (scaleEnabled) scaleOut(
            animationSpec = motion(250),
            targetScale = 0.4f,
            transformOrigin = origin,
        ) else ExitTransition.None),
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        
                        var ev: PointerEvent
                        do {
                            ev = awaitPointerEvent(PointerEventPass.Final)
                        } while (!ev.changes.any { it.changedToDownIgnoreConsumed() })
                        
                        val downChange =
                            ev.changes.firstOrNull { it.changedToDownIgnoreConsumed() }
                                ?: ev.changes.firstOrNull()
                                ?: return@awaitEachGesture

                        val pointerId = downChange.id
                        val downPos = downChange.position
                        val downConsumed = downChange.isConsumed
                        var hasExceededSlop = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change =
                                event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull()
                                    ?: break

                            val dx = change.position.x - downPos.x
                            val dy = change.position.y - downPos.y
                            if (dx * dx + dy * dy > slop * slop) {
                                hasExceededSlop = true
                            }

                            if (!change.pressed) {
                                if (!downConsumed && !change.isConsumed && !hasExceededSlop) {
                                    change.consume()
                                    viewModel.statusBarExpansion.collapse()
                                }
                                break
                            }
                        }
                    }
                }
                .padding(top = topPad),
            contentAlignment = Alignment.TopCenter,
        ) {
            chipState?.let { state ->
                val filtered = state.allEvents.filter {
                    it !is IslandEvent.AospChip && lunaris.showsExpandedCard(it)
                }
                if (filtered.isEmpty()) return@let
                Box(
                    modifier =
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {                
                   ExpandedIslandContent(
                    events = filtered,
                    interactor = viewModel.interactor,
                    onCollapse = { viewModel.statusBarExpansion.collapse() },
                    pinnedEventId = state.event.id,
                    hapticsViewModelFactory = viewModel.interactor.sliderHapticsViewModelFactory,
                  ) 
                }
            }
        }
    }

    AnimatedVisibility(
        visibleState = notifVisible,
        enter = fadeIn(motion(300)) + (if (scaleEnabled) scaleIn(
            animationSpec = motion(300),
            initialScale = 0.4f,
            transformOrigin = origin,
        ) else EnterTransition.None),
        exit = fadeOut(motion(250)) + (if (scaleEnabled) scaleOut(
            animationSpec = motion(250),
            targetScale = 0.4f,
            transformOrigin = origin,
        ) else ExitTransition.None),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPad),
            contentAlignment = Alignment.TopCenter,
        ) {
            val alert = lastAlert.value
            if (alert != null) {
                NotificationAlertCard(
                    notification = alert,
                    interactor = viewModel.interactor,
                    onDismiss = { viewModel.interactor.dismissNotificationAlert() },
                    initiallyCompact = compactNotifs,
                    modifier =
                        (if (lunaris.expandedWidth == 1) Modifier
                            else Modifier.widthIn(max = ExpandedMaxWidth))
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                )
            }
        }
    }
}

private class PanelLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val backDispatcher = OnBackPressedDispatcher()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val onBackPressedDispatcher: OnBackPressedDispatcher
        get() = backDispatcher

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
