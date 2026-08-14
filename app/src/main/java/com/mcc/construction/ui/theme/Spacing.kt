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
 * [minTouch] is 56dp rather than the usual Material 48dp: CLAUDE.md notes workers
 * may be wearing gloves, so primary controls get a larger target.
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

    /** Minimum height for a primary, gloved-hand touch target. */
    val minTouch = 56.dp

    /** Standard height for a list row. */
    val rowHeight = 72.dp
}
