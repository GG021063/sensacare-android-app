package com.sensacare.app.presentation.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.sensacare.app.R
import com.sensacare.app.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * CommonComponents - Reusable UI components for the SensaCare app
 *
 * This file contains a comprehensive set of common UI components that are used
 * throughout the SensaCare app, ensuring a consistent look and feel while
 * promoting code reuse and maintainability.
 */

//region Loading Components

/**
 * LoadingView - Fullscreen loading indicator with message
 *
 * @param message Optional message to display during loading
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun LoadingView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Loading: $message"
                    }
            )
        }
    }
}

/**
 * LoadingScreen - Fullscreen loading with animation and branding
 *
 * @param message Optional message to display during loading
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun LoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    // Animation for logo pulsing
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo or icon with pulsing effect
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = "SensaCare Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Loading: $message"
                    }
            )
        }
    }
}

/**
 * ContentLoadingIndicator - Inline loading indicator for content areas
 *
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ContentLoadingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Loading content...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

/**
 * ShimmerLoadingEffect - Skeleton loading effect
 *
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ShimmerLoadingEffect(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
    
    Box(
        modifier = modifier
            .background(brush)
    )
}

//endregion

//region Error Components

/**
 * ErrorView - Error state with retry option
 *
 * @param message Error message to display
 * @param onRetry Callback for retry action
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Error: $message"
                    }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Retry"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(text = "Retry")
            }
        }
    }
}

/**
 * ErrorCard - Card-based error display for inline errors
 *
 * @param message Error message to display
 * @param onDismiss Optional callback to dismiss the error
 * @param onRetry Optional callback for retry action
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ErrorCard(
    message: String,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Error: $message"
                    }
            )
            
            if (onRetry != null) {
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Retry"
                    }
                ) {
                    Text(text = "Retry")
                }
            }
            
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics {
                        contentDescription = "Dismiss error"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * NetworkErrorView - Specific error view for network issues
 *
 * @param onRetry Callback for retry action
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun NetworkErrorView(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SignalWifiConnectedNoInternet4,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Network Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please check your internet connection and try again.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(text = "Try Again")
        }
    }
}

//endregion

//region Section Headers

/**
 * SectionHeader - Header with title, subtitle, and optional view all action
 *
 * @param title Section title
 * @param subtitle Optional subtitle
 * @param showViewAll Whether to show the "View All" action
 * @param onViewAll Callback for view all action
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    showViewAll: Boolean = false,
    onViewAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            
            if (showViewAll && onViewAll != null) {
                TextButton(
                    onClick = onViewAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "View all $title"
                    }
                ) {
                    Text(text = "View All")
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * CollapsibleSectionHeader - Expandable section header with content
 *
 * @param title Section title
 * @param expanded Whether the section is expanded
 * @param onToggleExpanded Callback to toggle expansion
 * @param content Content to display when expanded
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Header with expand/collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics {
                    heading()
                    expanded = expanded
                    onClick { onToggleExpanded(); true }
                }
            )
            
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Expandable content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            content()
        }
    }
}

//endregion

//region Animated Components

/**
 * AnimatedCounter - Animated numeric counter for real-time metrics
 *
 * @param count Current count value
 * @param style Text style for the counter
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun AnimatedCounter(
    count: Int,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    modifier: Modifier = Modifier
) {
    var oldCount by remember { mutableStateOf(count) }
    var animatedCount by remember { mutableStateOf(count.toFloat()) }
    
    LaunchedEffect(count) {
        if (count != oldCount) {
            val start = oldCount
            val end = count
            
            // Animate from old value to new value
            val animation = TargetBasedAnimation(
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                ),
                typeConverter = Float.VectorConverter,
                initialValue = start.toFloat(),
                targetValue = end.toFloat()
            )
            
            val startTime = withFrameNanos { it }
            
            do {
                val playTime = withFrameNanos { it } - startTime
                animatedCount = animation.getValueFromNanos(playTime)
            } while (!animation.isFinishedFromNanos(playTime))
            
            oldCount = count
        }
    }
    
    Text(
        text = animatedCount.roundToInt().toString(),
        style = style,
        modifier = modifier.semantics {
            contentDescription = "$count"
        }
    )
}

/**
 * PulsingEffect - Creates a pulsing animation effect for a composable
 *
 * @param content Content to apply the pulsing effect to
 */
