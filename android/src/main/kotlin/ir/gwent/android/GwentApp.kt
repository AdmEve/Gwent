package ir.gwent.android

import android.app.Application
import android.content.Context

/**
 * Stores the stack trace of an uncaught exception so the next launch can show it. Without a
 * device in the loop a crash is otherwise invisible — the app just disappears.
 */
class GwentApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                prefs(this).edit()
                    .putString(KEY_LAST_CRASH, error.stackTraceToString())
                    .commit() // commit, not apply: the process is about to die
            }
            previous?.uncaughtException(thread, error)
        }
        note(this, "--- app started ---")
    }

    companion object {
        private const val PREFS = "gwent-diagnostics"
        private const val KEY_LAST_CRASH = "last_crash"
        private const val KEY_TRAIL = "trail"
        private const val TRAIL_LIMIT = 25

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun lastCrash(context: Context): String? = prefs(context).getString(KEY_LAST_CRASH, null)

        fun clearLastCrash(context: Context) {
            prefs(context).edit().remove(KEY_LAST_CRASH).apply()
        }

        /**
         * Records what the game just did. A crash report is far more useful with the last few
         * moves attached than with a stack trace alone, and a freeze leaves no trace at all
         * without it — the trail survives the process being killed.
         */
        fun note(context: Context, line: String) {
            runCatching {
                val existing = prefs(context).getString(KEY_TRAIL, "").orEmpty()
                val kept = (existing.lines() + line).filter { it.isNotBlank() }.takeLast(TRAIL_LIMIT)
                prefs(context).edit().putString(KEY_TRAIL, kept.joinToString("\n")).commit()
            }
        }

        fun trail(context: Context): String = prefs(context).getString(KEY_TRAIL, "").orEmpty()

        /** Stores a caught failure that did not kill the process, so it still gets reported. */
        fun recordHandled(context: Context, error: Throwable) {
            runCatching {
                prefs(context).edit()
                    .putString(KEY_LAST_CRASH, "Caught (the app kept running):\n" + error.stackTraceToString())
                    .commit()
            }
        }
    }
}
