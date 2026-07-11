package com.aaron.sidegesture.action

/**
 * Action 的处理封装
 *
 * @author DS-Z
 * @since 2026/7/11
 */
interface ActionHandler {

    val supportedActions: Set<String>

    suspend fun handle(request: ActionRequest)
}