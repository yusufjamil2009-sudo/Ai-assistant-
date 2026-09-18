package com.ustad.personalassistant

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Android12And13SmokeTest {
    @Test
    fun appLaunchesOnAndroid12And13() {
        assertTrue("Smoke test is intended for Android 12/13 emulators", Build.VERSION.SDK_INT in 31..33)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.packageName == "com.ustad.personalassistant")
            }
        }
    }
}