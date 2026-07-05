package com.detoxapp.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.detoxapp.data.ScheduleRepository
import com.detoxapp.data.StatsRepository

/**
 * Watches Instagram and YouTube's on-screen content for signs that the user
 * has landed on Reels / Shorts, and immediately presses "back" to bounce
 * them out. Chat, DMs, feed, and regular video playback are left untouched.
 *
 * NOTE ON FRAGILITY: Instagram and YouTube do not expose a public "which
 * screen am I on" API. This works by scanning the accessibility node tree
 * for resource-ids / text / content-descriptions that (as of writing) show
 * up only on the Reels/Shorts screens. Instagram and YouTube update their
 * apps often and MAY change these identifiers, which can silently break
 * detection. If blocking stops working after an app update:
 *   1. Enable Android's "Layout Inspector" (Android Studio) or a tool like
 *      "Accessibility Scanner" while the Reels/Shorts screen is open.
 *   2. Find the new resource-id / description used by that screen.
 *   3. Add it to INSTAGRAM_REELS_MARKERS / YOUTUBE_SHORTS_MARKERS below.
 */
class ReelsShortsBlockerService : AccessibilityService() {

    private lateinit var statsRepository: StatsRepository
    private lateinit var scheduleRepository: ScheduleRepository

    // package -> last time (elapsedRealtime ms) we triggered a back press,
    // so we don't spam GLOBAL_ACTION_BACK on every content-changed event.
    private val lastBlockTimeMs = HashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        statsRepository = StatsRepository(this)
        scheduleRepository = ScheduleRepository(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Detox accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        val app = when (packageName) {
            PACKAGE_INSTAGRAM -> StatsRepository.App.INSTAGRAM
            PACKAGE_YOUTUBE -> StatsRepository.App.YOUTUBE
            else -> return
        }

        if (!scheduleRepository.isBlockingActiveNow()) return
        if (isInCooldown(packageName)) return

        val root = rootInActiveWindow ?: return
        val markers = if (app == StatsRepository.App.INSTAGRAM)
            INSTAGRAM_REELS_MARKERS else YOUTUBE_SHORTS_MARKERS

        val found = try {
            nodeTreeContainsMarker(root, markers)
        } finally {
            root.recycle()
        }

        if (found) {
            Log.i(TAG, "Blocking $packageName - Reels/Shorts detected")
            performGlobalAction(GLOBAL_ACTION_BACK)
            statsRepository.recordBlock(app)
            lastBlockTimeMs[packageName] = SystemClock.elapsedRealtime()
        }
    }

    private fun isInCooldown(packageName: String): Boolean {
        val last = lastBlockTimeMs[packageName] ?: return false
        return SystemClock.elapsedRealtime() - last < COOLDOWN_MS
    }

    /**
     * Breadth-first search over the visible node tree, bounded by
     * MAX_NODES_TO_SCAN so a deep/complex screen can't cause jank or ANRs.
     */
    private fun nodeTreeContainsMarker(root: AccessibilityNodeInfo, markers: List<String>): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES_TO_SCAN) {
            val node = queue.removeFirst()
            visited++

            val viewId = node.viewIdResourceName ?: ""
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""

            for (marker in markers) {
                if (viewId.contains(marker, ignoreCase = true) ||
                    desc.contains(marker, ignoreCase = true) ||
                    text.contains(marker, ignoreCase = true)
                ) {
                    return true
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "Detox accessibility service interrupted")
    }

    companion object {
        private const val TAG = "ReelsShortsBlocker"

        private const val PACKAGE_INSTAGRAM = "com.instagram.android"
        private const val PACKAGE_YOUTUBE = "com.google.android.youtube"

        private const val COOLDOWN_MS = 1200L
        private const val MAX_NODES_TO_SCAN = 400

        // Known-good identifiers as of early 2026. See the class-level note
        // above for how to refresh these if Instagram/YouTube change them.
        private val INSTAGRAM_REELS_MARKERS = listOf(
            "clips_viewer_view_pager",
            "clips_swipe_refresh_layout",
            "clips_tab",
            "reel_viewer",
            "clips_viewer_media_container"
        )

        private val YOUTUBE_SHORTS_MARKERS = listOf(
            "reel_recycler",
            "reel_player_page_container",
            "shorts_player",
            "reel_dyn_length_pivot_button"
        )
    }
}
