package com.studyos.app.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.studyos.app.theme.StudyOSTheme

/**
 * Micro-interaction modifier that subtly scales down when pressed (bounce click feedback).
 */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.97f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "bounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

/**
 * Haptic feedback helper for subtle, non-intrusive tactile responses.
 */
class StudyOSHapticHelper(private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    fun performCompletion() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performConfirmation() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performToggle() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberStudyOSHaptics(): StudyOSHapticHelper {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { StudyOSHapticHelper(haptic) }
}

/**
 * Base glass surface with subtle translucency, 1dp low-opacity border, and soft elevation.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = StudyOSTheme.shapes.medium,
    backgroundColor: Color = StudyOSTheme.colors.glassSurface,
    borderColor: Color = StudyOSTheme.colors.glassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Reusable GlassCard for lists, items, and focused content.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = StudyOSTheme.shapes.medium,
    backgroundColor: Color = StudyOSTheme.colors.glassSurface,
    borderColor: Color = StudyOSTheme.colors.glassBorder,
    padding: Dp = 16.dp,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .bounceClick(scaleDown = 0.98f, onClick = onClick)
            .semantics { role = Role.Button }
    } else {
        modifier
    }

    val innerPaddingModifier = if (contentPadding != null) {
        Modifier.padding(contentPadding)
    } else {
        Modifier.padding(padding)
    }

    GlassSurface(
        modifier = cardModifier,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(innerPaddingModifier),
            content = content
        )
    }
}

/**
 * Compact modern Top App Bar with glass styling.
 * Structure: [NavigationIcon] Screen Title [Actions]
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        backgroundColor = colors.background.copy(alpha = 0.92f),
        borderColor = colors.border.copy(alpha = 0.5f),
        borderWidth = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.subsectionTitle.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText,
                    maxLines = 1
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = typography.caption,
                        color = colors.secondaryText,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                content = actions
            )
        }
    }
}

/**
 * Reusable GlassIconButton with minimum 44dp touch target and bounce micro-interaction.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    backgroundColor: Color = StudyOSTheme.colors.glassSurfaceSubtle,
    tint: Color = StudyOSTheme.colors.primaryText,
    icon: @Composable () -> Unit
) {
    val colors = StudyOSTheme.colors
    val effectiveBg = if (enabled) backgroundColor else colors.border.copy(alpha = 0.2f)
    val effectiveBorder = if (enabled) colors.glassBorder else Color.Transparent

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .bounceClick(scaleDown = 0.94f) {
                if (enabled) onClick()
            }
            .clip(shape)
            .background(effectiveBg)
            .border(1.dp, effectiveBorder, shape)
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * Reusable GlassChip for tag filtering or status indicators.
 */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) colors.primaryText else colors.glassSurfaceSubtle,
        animationSpec = tween(durationMillis = 200),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.background else colors.primaryText,
        animationSpec = tween(durationMillis = 200),
        label = "chipContent"
    )

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .clip(shapes.pill)
            .background(backgroundColor)
            .border(1.dp, if (selected) Color.Transparent else colors.glassBorder, shapes.pill)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                this.role = Role.Tab
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = typography.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = contentColor
        )
    }
}

/**
 * Floating glass bottom navigation bar with 4 primary destinations:
 * Home, Subjects, Progress, Calendar.
 */
data class GlassNavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun GlassBottomBar(
    items: List<GlassNavigationItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.large,
            backgroundColor = colors.glassSurface,
            borderColor = colors.glassBorder,
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primaryText.copy(alpha = 0.08f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "navPillBg"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) colors.accent else colors.secondaryText,
                        animationSpec = tween(durationMillis = 200),
                        label = "navIconTint"
                    )

                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(shapes.pill)
                            .background(pillBgColor)
                            .bounceClick(scaleDown = 0.94f) {
                                onNavigate(item.route)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics {
                                this.role = Role.Tab
                                this.contentDescription = item.title
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                style = typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primaryText
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable GlassDialog with 28dp modal corners and clean confirmation/cancel buttons.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmButtonText: String = "Confirm",
    onConfirm: () -> Unit,
    dismissButtonText: String? = "Cancel",
    onDismiss: (() -> Unit)? = onDismissRequest,
    isDestructive: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = modifier
                    .fillMaxWidth(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // prevent click through
                    ),
                shape = shapes.modal,
                backgroundColor = colors.surface,
                borderColor = colors.glassBorder,
                borderWidth = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = title,
                        style = typography.sectionTitle.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )

                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = typography.body,
                            color = colors.secondaryText
                        )
                    }

                    if (content != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        content()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissButtonText != null && onDismiss != null) {
                            StudyOSOutlinedButton(
                                text = dismissButtonText,
                                onClick = onDismiss
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        StudyOSButton(
                            text = confirmButtonText,
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable GlassBottomSheet with 28dp top corners, drag handle, and glass surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = StudyOSTheme.colors
    val shapes = StudyOSTheme.shapes

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = shapes.bottomSheet,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(shapes.pill)
                    .background(colors.border)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            content = content
        )
    }
}

/**
 * Animated Shimmer Skeleton for elegant loading states matching layout.
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = StudyOSTheme.shapes.small
) {
    val colors = StudyOSTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBaseColor = if (colors.isDark) Color(0xFF222220) else Color(0xFFEBEBE5)
    val shimmerHighlightColor = if (colors.isDark) Color(0xFF2E2E2B) else Color(0xFFF7F7F2)

    val brush = Brush.linearGradient(
        colors = listOf(shimmerBaseColor, shimmerHighlightColor, shimmerBaseColor),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Modal Navigation Drawer for StudyOS.
 * Hamburger menu opens this drawer.
 */
data class DrawerNavigationItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun StudyOSNavigationDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    primaryItems: List<DrawerNavigationItem>,
    secondaryItems: List<DrawerNavigationItem>,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudyOSTheme.colors
    val typography = StudyOSTheme.typography
    val shapes = StudyOSTheme.shapes

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp)
                .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                .background(colors.surface)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drawer Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "StudyOS",
                            style = typography.sectionTitle.copy(fontWeight = FontWeight.Bold),
                            color = colors.primaryText
                        )
                        Text(
                            text = "Personal Study Workspace",
                            style = typography.caption,
                            color = colors.secondaryText
                        )
                    }

                    GlassIconButton(
                        onClick = onClose,
                        contentDescription = "Close Menu"
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = colors.secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                StudyOSDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Primary Navigation Items
                primaryItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val pillBg = if (isSelected) colors.primaryText.copy(alpha = 0.08f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(shapes.small)
                            .background(pillBg)
                            .clickable {
                                onNavigate(item.route)
                                onClose()
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            style = if (isSelected) typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else typography.body,
                            color = if (isSelected) colors.primaryText else colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                StudyOSDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Navigation Items
                secondaryItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val pillBg = if (isSelected) colors.primaryText.copy(alpha = 0.08f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(shapes.small)
                            .background(pillBg)
                            .clickable {
                                onNavigate(item.route)
                                onClose()
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) colors.accent else colors.secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            style = if (isSelected) typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else typography.body,
                            color = if (isSelected) colors.primaryText else colors.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // About studyOS footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .clickable(onClick = onAboutClick)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "About StudyOS • v1.0",
                        style = typography.caption,
                        color = colors.mutedText
                    )
                }
            }
        }
    }
}
