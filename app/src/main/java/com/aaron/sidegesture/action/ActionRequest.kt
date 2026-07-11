package com.aaron.sidegesture.action

import com.aaron.sidegesture.entity.Action

/**
 * 通过 ActionRequest 触达对应 ActionHandler
 *
 * @author DS-Z
 * @since 2026/7/11
 */
data class ActionRequest(
    val action: Action,
    val actionContext: ActionContext? = null
)
