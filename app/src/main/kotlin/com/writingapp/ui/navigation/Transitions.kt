package com.writingapp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

private const val TransitionDuration = 350

val enterFromRight: EnterTransition = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(TransitionDuration)
) + fadeIn(animationSpec = tween(TransitionDuration))

val exitToLeft: ExitTransition = slideOutHorizontally(
    targetOffsetX = { -it },
    animationSpec = tween(TransitionDuration)
) + fadeOut(animationSpec = tween(TransitionDuration))

val enterFromLeft: EnterTransition = slideInHorizontally(
    initialOffsetX = { -it },
    animationSpec = tween(TransitionDuration)
) + fadeIn(animationSpec = tween(TransitionDuration))

val exitToRight: ExitTransition = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(TransitionDuration)
) + fadeOut(animationSpec = tween(TransitionDuration))

val enterFromBottom: EnterTransition = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(TransitionDuration)
) + fadeIn(animationSpec = tween(TransitionDuration))

val exitToBottom: ExitTransition = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(TransitionDuration)
) + fadeOut(animationSpec = tween(TransitionDuration))

val scaleIn: EnterTransition = scaleIn(
    initialScale = 0.9f,
    animationSpec = tween(TransitionDuration)
) + fadeIn(animationSpec = tween(TransitionDuration))

val scaleOut: ExitTransition = scaleOut(
    targetScale = 0.9f,
    animationSpec = tween(TransitionDuration)
) + fadeOut(animationSpec = tween(TransitionDuration))
