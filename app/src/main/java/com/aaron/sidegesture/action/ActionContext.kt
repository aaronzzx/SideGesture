package com.aaron.sidegesture.action

import androidx.compose.ui.geometry.Offset
import com.aaron.sidegesture.entity.Action
import com.aaron.sidegesture.entity.GestureButton

/**
 * ActionRequest 可能需要的上下文
 *
 * @param anchor overlay 弹出时可能需要根据锚点显示位置
 *
 * @author DS-Z
 * @since 2026/7/11
 */
data class ActionContext(
    val anchor: Offset?,
    val button: GestureButton?
)
