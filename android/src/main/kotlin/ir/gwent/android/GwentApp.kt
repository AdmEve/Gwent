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
    }

    companion object {
        private const val PREFS = "gwent-diagnostics"
        private const val KEY_LAST_CRASH = "last_crash"

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun lastCrash(context: Context): String? = prefs(context).getString(KEY_LAST_CRASH, null)

        fun clearLastCrash(context: Context) {
            prefs(context).edit().remove(KEY_LAST_CRASH).apply()
        }
    }
}
