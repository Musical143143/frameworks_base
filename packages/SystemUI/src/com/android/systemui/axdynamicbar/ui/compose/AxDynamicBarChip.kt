package com.android.systemui.axdynamicbar.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kotlinx.coroutines.withTimeoutOrNull
import com.android.systemui.axdynamicbar.domain.LocalLunarisBarConfig
import com.android.systemui.axdynamicbar.domain.LunarisBarConfig
import com.android.systemui.axdynamicbar.shared.SubtleGray
import com.android.systemui.axdynamicbar.shared.bounceDampingRatio
import com.android.systemui.axdynamicbar.shared.compactChipBackground
import com.android.systemui.axdynamicbar.shared.compactChipContent
import com.android.systemui.axdynamicbar.shared.compactChipShape
import com.android.systemui.axdynamicbar.shared.musicCardBackground
import com.android.systemui.axdynamicbar.shared.musicCardContent
import com.android.systemui.axdynamicbar.shared.outlineColor
import com.android.systemui.axdynamicbar.shared.resolvedCompactShape
import com.android.systemui.axdynamicbar.shared.scaledDurationMillis
import com.android.systemui.axdynamicbar.shared.shadowColor
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.dimensionResource
import com.android.compose.animation.Expandable
import com.android.compose.animation.rememberExpandableController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.graphics.drawable.Drawable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.stringResource
import com.android.systemui.axdynamicbar.model.IslandEvent
import com.android.systemui.axdynamicbar.shared.AlphaIconBg
import com.android.systemui.axdynamicbar.shared.AlphaSecondary
import com.android.systemui.axdynamicbar.shared.AlphaTertiary
import com.android.systemui.axdynamicbar.shared.PillPrimary
import com.android.systemui.axdynamicbar.shared.ShapeXl
import com.android.systemui.axdynamicbar.shared.ShapeXs
import com.android.systemui.axdynamicbar.shared.SizeBadge
import com.android.systemui.axdynamicbar.shared.SpaceMd
import com.android.systemui.axdynamicbar.shared.SpaceSm
import com.android.systemui.axdynamicbar.shared.SpaceXs
import com.android.systemui.axdynamicbar.shared.TsBadge
import com.android.systemui.axdynamicbar.shared.chipAccentColorFor
import com.android.systemui.axdynamicbar.shared.chipContentColorOn
import com.android.systemui.axdynamicbar.shared.chipProgressFor
import com.android.systemui.axdynamicbar.shared.textKeyFor
import com.android.systemui.axdynamicbar.shared.toScaledBitmap
import com.android.systemui.axdynamicbar.ui.AxDynamicBarChipViewModel
import com.android.systemui.res.R
import kotlin.math.abs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

