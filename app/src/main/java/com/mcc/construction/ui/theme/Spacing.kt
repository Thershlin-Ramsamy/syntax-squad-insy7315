package com.mcc.construction.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing rhythm.
 *
 * Everything sits on a 4dp grid. Screens use exactly three vertical intervals —
 * [itemGap] between sibling items, [blockGap] between a heading and its content,
 * and [sectionGap] between unrelated sections. Restricting the vocabulary to
 * three intervals is what makes independently-written screens feel like one app.
 *
 * Touch targets come in two sizes because workers may be wearing gloves:
 * [touchTarget] (48dp) is the floor for anything tappable, and [minTouch]
 * (56dp) is used for primary actions such as sign in, clock in and submit.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Horizontal page gutter. */
    val screenH = 20.dp

    /** Padding inside a card or list row. */
    val cardPad = 16.dp

    /** Between sibling cards / list rows. */
    val itemGap = 12.dp

    /** Between a section heading and the content beneath it. */
    val blockGap = 12.dp

    /** Between two unrelated sections. */
    val sectionGap = 28.dp

    /** Smallest size for anything tappable — icon buttons, rows, chips. */
    val touchTarget = 48.dp

    /** Minimum height for a primary, gloved-hand touch target. */
    val minTouch = 56.dp

    /** Standard height for a list row. */
    val rowHeight = 72.dp

    /**
     * Widest a column of content grows on a desktop-width window. Past this,
     * lines get too long to scan, so content centres instead of stretching.
     */
    val contentMaxWidth = 840.dp

    /** Widest a single-column form (sign in, reset password) grows. */
    val formMaxWidth = 480.dp
}
