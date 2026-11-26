package com.vince.frostos;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class FrostNotificationListener extends NotificationListenerService {
    private WindowManager windowManager;
    private View islandView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideIsland = () -> removeIsland();

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        if (!Settings.canDrawOverlays(this)) return;
        Notification n = sbn.getNotification();
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (title == null && body == null) return;
        String app = sbn.getPackageName();
        String heading = title == null ? app : title.toString();
        String detail = body == null ? "New notification" : body.toString();
        PendingIntent intent = n.contentIntent;
        handler.post(() -> showIsland(heading, detail, intent));
    }

    private void showIsland(String title, String detail, PendingIntent intent) {
        try {
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            removeIsland();
            TextView pill = new TextView(this);
            pill.setText(title + "\n" + detail);
            pill.setTextColor(Color.WHITE);
            pill.setTextSize(12);
            pill.setTypeface(Typeface.DEFAULT, Typeface.MEDIUM);
            pill.setMaxLines(2);
            pill.setPadding(24, 15, 24, 15);
            GradientDrawable shape = new GradientDrawable();
            shape.setColor(Color.rgb(5, 10, 20));
            shape.setCornerRadius(48);
            shape.setStroke(2, 0xFF4A9FE8);
            pill.setBackground(shape);
            pill.setGravity(Gravity.CENTER);
            pill.setOnClickListener(v -> {
                try { if (intent != null) intent.send(); } catch (PendingIntent.CanceledException ignored) {}
                removeIsland();
            });
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = 28;
            windowManager.addView(pill, params);
            islandView = pill;
            handler.removeCallbacks(hideIsland);
            handler.postDelayed(hideIsland, 5500);
        } catch (Exception ignored) {
            removeIsland();
        }
    }

    private void removeIsland() {
        handler.removeCallbacks(hideIsland);
        if (windowManager != null && islandView != null) {
            try { windowManager.removeView(islandView); } catch (Exception ignored) {}
        }
        islandView = null;
    }

    @Override public void onDestroy() {
        removeIsland();
        super.onDestroy();
    }
}
