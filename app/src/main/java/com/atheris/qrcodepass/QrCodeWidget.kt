package com.atheris.qrcodepass

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.atheris.qrcodepass.qrcode.logd


class QrCodeWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        var qr= QR(context)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val views = RemoteViews(context.packageName, R.layout.qr_code_widget).apply {
            setOnClickPendingIntent(R.id.widgetQrImage, pendingIntent)
            this.setBitmap(R.id.widgetQrImage, "setImageBitmap", qr.getQr())
        }
        for (id in appWidgetIds){
            appWidgetManager.updateAppWidget(id, views)
        }

    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
    }
}