package com.graygate.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundResolverTest {

    private val self = "com.graygate"
    private val home = "com.google.android.apps.nexuslauncher"

    private fun resolve(
        resumed: String? = null,
        resumedTime: Long = 0L,
        stopped: String? = null,
        stoppedTime: Long = 0L,
        owner: String = ""
    ) = ForegroundResolver.resolve(resumed, resumedTime, stopped, stoppedTime, owner, self, home)

    // --- Normal resume path ---

    @Test
    fun `plain resume reports that package`() {
        assertEquals("com.instagram.android", resolve(resumed = "com.instagram.android", resumedTime = 100))
    }

    @Test
    fun `successor resume wins even when the old app's stop is newer`() {
        // Android resumes the next activity BEFORE stopping the previous one,
        // so one batch often contains RESUMED(camera) then STOPPED(instagram)
        // with the stop timestamped later. Camera is the foreground app.
        assertEquals(
            "com.android.camera",
            resolve(
                resumed = "com.android.camera", resumedTime = 100,
                stopped = "com.instagram.android", stoppedTime = 400,
                owner = "com.instagram.android"
            )
        )
    }

    // --- The close case: the bug this resolver exists for ---

    @Test
    fun `stop of the state owner with no successor resolves to the launcher`() {
        // Swiping the gray app away in recents: the launcher was already
        // resumed underneath, so no new RESUMED fires. Previously this
        // stranded the screen in grayscale.
        assertEquals(
            home,
            resolve(stopped = "com.instagram.android", stoppedTime = 100, owner = "com.instagram.android")
        )
    }

    @Test
    fun `stop of the state owner still resolves to launcher when only ignored packages resumed`() {
        assertEquals(
            home,
            resolve(
                resumed = "com.android.systemui", resumedTime = 200,
                stopped = "com.instagram.android", stoppedTime = 100,
                owner = "com.instagram.android"
            )
        )
    }

    @Test
    fun `flash open-and-close of the owner resolves to the launcher`() {
        // Same package resumed then stopped again, stop newer: it's gone.
        assertEquals(
            home,
            resolve(
                resumed = "com.instagram.android", resumedTime = 100,
                stopped = "com.instagram.android", stoppedTime = 300,
                owner = "com.instagram.android"
            )
        )
    }

    @Test
    fun `relaunch of the owner keeps it foreground`() {
        // Stop from the old instance, resume from the new one, resume newer.
        assertEquals(
            "com.instagram.android",
            resolve(
                resumed = "com.instagram.android", resumedTime = 300,
                stopped = "com.instagram.android", stoppedTime = 100,
                owner = "com.instagram.android"
            )
        )
    }

    // --- No-change cases ---

    @Test
    fun `stop of an app that does not own the state changes nothing`() {
        // A background app being cleaned up must not toggle anything.
        assertNull(resolve(stopped = "com.spotify.music", stoppedTime = 100, owner = "com.instagram.android"))
    }

    @Test
    fun `ignored resume alone changes nothing`() {
        assertNull(resolve(resumed = "com.android.systemui", resumedTime = 100, owner = "com.instagram.android"))
    }

    @Test
    fun `graygate itself resuming changes nothing`() {
        assertNull(resolve(resumed = self, resumedTime = 100, owner = "com.instagram.android"))
    }

    @Test
    fun `no events changes nothing`() {
        assertNull(resolve(owner = "com.instagram.android"))
    }

    @Test
    fun `null home degrades to no change instead of crashing`() {
        assertNull(
            ForegroundResolver.resolve(
                null, 0L, "com.instagram.android", 100L,
                "com.instagram.android", self, homePackage = null
            )
        )
    }
}
