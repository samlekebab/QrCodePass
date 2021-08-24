package com.atheris.qrcodepass

import android.content.Intent
import android.net.Uri

class PlayIntent {
    companion object {
        val cheeseWheelIntent:Intent
                get()
        {
            return playIntent("""https://play.google.com/store/apps/details?id=com.atheris.fromaGame&referrer=utm_source%3Din_app%26utm_medium%3Din_app"""")
        }
        val rateThisAppIntent:Intent
        get() {
            return playIntent("""https://play.google.com/store/apps/details?id=com.atheris.qrcodepass&referrer=utm_source%3Din_app%26utm_medium%3Din_app""")
        }

        private fun playIntent(string: String) :Intent =
            Intent(Intent.ACTION_VIEW).also { intent ->
                intent.data =
                    Uri.parse(string)
            }
    }
}