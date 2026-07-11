package com.aaron.sidegesture.action

import kotlinx.coroutines.flow.Flow

/**
 * ActionHandler 能持续产出新的 ActionRequest 的能力
 *
 * @author DS-Z
 * @since 2026/7/11
 */
interface ActionRequestProducer {

    val flow: Flow<ActionRequest>
}