val handler = Handler(Looper.getMainLooper())
var longPressRunnable: Runnable? = null
var isLongPressTriggered = false

// ...
        // 6. Long-Press Key Mapping
        val longPressKey = ConfigManager.getLongPressKey(this)
        if (longPressKey > 0 && keyCode == longPressKey) {
            if (action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                isLongPressTriggered = false
                longPressRunnable = Runnable {
                    isLongPressTriggered = true
                    triggerSwitcher(isNext = true)
                }
                mainHandler.postDelayed(longPressRunnable!!, 500L) // 500ms long press
                return super.onKeyEvent(event) // Pass DOWN to system
            } else if (action == KeyEvent.ACTION_UP) {
                longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                longPressRunnable = null
                
                if (isLongPressTriggered) {
                    isLongPressTriggered = false
                    return true // Consume UP, so short-click doesn't happen
                } else {
                    return super.onKeyEvent(event) // Short press, pass UP to system
                }
            } else if (action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) {
                // If the remote does send repeat events, we consume them so the system doesn't get multiple downs
                if (isLongPressTriggered) {
                    return true
                } else {
                    return super.onKeyEvent(event)
                }
            }
        }
