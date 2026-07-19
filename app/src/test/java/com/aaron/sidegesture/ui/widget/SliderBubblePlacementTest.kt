package com.aaron.sidegesture.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class SliderBubblePlacementTest {

    @Test
    fun centeredThumbCentersBubbleAndPointer() {
        val placement = calculateSliderBubbleHorizontalPlacement(
            containerWidthPx = 200,
            thumbCenterPx = 100,
            bubbleWidthPx = 60
        )

        assertEquals(70, placement.bodyLeftPx)
        assertEquals(30, placement.pointerCenterInBodyPx)
        assertEquals(100, placement.bodyLeftPx + placement.pointerCenterInBodyPx)
    }

    @Test
    fun edgeThumbsClampBodyWithoutMovingPointerTip() {
        val leftPlacement = calculateSliderBubbleHorizontalPlacement(
            containerWidthPx = 200,
            thumbCenterPx = 10,
            bubbleWidthPx = 60
        )
        val rightPlacement = calculateSliderBubbleHorizontalPlacement(
            containerWidthPx = 200,
            thumbCenterPx = 190,
            bubbleWidthPx = 60
        )

        assertEquals(0, leftPlacement.bodyLeftPx)
        assertEquals(10, leftPlacement.pointerCenterInBodyPx)
        assertEquals(140, rightPlacement.bodyLeftPx)
        assertEquals(50, rightPlacement.pointerCenterInBodyPx)
        assertEquals(10, leftPlacement.bodyLeftPx + leftPlacement.pointerCenterInBodyPx)
        assertEquals(190, rightPlacement.bodyLeftPx + rightPlacement.pointerCenterInBodyPx)
    }

    @Test
    fun edgePointerBaseStaysOnFlatRoundedEdgeWithoutMovingTip() {
        val leftPointer = calculateSliderBubblePointerHorizontalPlacement(
            bodyWidthPx = 60,
            pointerCenterInBodyPx = 10,
            pointerWidthPx = 12,
            cornerInsetPx = 8
        )
        val rightPointer = calculateSliderBubblePointerHorizontalPlacement(
            bodyWidthPx = 60,
            pointerCenterInBodyPx = 50,
            pointerWidthPx = 12,
            cornerInsetPx = 8
        )

        assertEquals(8, leftPointer.baseStartInBodyPx)
        assertEquals(10, leftPointer.tipInBodyPx)
        assertEquals(16, leftPointer.baseEndInBodyPx)
        assertEquals(44, rightPointer.baseStartInBodyPx)
        assertEquals(50, rightPointer.tipInBodyPx)
        assertEquals(52, rightPointer.baseEndInBodyPx)
    }

    @Test
    fun shortPointerLeavesSpaceBeforeThumbAndKeepsBodyClearance() {
        val placement = calculateSliderBubbleVerticalPlacement(
            pointerHeightPx = 8,
            pointerOverlapPx = 1,
            bodyToThumbClearancePx = 40
        )

        assertEquals(7, placement.visiblePointerHeightPx)
        assertEquals(33, placement.pointerToThumbGapPx)
        assertEquals(
            40,
            placement.visiblePointerHeightPx + placement.pointerToThumbGapPx
        )
    }

    @Test
    fun oversizedBubbleUsesContainerWidthAndKeepsPointerAligned() {
        val placement = calculateSliderBubbleHorizontalPlacement(
            containerWidthPx = 80,
            thumbCenterPx = 65,
            bubbleWidthPx = 120
        )

        assertEquals(0, placement.bodyLeftPx)
        assertEquals(65, placement.pointerCenterInBodyPx)
    }

    @Test
    fun thumbCenterUsesTrackWidthAndMirrorsInRtl() {
        assertEquals(10, calculateSliderThumbCenterPx(0f, 0f..1f, 200, 20, false))
        assertEquals(100, calculateSliderThumbCenterPx(0.5f, 0f..1f, 200, 20, false))
        assertEquals(190, calculateSliderThumbCenterPx(1f, 0f..1f, 200, 20, false))
        assertEquals(190, calculateSliderThumbCenterPx(0f, 0f..1f, 200, 20, true))
        assertEquals(10, calculateSliderThumbCenterPx(1f, 0f..1f, 200, 20, true))
    }
}
