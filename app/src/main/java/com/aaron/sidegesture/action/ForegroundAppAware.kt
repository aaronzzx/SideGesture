package com.aaron.sidegesture.action

/**
 * @author DS-Z
 * @since 2026/7/11
 */
interface ForegroundAppAware {
    
    fun onChange(snapshot: Snapshot)
    
    data class Snapshot(
        val packageName: String?,
        val className: String?
    )
}