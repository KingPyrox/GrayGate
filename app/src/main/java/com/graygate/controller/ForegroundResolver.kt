package com.graygate.controller

/**
 * Pure logic for turning a batch of UsageStats events into a foreground-app
 * report. Kept free of Android dependencies so it can be unit tested on the
 * JVM, like [GrayscaleRules].
 *
 * Why this exists: closing an app (swiping it away in recents, or backing out)
 * often produces NO ACTIVITY_RESUMED for the launcher — the launcher was
 * already resumed underneath the recents overlay. A detector that only reacts
 * to RESUMED events therefore never notices the close, and the grayscale
 * state is stranded on an app that is no longer on screen. The ACTIVITY_STOPPED
 * event of the closed app is the reliable signal for that case.
 */
object ForegroundResolver {

    /**
     * Decide which package to report as foreground, given the newest
     * ACTIVITY_RESUMED and ACTIVITY_STOPPED events observed since the last poll.
     *
     * @param resumedPackage package of the newest ACTIVITY_RESUMED event, or null
     * @param resumedTime    timestamp of that event (0 when none)
     * @param stoppedPackage package of the newest ACTIVITY_STOPPED event, or null
     * @param stoppedTime    timestamp of that event (0 when none)
     * @param stateOwnerPackage the app that last decided the grayscale state
     * @param selfPackageName   GrayGate's own package
     * @param homePackage       the default launcher, or null when unresolvable
     * @return package to report to the controller, or null when the current
     *         state should be left alone
     */
    fun resolve(
        resumedPackage: String?,
        resumedTime: Long,
        stoppedPackage: String?,
        stoppedTime: Long,
        stateOwnerPackage: String,
        selfPackageName: String,
        homePackage: String?
    ): String? {
        val hasRealResume = resumedPackage != null &&
                !GrayscaleRules.isIgnored(resumedPackage, selfPackageName)

        // Normal case: something real came to the foreground. Trust it even when
        // a stop event carries a LATER timestamp — Android resumes the next
        // activity before stopping the previous one, so "RESUMED(camera),
        // STOPPED(instagram)" lands in one batch with the stop last. The only
        // exception is the same package flash-opening and closing again
        // (resume then a newer stop of that very package).
        if (hasRealResume && (resumedPackage != stoppedPackage || resumedTime >= stoppedTime)) {
            return resumedPackage
        }

        // Close case: the app that owns the grayscale state stopped, and nothing
        // real resumed in its place. It was swiped away or backed out of, and
        // we're on the home screen — which produced no RESUMED of its own.
        if (stoppedPackage != null && stoppedPackage == stateOwnerPackage) {
            return homePackage
        }

        // Only ignored packages (or nothing) resumed — leave the state alone.
        return null
    }
}