private val ChipShape = ShapeXl
private val ChipHeight = 24.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AxDynamicBarChip(
    viewModel: AxDynamicBarChipViewModel,
    modifier: Modifier = Modifier,
    ignoreKeyguard: Boolean = false,
) {
    val lunarisBar by viewModel.lunarisBar.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLunarisBarConfig provides lunarisBar) {
        AxDynamicBarChipContent(
            viewModel = viewModel,
            modifier = modifier,
            ignoreKeyguard = ignoreKeyguard,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AxDynamicBarChipContent(
    viewModel: AxDynamicBarChipViewModel,
    modifier: Modifier,
    ignoreKeyguard: Boolean,
) {
    val lunaris = LocalLunarisBarConfig.current
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val state by viewModel.chipState.collectAsStateWithLifecycle()
    val isOnKeyguard by viewModel.isOnKeyguard.collectAsStateWithLifecycle()
    val keyguardCarrier by viewModel.keyguardCarrierText.collectAsStateWithLifecycle()
    val chipStyle by viewModel.chipStyle.collectAsStateWithLifecycle()
    val isMediaEventTypeEnabled by viewModel.isMediaEventTypeEnabled.collectAsStateWithLifecycle()
    val isExpanded by viewModel.isExpanded.collectAsStateWithLifecycle()
    val mediaChipEnabled = isMediaEventTypeEnabled && lunaris.showMusicChip
    val chipShape: Shape =
        if (lunaris.compactShape == null) ChipShape else lunaris.compactChipShape(chipStyle == 1)
    val widthScale = lunaris.chipScalePct * lunaris.chipWidthPct / 10000f
    val heightScale = lunaris.chipScalePct * lunaris.chipHeightPct / 10000f

    var toggleCount by remember { mutableIntStateOf(0) }

    val displayEvent: IslandEvent? = state?.let { chipState ->
        val raw = chipState.notificationAlert ?: chipState.event
        when {
            mediaChipEnabled -> raw
            raw is IslandEvent.Media -> chipState.allEvents.firstOrNull { it !is IslandEvent.Media }
            else -> raw
        }
    }
    
    val carrierName = if (isOnKeyguard && ignoreKeyguard) keyguardCarrier.takeIf { it.isNotBlank() } else null
    val chipTextMaxWidth = dimensionResource(R.dimen.ongoing_activity_chip_max_text_width)
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    val touchSlop = LocalViewConfiguration.current.touchSlop
    val expandableController = rememberExpandableController(color = Color.Transparent, shape = ChipShape)

    val motionScheme = MaterialTheme.motionScheme

    val viewConfig = LocalViewConfiguration.current

    // Runs one configured chip action. Tap action 1 is the existing expand toggle.
    val currentDisplayEvent by rememberUpdatedState(displayEvent)
    val runGestureAction: (Int, Boolean) -> Unit = { action, withHaptic ->
        if (withHaptic && lunaris.hapticFeedback) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        when (action) {
            1 -> {
                val wasExpanded = viewModel.statusBarExpansion.isExpanded.value
                val current = state?.event
                val handledByAosp =
                    current is IslandEvent.AospChip &&
                        viewModel.handleAospChipTap(current, expandableController.expandable)
                if (!handledByAosp) {
                    viewModel.statusBarExpansion.toggle()
                    if (!wasExpanded) toggleCount++
                }
            }
            2 -> {
                val pkg = currentDisplayEvent?.let { chipAppPackage(it) }
                val intent =
                    if (pkg.isNullOrEmpty()) null
                    else context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                } else {
                    viewModel.interactor.openMediaApp()
                }
            }
            3 -> viewModel.togglePlayPause()
            4 -> viewModel.skipNext()
            else -> Unit
        }
    }

    val chipVisible =
        state != null &&
            displayEvent != null &&
            (ignoreKeyguard || !isOnKeyguard) &&
            (lunaris.showPeekChip || isExpanded) &&
            !(lunaris.hidePaused && displayEvent is IslandEvent.Media && !displayEvent.isPlaying)

    // Resting capsule shown when nothing is displayed and the option keeps it visible.
    if (lunaris.keepCapsuleVisible && state == null) {
        val capsuleHeight = lunaris.restingHeightDp.dp
        Box(
            modifier =
                modifier
                    .padding(start = 4.dp, end = 2.dp)
                    .size(width = capsuleHeight * 2f, height = capsuleHeight)
                    .clip(lunaris.compactChipShape(chipStyle == 1))
                    .background(lunaris.compactChipBackground(SubtleGray))
        )
    }

    val bounce = lunaris.bounceDampingRatio()
    val enterSpatial: FiniteAnimationSpec<Float> =
        if (bounce != null) spring(dampingRatio = bounce, stiffness = Spring.StiffnessMediumLow)
        else motionScheme.defaultSpatialSpec()
    val exitSpatial: FiniteAnimationSpec<Float> =
        if (bounce != null) spring(dampingRatio = bounce, stiffness = Spring.StiffnessMediumLow)
        else motionScheme.fastSpatialSpec()
    val enterTransition: EnterTransition =
        when {
            lunaris.reduceAnimations -> EnterTransition.None
            lunaris.fadeChipAppear ->
                fadeIn(motionScheme.defaultEffectsSpec()) +
                    scaleIn(initialScale = 0.8f, animationSpec = enterSpatial)
            else -> scaleIn(initialScale = 0.8f, animationSpec = enterSpatial)
        }
    val exitTransition: ExitTransition =
        if (lunaris.reduceAnimations) ExitTransition.None
        else
            fadeOut(motionScheme.fastEffectsSpec()) +
                scaleOut(targetScale = 0.8f, animationSpec = exitSpatial)

    AnimatedVisibility(
        visible = chipVisible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier
            .then(
                if (lunaris.horizontalPositionDp != 0 || lunaris.verticalPositionDp != 0) {
                    Modifier.offset(
                        x = lunaris.horizontalPositionDp.dp,
                        y = lunaris.verticalPositionDp.dp,
                    )
                } else {
                    Modifier
                }
            )
            .padding(start = 4.dp, end = 2.dp)
            .widthIn(min = 25.dp, max = (90.dp * widthScale).coerceAtLeast(25.dp))
            .pointerInput(viewModel, lunaris) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    
                    val startX = down.position.x
                    val startY = down.position.y
                    var dragging = false
                    var totalDx = 0f
                    var decided = false
                    var longPressArmed = lunaris.gestureLongPress != 0
                    var longPressFired = false
                    val longPressDeadline = down.uptimeMillis + viewConfig.longPressTimeoutMillis 
                    while (true) {
                        val event =
                            if (longPressArmed) {
                                withTimeoutOrNull(
                                    (longPressDeadline - SystemClock.uptimeMillis()).coerceAtLeast(1L)
                                ) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                }
                            } else {
                                awaitPointerEvent(PointerEventPass.Initial)
                            }
                        if (event == null) {
                            // Held still past the long press timeout.
                            longPressFired = true
                            runGestureAction(lunaris.gestureLongPress, true)
                            break
                        }
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            
                            if (dragging) {
                                change.consume()
                                if (totalDx > 0) viewModel.cyclePrev()
                                else viewModel.cycleNext()
                            } else if (!decided) {

                                change.consume()
                                if (lunaris.gestureDoubleTap == 0) {
                                    runGestureAction(lunaris.gestureTap, false)
                                } else {
                                    // Wait for a possible second tap before acting on this one.
                                    val second =
                                        withTimeoutOrNull(viewConfig.doubleTapTimeoutMillis) {
                                            awaitFirstDown(pass = PointerEventPass.Initial)
                                        }
                                    if (second == null) {
                                        runGestureAction(lunaris.gestureTap, false)
                                    } else {
                                        second.consume()
                                        while (true) {
                                            val held = awaitPointerEvent(PointerEventPass.Initial)
                                            val heldChange = held.changes.firstOrNull() ?: break
                                            heldChange.consume()
                                            if (!heldChange.pressed) break
                                        }
                                        runGestureAction(lunaris.gestureDoubleTap, true)
                                    }
                                }
                            }
                            
                            break
                        }
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (!decided && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                            if (abs(dx) >= abs(dy)) {
                                
                                decided = true
                                dragging = true
                                longPressArmed = false
                                totalDx = dx
                                change.consume()
                            } else {
                                
                                decided = true
                                break
                            }
                        } else if (dragging) {
                            totalDx = dx
                            change.consume()
                        }
                    }
                    if (longPressFired) {
                        // Hold until release so the rest of this press fires nothing else.
                        while (true) {
                            val held = awaitPointerEvent(PointerEventPass.Initial)
                            val heldChange = held.changes.firstOrNull() ?: break
                            heldChange.consume()
                            if (!heldChange.pressed) break
                        }
                    }
                }
            }
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                val centerX = (bounds.left + bounds.right) / 2f
                if (screenWidthPx > 0f) {
                    viewModel.updateChipCenterX(centerX / screenWidthPx)
                }
            },
    ) {
        state?.let { chipState ->
            val isAlert = chipState.notificationAlert != null

            val event = displayEvent ?: return@let

            Expandable(
                controller = expandableController,
                onClick = null,
                defaultMinSize = false,
            ) { _ ->
                AnimatedContent(
                    targetState = chipDisplayKey(displayEvent, isAlert),
                    transitionSpec = {
                        if (lunaris.reduceAnimations) {
                            (EnterTransition.None togetherWith ExitTransition.None).using(sizeTransform = null)
                        } else ((
                            fadeIn(motionScheme.defaultEffectsSpec()) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = motionScheme.defaultSpatialSpec(),
                                )
                            ) togetherWith (
                            fadeOut(motionScheme.fastEffectsSpec()) +
                                scaleOut(
                                    targetScale = 0.92f,
                                    animationSpec = motionScheme.fastSpatialSpec(),
                                )
                            ))
                            .using(sizeTransform = null)
                    },
                    label = "chip_event",
                ) {
                    val event = displayEvent
                    val rawAccent = chipAccentColorFor(event)
                    val isMusicChip = lunaris.customizeMusicChip && event is IslandEvent.Media
                    val styledBase =
                        if (isMusicChip) lunaris.musicCardBackground(rawAccent)
                        else lunaris.compactChipBackground(rawAccent)
                    val styledAccent =
                        if (lunaris.blurChip) styledBase.copy(alpha = styledBase.alpha * 0.6f)
                        else styledBase
                    val accent by animateColorAsState(styledAccent, MaterialTheme.motionScheme.fastEffectsSpec(), label = "accent")
                    val contentColor by animateColorAsState(
                        if (isMusicChip) lunaris.musicCardContent(chipContentColorOn(styledAccent))
                        else lunaris.compactChipContent(chipContentColorOn(styledAccent)),
                        MaterialTheme.motionScheme.fastEffectsSpec(), label = "content",
                    )
                    val statusBarEventCount = remember(chipState.allEvents, mediaChipEnabled) {
                        if (mediaChipEnabled) chipState.eventCount
                        else chipState.allEvents.count { it !is IslandEvent.Media }
                    }
                val appLabel: String? =
                    if (lunaris.playerIdentification == 1) chipAppLabel(context, event) else null
                val trailingDrawable: Drawable? =
                    when (lunaris.trailingContent) {
                        0 -> chipAppIcon(event)
                        1 -> (event as? IslandEvent.Media)?.albumArt
                        else -> null
                    }
                val useCircleStyle = lunaris.resolvedCompactShape(chipStyle == 1) == 0 &&
                        !isAlert &&
                        event !is IslandEvent.AudioRecording &&
                        event !is IslandEvent.Timer &&
                        event !is IslandEvent.Stopwatch &&
                        !(event is IslandEvent.Sports && event.team2Name.isNotEmpty())

                val progress = chipProgressFor(event, includeMediaProgress = useCircleStyle)

                if (useCircleStyle) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircleChip(
                            event = event,
                            accent = accent,
                            contentColor = contentColor,
                            progress = progress,
                            modifier =
                                if (lunaris.reduceAnimations) Modifier
                                else Modifier.squishAnimation(toggleCount, lunaris),
                        )
                    }
                } else {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier =
                            Modifier.height(ChipHeight * heightScale)
                                .widthIn(max = 100.dp * widthScale)
                                .chipShadow(lunaris, chipShape)
                                .clip(chipShape)
                                .then(
                                    if (lunaris.reduceAnimations) Modifier
                                    else Modifier.squishAnimation(toggleCount, lunaris)
                                )
                                .background(accent)
                                .chipBorder(lunaris, chipShape)
                                .then(
                                    if (progress != null) {
                                        val trackColor = lerp(accent, contentColor, 0.2f)
                                        val fillColor = lerp(accent, contentColor, 0.6f)
                                        Modifier.drawWithContent {
                                            drawContent()
                                            val barH = 2.dp.toPx()
                                            val y = size.height - barH
                                            drawRect(
                                                trackColor,
                                                topLeft = Offset(0f, y),
                                                size = Size(size.width, barH),
                                            )
                                            drawRect(
                                                fillColor,
                                                topLeft = Offset(0f, y),
                                                size = Size(size.width * progress, barH),
                                            )
                                        }
                                    } else Modifier
                                )
                                .padding(start = SpaceSm, end = SpaceMd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (carrierName != null) {
                            Text(
                                text = carrierName,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = AlphaSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 56.dp),
                            )
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor.copy(alpha = AlphaTertiary),
                            )
                        }
                        if (isAlert && event is IslandEvent.Notification) {
                            val notif = event
                            AnimatedContent(
                                targetState = notif.sbn.key,
                                transitionSpec = {
                                    (if (lunaris.reduceAnimations) (EnterTransition.None togetherWith ExitTransition.None) else (fadeIn(motionScheme.defaultEffectsSpec()) togetherWith fadeOut(motionScheme.fastEffectsSpec())))
                                        .using(sizeTransform = null)
                                },
                                label = "alert_content",
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    notif.appIcon?.let { icon ->
                                        Image(
                                            bitmap = icon.toScaledBitmap(16.dp),
                                            contentDescription = null,
                                            modifier =
                                                Modifier.size(16.dp)
                                                    .clip(ShapeXs),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(Modifier.width(SpaceXs))
                                    }
                                    Text(
                                        text = notif.appName ?: "",
                                        style = PillPrimary,
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = chipTextMaxWidth).basicMarquee(iterations = 1),
                                    )
                                }
                            }
                        } else if (event is IslandEvent.Sports && event.team2Name.isNotEmpty()) {
                            val sport = event
                            StatusBarSportsTeamBadge(sport.team1Name, sport.team1Icon, contentColor)
                            Spacer(Modifier.width(SpaceXs))
                            Text(
                                if (sport.score1.isNotEmpty()) "${sport.score1} - ${sport.score2}"
                                    else stringResource(R.string.ax_dynamic_bar_sports_vs),
                                style = PillPrimary,
                                color = contentColor,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(SpaceXs))
                            StatusBarSportsTeamBadge(sport.team2Name, sport.team2Icon, contentColor)
                        } else {
                            if (lunaris.playerIdentification == 1) {
                                appLabel?.let { name ->
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 56.dp),
                                    )
                                    Spacer(Modifier.width(SpaceXs))
                                }
                            }
                            if (lunaris.playerIdentification == 0) {
                            AnimatedContent(
                                targetState = chipIconKey(event),
                                transitionSpec = {
                                    (if (lunaris.reduceAnimations) (EnterTransition.None togetherWith ExitTransition.None) else (fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                                        fadeOut(motionScheme.fastEffectsSpec())))
                                        .using(sizeTransform = null)
                                },
                                label = "chip_icon",
                            ) {
                                PillEventIcon(event, tint = contentColor, animated = lunaris.animateEqualizer)
                            }
                            Spacer(Modifier.width(SpaceXs))
                            }
                            AnimatedContent(
                                targetState = textKeyFor(event),
                                transitionSpec = {
                                    (if (lunaris.reduceAnimations) (EnterTransition.None togetherWith ExitTransition.None) else (fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                                        fadeOut(motionScheme.fastEffectsSpec())))
                                        .using(sizeTransform = null)
                                },
                                label = "chip_text",
                                modifier = Modifier.weight(1f, fill = false).widthIn(max = chipTextMaxWidth),
                            ) {
                                PillEventText(
                                    event,
                                    Modifier.widthIn(max = chipTextMaxWidth),
                                    overrideColor = contentColor,
                                )
                            }
                            if (statusBarEventCount > 1) {
                                Spacer(Modifier.width(SpaceXs))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .height(SizeBadge)
                                        .widthIn(min = SizeBadge)
                                        .background(
                                            lerp(accent, contentColor, 0.3f),
                                            RoundedCornerShape(SizeBadge / 2),
                                        )
                                        .padding(horizontal = 3.dp),
                                ) {
                                    Text(
                                        text = "$statusBarEventCount",
                                        style = TsBadge,
                                        color = contentColor,
                                        maxLines = 1,
                                    )
                                }
                            }
                            trailingDrawable?.let { trailingIcon ->
                                Spacer(Modifier.width(SpaceXs))
                                Image(
                                    bitmap = trailingIcon.toScaledBitmap(16.dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).clip(ShapeXs),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
                }
            }
            }
        }
    }
}