@Composable
fun PulsingEffect(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulsingEffect")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsingScale"
    )
    
    Box(
        modifier = Modifier.scale(scale),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * FadeInContent - Fades in content when it appears
 *
 * @param content Content to fade in
 * @param durationMillis Duration of the fade-in animation
 */
@Composable
fun FadeInContent(
    content: @Composable () -> Unit,
    durationMillis: Int = 500
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = durationMillis))
    ) {
        content()
    }
}

/**
 * SlideInContent - Slides in content from a direction
 *
 * @param content Content to slide in
 * @param durationMillis Duration of the slide-in animation
 * @param direction Direction to slide from
 */
@Composable
fun SlideInContent(
    content: @Composable () -> Unit,
    durationMillis: Int = 500,
    direction: AnimatedContentTransitionScope.SlideDirection = AnimatedContentTransitionScope.SlideDirection.Start
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = when (direction) {
            AnimatedContentTransitionScope.SlideDirection.Start -> 
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = durationMillis)
                )
            AnimatedContentTransitionScope.SlideDirection.End -> 
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = durationMillis)
                )
            AnimatedContentTransitionScope.SlideDirection.Up -> 
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = durationMillis)
                )
            AnimatedContentTransitionScope.SlideDirection.Down -> 
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = durationMillis)
                )
        } + fadeIn(animationSpec = tween(durationMillis = durationMillis))
    ) {
        content()
    }
}

//endregion

//region Empty State Components

/**
 * EmptyStateView - Generic empty state component
 *
 * @param icon Icon to display
 * @param title Title text
 * @param message Descriptive message
 * @param actionText Optional action button text
 * @param onAction Optional action callback
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}

/**
 * EmptyListView - Empty state specifically for empty lists
 *
 * @param message Message to display
 * @param actionText Optional action button text
 * @param onAction Optional action callback
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun EmptyListView(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.List,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}

//endregion

//region Progress Indicators and Charts

/**
 * CircularProgressWithLabel - Circular progress indicator with percentage label
 *
 * @param progress Progress value between 0.0 and 1.0
 * @param color Color of the progress indicator
 * @param size Size of the progress indicator
 * @param strokeWidth Width of the progress stroke
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun CircularProgressWithLabel(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Background circle
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = size.toPx() / 2 - strokeWidth.toPx() / 2,
                style = Stroke(width = strokeWidth.toPx())
            )
        }
        
        // Progress arc
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
        
        // Percentage text
        Text(
            text = "${(animatedProgress.value * 100).roundToInt()}%",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * HorizontalProgressBar - Horizontal progress bar with label and percentage
 *
 * @param progress Progress value between 0.0 and 1.0
 * @param label Optional label to display
 * @param color Color of the progress indicator
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun HorizontalProgressBar(
    progress: Float,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "HorizontalProgressAnimation"
    )
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (label != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "${(animatedProgress.value * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        LinearProgressIndicator(
            progress = { animatedProgress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = progress,
                        range = 0f..1f
                    )
                },
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

/**
 * SegmentedProgressBar - Progress bar divided into segments
 *
 * @param segments List of progress segments with values and colors
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun SegmentedProgressBar(
    segments: List<ProgressSegment>,
    modifier: Modifier = Modifier
) {
    // Ensure segments sum to 1.0
    val totalValue = segments.sumOf { it.value.toDouble() }.toFloat()
    val normalizedSegments = if (totalValue > 0f) {
        segments.map { it.copy(value = it.value / totalValue) }
    } else {
        segments
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics {
                contentDescription = "Segmented progress bar with ${segments.size} segments"
            }
    ) {
        normalizedSegments.forEach { segment ->
            val animatedWeight = animateFloatAsState(
                targetValue = segment.value,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "SegmentAnimation"
            )
            
            Box(
                modifier = Modifier
                    .weight(animatedWeight.value)
                    .fillMaxHeight()
                    .background(segment.color)
            )
        }
    }
}

/**
 * Data class representing a segment in a segmented progress bar
 */
