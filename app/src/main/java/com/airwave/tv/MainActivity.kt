package com.airwave.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.airwave.tv.ui.AirwaveTheme
import com.airwave.tv.ui.AppRoot
import com.airwave.tv.ui.AppViewModel
import com.airwave.tv.ui.FIRST

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirwaveTheme {
                AppRoot(vm)
            }
        }
    }

    // If nothing holds focus (e.g. the window started in touch mode), the first D-pad press
    // puts focus back on the last focused item instead of being lost.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in navKeys && !vm.uiHasFocus) {
            leaveTouchMode()
            vm.focusTarget = vm.lastFocused ?: FIRST
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { leaveTouchMode() }
    }

    /** Remote-driven UI: Compose won't focus clickable items while the window is in touch mode. */
    private fun leaveTouchMode() {
        val content = findViewById<ViewGroup>(android.R.id.content)
        val composeView = content?.getChildAt(0) ?: return
        if (composeView.isInTouchMode) composeView.requestFocusFromTouch()
    }

    private companion object {
        val navKeys = setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
        )
    }
}