@Composable
private fun StatusBarSportsTeamBadge(name: String, icon: Drawable?, contentColor: Color) {
    val badgeSize = 16.dp
    if (icon != null) {
        Image(
            bitmap = icon.toScaledBitmap(badgeSize),
            contentDescription = name,
            modifier = Modifier.size(badgeSize).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(badgeSize).clip(CircleShape)
                .background(contentColor.copy(alpha = AlphaIconBg)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(2).uppercase(),
                style = TsBadge,
                color = contentColor,
            )
        }
    }
}


private fun chipDisplayKey(event: IslandEvent, isAlert: Boolean): String =
    if (isAlert) "alert:${event.id}" else event.id

private fun chipIconKey(event: IslandEvent): Any =
    when (event) {
        is IslandEvent.AppSwitch -> {
            val app = event.previousApp ?: event.recentApps.firstOrNull()
            app?.taskId ?: app?.packageName ?: event.id
        }
        is IslandEvent.Media -> event.albumArt ?: event.packageName
        is IslandEvent.Notification -> event.sbn.key
        else -> event.id
    }

@Composable
private fun Modifier.squishAnimation(toggleCount: Int, lunaris: LunarisBarConfig): Modifier {
    val scaleX = remember { Animatable(1f, visibilityThreshold = 0.01f) }
    val scaleY = remember { Animatable(1f, visibilityThreshold = 0.01f) }
    val currentToggleCount by rememberUpdatedState(toggleCount)
    val currentLunaris by rememberUpdatedState(lunaris)

    LaunchedEffect(Unit) {
        snapshotFlow { currentToggleCount }
            .drop(1)
            .collectLatest {
                scaleX.snapTo(1f)
                scaleY.snapTo(1f)
                coroutineScope {
                    launch {
                        scaleX.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = currentLunaris.scaledDurationMillis(400)
                                1.06f at currentLunaris.scaledDurationMillis(120) using FastOutSlowInEasing
                                0.97f at currentLunaris.scaledDurationMillis(260)
                                1f at currentLunaris.scaledDurationMillis(400)
                            },
                        )
                    }
                    launch {
                        scaleY.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = currentLunaris.scaledDurationMillis(400)
                                0.95f at currentLunaris.scaledDurationMillis(120) using FastOutSlowInEasing
                                1.03f at currentLunaris.scaledDurationMillis(260)
                                1f at currentLunaris.scaledDurationMillis(400)
                            },
                        )
                    }
                }
            }
    }

    return this.graphicsLayer {
        this.scaleX = scaleX.value
        this.scaleY = scaleY.value
    }
}