data class ProgressSegment(
    val value: Float,
    val color: Color,
    val label: String? = null
)

/**
 * RadialProgressIndicator - Circular progress with customizable start/end angles
 *
 * @param progress Progress value between 0.0 and 1.0
 * @param color Color of the progress indicator
 * @param backgroundColor Background color
 * @param strokeWidth Width of the progress stroke
 * @param startAngle Starting angle in degrees
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun RadialProgressIndicator(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 8.dp,
    startAngle: Float = -90f,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "RadialProgressAnimation"
    )
    
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = progress,
                    range = 0f..1f
                )
            }
    ) {
        // Background circle
        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        
        // Progress arc
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = 360f * animatedProgress.value,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * AnimatedDonutChart - Animated donut chart for displaying proportions
 *
 * @param segments List of segments with values and colors
 * @param centerContent Optional composable for the center of the donut
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun AnimatedDonutChart(
    segments: List<ProgressSegment>,
    centerContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Ensure segments sum to 1.0
    val totalValue = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
    val normalizedSegments = segments.map { it.copy(value = it.value / totalValue) }
    
    // Animate each segment
    val animatedSegments = normalizedSegments.map { segment ->
        val animatedValue = animateFloatAsState(
            targetValue = segment.value,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "DonutSegmentAnimation"
        )
        segment.copy(value = animatedValue.value)
    }
    
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "Donut chart with ${segments.size} segments"
                }
        ) {
            val strokeWidth = size.minDimension * 0.2f
            val radius = (size.minDimension - strokeWidth) / 2
            
            var startAngle = 0f
            
            animatedSegments.forEach { segment ->
                val sweepAngle = segment.value * 360f
                
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Center content
        if (centerContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                centerContent()
            }
        }
    }
}

//endregion

//region Card Layouts

/**
 * InfoCard - Card for displaying information with icon
 *
 * @param title Card title
 * @param content Card content
 * @param icon Icon to display
 * @param iconTint Icon tint color
 * @param onClick Optional click handler
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun InfoCard(
    title: String,
    content: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Arrow if clickable
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * StatusCard - Card for displaying status information with color coding
 *
 * @param title Card title
 * @param status Status text
 * @param statusColor Color representing the status
 * @param icon Optional icon to display
 * @param onClick Optional click handler
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun StatusCard(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon if provided
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Status chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    contentColor = statusColor,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Arrow if clickable
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * MetricOverviewCard - Card for displaying health metric overview
 *
 * @param title Metric title
 * @param value Current value
 * @param unit Unit of measurement
 * @param trend Optional trend direction
 * @param icon Icon to display
 * @param color Primary color for the metric
 * @param onClick Optional click handler
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun MetricOverviewCard(
    title: String,
    value: String,
    unit: String,
    trend: TrendDirection? = null,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Value with unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(start = 4.dp, bottom = 4.dp)
                            .alignByBaseline()
                    )
                }
            }
            
            // Trend indicator if available
            if (trend != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (trend) {
                            TrendDirection.UP -> Icons.Default.TrendingUp
                            TrendDirection.DOWN -> Icons.Default.TrendingDown
                            TrendDirection.STABLE -> Icons.Default.TrendingFlat
                        },
                        contentDescription = when (trend) {
                            TrendDirection.UP -> "Trending up"
                            TrendDirection.DOWN -> "Trending down"
                            TrendDirection.STABLE -> "Stable"
                        },
                        tint = when (trend) {
                            TrendDirection.UP -> MaterialTheme.colorScheme.error
                            TrendDirection.DOWN -> MaterialTheme.colorScheme.tertiary
                            TrendDirection.STABLE -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = when (trend) {
                            TrendDirection.UP -> "Increasing"
                            TrendDirection.DOWN -> "Decreasing"
                            TrendDirection.STABLE -> "Stable"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * ActionCard - Card with prominent action button
 *
 * @param title Card title
 * @param description Card description
 * @param actionText Text for the action button
 * @param onAction Callback for the action button
 * @param icon Optional icon to display
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ActionCard(
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with icon if provided
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action button
            Button(
                onClick = onAction,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionText)
            }
        }
    }
}

//endregion

//region Action Components

/**
 * ActionButton - Primary button with icon and text
 *
 * @param text Button text
 * @param onClick Click handler
 * @param icon Optional icon to display
 * @param enabled Whether the button is enabled
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(text = text)
    }
}

/**
 * SecondaryActionButton - Secondary button with icon and text
 *
 * @param text Button text
 * @param onClick Click handler
 * @param icon Optional icon to display
 * @param enabled Whether the button is enabled
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(text = text)
    }
}

/**
 * IconActionButton - Button with just an icon and tooltip
 *
 * @param icon Icon to display
 * @param contentDescription Content description for accessibility
 * @param onClick Click handler
 * @param enabled Whether the button is enabled
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}

/**
 * FloatingActionButtonWithTooltip - FAB with tooltip
 *
 * @param icon Icon to display
 * @param contentDescription Content description for accessibility
 * @param onClick Click handler
 * @param expanded Whether to show the extended FAB
 * @param text Text for extended FAB
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun FloatingActionButtonWithTooltip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    expanded: Boolean = false,
    text: String? = null,
    modifier: Modifier = Modifier
) {
    if (expanded && text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            text = {
                Text(text = text)
            },
            modifier = modifier.semantics {
                this.contentDescription = contentDescription
            }
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.semantics {
                this.contentDescription = contentDescription
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

/**
 * ChipGroup - Group of selectable chips
 *
 * @param items List of items to display as chips
 * @param selectedItem Currently selected item
 * @param onItemSelected Callback when an item is selected
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun <T> ChipGroup(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val selected = item == selectedItem
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    }
                ),
                modifier = Modifier
                    .height(32.dp)
                    .clickable { onItemSelected(item) }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    itemContent(item, selected)
                }
            }
        }
    }
}

//endregion

//region Status Indicators

/**
 * StatusBadge - Small badge indicating status
 *
 * @param status Status text
 * @param color Color representing the status
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun StatusBadge(
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * ConnectionStatusIndicator - Indicator for connection status
 *
 * @param connected Whether the device is connected
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ConnectionStatusIndicator(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (connected) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Status text
        Text(
            text = if (connected) "Connected" else "Disconnected",
            style = MaterialTheme.typography.labelSmall,
            color = if (connected) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

/**
 * BatteryLevelIndicator - Battery level indicator with icon
 *
 * @param level Battery level percentage (0-100)
 * @param charging Whether the device is charging
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun BatteryLevelIndicator(
    level: Int,
    charging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        level <= 15 -> MaterialTheme.colorScheme.error
        level <= 30 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        level <= 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery icon
        Icon(
            imageVector = when {
                charging -> Icons.Default.BatteryCharging
                level <= 15 -> Icons.Default.BatteryAlert
                level <= 30 -> Icons.Default.Battery2Bar
                level <= 50 -> Icons.Default.Battery4Bar
                level <= 80 -> Icons.Default.Battery6Bar
                else -> Icons.Default.BatteryFull
            },
            contentDescription = "Battery level $level%",
            tint = batteryColor,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Battery level text
        Text(
            text = "$level%",
            style = MaterialTheme.typography.labelSmall,
            color = batteryColor
        )
        
        // Charging indicator
        if (charging) {
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = "Charging",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/**
 * HealthStatusBadge - Badge for health status indicators
 *
 * @param status Health status
 * @param showLabel Whether to show a text label
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun HealthStatusBadge(
    status: HealthStatus,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        HealthStatus.EXCELLENT -> MaterialTheme.colorScheme.tertiary
        HealthStatus.GOOD -> MaterialTheme.colorScheme.primary
        HealthStatus.FAIR -> MaterialTheme.colorScheme.secondary
        HealthStatus.POOR -> MaterialTheme.colorScheme.error
    }
    
    val statusText = when (status) {
        HealthStatus.EXCELLENT -> "Excellent"
        HealthStatus.GOOD -> "Good"
        HealthStatus.FAIR -> "Fair"
        HealthStatus.POOR -> "Poor"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        
        // Status label
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
        }
    }
}

/**
 * AlertSeverityIndicator - Visual indicator for alert severity
 *
 * @param severity Alert severity level
 * @param showLabel Whether to show a text label
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun AlertSeverityIndicator(
    severity: AlertSeverity,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val severityColor = when (severity) {
        AlertSeverity.LOW -> MaterialTheme.colorScheme.primary
        AlertSeverity.MEDIUM -> MaterialTheme.colorScheme.secondary
        AlertSeverity.HIGH -> MaterialTheme.colorScheme.error
        AlertSeverity.EMERGENCY -> MaterialTheme.colorScheme.error
    }
    
    val severityText = when (severity) {
        AlertSeverity.LOW -> "Low"
        AlertSeverity.MEDIUM -> "Medium"
        AlertSeverity.HIGH -> "High"
        AlertSeverity.EMERGENCY -> "Emergency"
    }
    
    val severityIcon = when (severity) {
        AlertSeverity.LOW -> Icons.Outlined.Info
        AlertSeverity.MEDIUM -> Icons.Outlined.Warning
        AlertSeverity.HIGH -> Icons.Filled.Warning
        AlertSeverity.EMERGENCY -> Icons.Filled.Error
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Severity icon
        Icon(
            imageVector = severityIcon,
            contentDescription = "$severityText severity",
            tint = severityColor,
            modifier = Modifier.size(16.dp)
        )
        
        // Severity label
        if (showLabel) {
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = severityText,
                style = MaterialTheme.typography.labelSmall,
                color = severityColor
            )
        }
    }
}

//endregion

//region Miscellaneous Components

/**
 * DateTimeDisplay - Formatted date and time display
 *
 * @param dateTime LocalDateTime to display
 * @param showDate Whether to show the date
 * @param showTime Whether to show the time
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun DateTimeDisplay(
    dateTime: LocalDateTime,
    showDate: Boolean = true,
    showTime: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = buildString {
                if (showDate) {
                    append(dateTime.format(dateFormatter))
                }
                
                if (showDate && showTime) {
                    append(" at ")
                }
                
                if (showTime) {
                    append(dateTime.format(timeFormatter))
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

/**
 * ExpandableContent - Content that can be expanded/collapsed
 *
 * @param expanded Whether the content is expanded
 * @param onExpandChanged Callback when expansion state changes
 * @param header Header content
 * @param content Expandable content
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun ExpandableContent(
    expanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Header with expand/collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandChanged(!expanded) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                header()
            }
            
            IconButton(
                onClick = { onExpandChanged(!expanded) },
                modifier = Modifier.semantics {
                    this.contentDescription = if (expanded) "Collapse" else "Expand"
                    this.expanded = expanded
                }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Expandable content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            content()
        }
    }
}

/**
 * DividerWithLabel - Divider with optional label
 *
 * @param label Optional label to display
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun DividerWithLabel(
    label: String? = null,
    modifier: Modifier = Modifier
) {
    if (label != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Divider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    } else {
        Divider(
            modifier = modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

/**
 * NoRippleClickable - Clickable modifier without ripple effect
 *
 * @param onClick Click handler
 */
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        onClick()
    }
}

//endregion
