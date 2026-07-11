package com.aaron.sidegesture.action

class ActionDispatcher(
    handlers: Collection<ActionHandler>
) {

    private val handlerMap = buildMap {
        handlers.forEach { handler ->
            require(handler.supportedActions.isNotEmpty()) {
                "${handler.javaClass.simpleName} supportedActions is empty"
            }
            handler.supportedActions.forEach { action ->
                require(put(action, handler) == null) {
                    "Duplicate ActionHandler for action: $action"
                }
            }
        }
    }

    suspend fun dispatch(request: ActionRequest): Boolean {
        val handler = handlerMap[request.action.value] ?: return false
        handler.handle(request)
        return true
    }
}