private fun Modifier.chipShadow(lunaris: LunarisBarConfig, shape: Shape): Modifier =
    if (lunaris.shadowElevationDp > 0) {
        this.shadow(
            elevation = lunaris.shadowElevationDp.dp,
            shape = shape,
            clip = false,
            ambientColor = lunaris.shadowColor(),
            spotColor = lunaris.shadowColor(),
        )
    } else {
        this
    }

// Blur is an approximation: a faint 1dp light border when no outline is set.
private fun Modifier.chipBorder(lunaris: LunarisBarConfig, shape: Shape): Modifier =
    when {
        lunaris.outlineWidthDp > 0 ->
            this.border(lunaris.outlineWidthDp.dp, lunaris.outlineColor(), shape)
        lunaris.blurChip -> this.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
        else -> this
    }

/** Package behind an event, or null when the event does not name one. */
private fun chipAppPackage(event: IslandEvent): String? =
    when (event) {
        is IslandEvent.Media -> event.packageName.takeIf { it.isNotEmpty() }
        is IslandEvent.AppSwitch ->
            (event.previousApp ?: event.recentApps.firstOrNull())?.packageName
        is IslandEvent.Notification -> event.sbn.packageName
        else -> null
    }

private fun chipAppIcon(event: IslandEvent): Drawable? =
    when (event) {
        is IslandEvent.Media -> event.appIcon
        is IslandEvent.PromotedOngoing -> event.appIcon
        is IslandEvent.Notification -> event.appIcon
        is IslandEvent.Call -> event.appIcon
        is IslandEvent.Sports -> event.appIcon
        is IslandEvent.NowPlaying -> event.appIcon
        is IslandEvent.Timer -> event.appIcon
        is IslandEvent.Stopwatch -> event.appIcon
        is IslandEvent.Alarm -> event.appIcon
        is IslandEvent.AudioRecording -> event.appIcon
        else -> null
    }

private fun chipAppLabel(context: Context, event: IslandEvent): String? {
    if (event is IslandEvent.PromotedOngoing && event.appName.isNotEmpty()) return event.appName
    if (event is IslandEvent.Notification && event.appName.isNotEmpty()) return event.appName
    val pkg = chipAppPackage(event)
    if (pkg.isNullOrEmpty()) return null
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        null
    }
}
