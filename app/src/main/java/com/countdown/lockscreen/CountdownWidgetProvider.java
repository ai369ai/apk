package com.countdown.lockscreen;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CountdownWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.countdown_widget);

        // Calculate days to next New Year
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(now.get(Calendar.YEAR) + 1, Calendar.JANUARY, 1, 0, 0, 0);

        long diffMillis = target.getTimeInMillis() - now.getTimeInMillis();
        long daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis);

        String countdownText = "⏳ " + daysRemaining + " Days to " + target.get(Calendar.YEAR) + " • Tap to Interact";
        views.setTextViewText(R.id.widget_countdown_text, countdownText);

        // Clicking the widget launches the full 3D Countdown Lock Screen
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
