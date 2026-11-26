package com.vince.frostos;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * FROST notification bridge.
 * Notification previews are transient; active media sessions remain available
 * as a compact media island while Android reports a playing/paused session.
 */
public class FrostNotificationListener extends NotificationListenerService {
    private WindowManager windowManager;
    private View islandView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hideIsland = this::removeIsland;
    private MediaController mediaController;
    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) {
            handler.post(() -> refreshMediaIsland());
        }
        @Override public void onMetadataChanged(MediaMetadata metadata) {
            handler.post(() -> refreshMediaIsland());
        }
        @Override public void onSessionDestroyed() {
            handler.post(() -> {
                mediaController = null;
                removeIsland();
            });
        }
    };

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        handler.post(this::findActiveMediaSession);
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        Notification n = sbn.getNotification();
        MediaSession.Token token = null;
        try {
            Object raw = n.extras.get(Notification.EXTRA_MEDIA_SESSION);
            if (raw instanceof MediaSession.Token) token = (MediaSession.Token) raw;
        } catch (Exception ignored) {}
        if (token != null) attachMediaSession(token);

        if (!Settings.canDrawOverlays(this)) return;
        if (n.category != null && Notification.CATEGORY_SERVICE.equals(n.category)
                && token != null) return;
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence body = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (title == null && body == null) return;
        String heading = title == null ? sbn.getPackageName() : title.toString();
        String detail = body == null ? "New notification" : body.toString();
        saveNotification(heading, detail, sbn.getPackageName());
        PendingIntent intent = n.contentIntent;
        handler.post(() -> showNotificationIsland(heading, detail, intent));
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        handler.post(this::findActiveMediaSession);
    }

    private void saveNotification(String title, String body, String packageName) {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("frost_notifications", MODE_PRIVATE);
            org.json.JSONArray old = new org.json.JSONArray(prefs.getString("history", "[]"));
            org.json.JSONArray next = new org.json.JSONArray();
            org.json.JSONObject item = new org.json.JSONObject();
            item.put("title", title);
            item.put("body", body);
            item.put("package", packageName);
            item.put("time", System.currentTimeMillis());
            next.put(item);
            for (int i = 0; i < old.length() && next.length() < 12; i++) next.put(old.getJSONObject(i));
            prefs.edit().putString("history", next.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void findActiveMediaSession() {
        if (!Settings.canDrawOverlays(this)) return;
        try {
            for (StatusBarNotification sbn : getActiveNotifications()) {
                Notification n = sbn.getNotification();
                Object raw = n.extras.get(Notification.EXTRA_MEDIA_SESSION);
                if (raw instanceof MediaSession.Token) {
                    attachMediaSession((MediaSession.Token) raw);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private void attachMediaSession(MediaSession.Token token) {
        try {
            if (mediaController != null && mediaController.getSessionToken().equals(token)) {
                refreshMediaIsland();
                return;
            }
            if (mediaController != null) mediaController.unregisterCallback(mediaCallback);
            mediaController = new MediaController(this, token);
            mediaController.registerCallback(mediaCallback, handler);
            handler.post(this::refreshMediaIsland);
        } catch (Exception ignored) {}
    }

    private void refreshMediaIsland() {
        if (!Settings.canDrawOverlays(this) || mediaController == null) return;
        try {
            PlaybackState state = mediaController.getPlaybackState();
            if (state == null || state.getState() == PlaybackState.STATE_NONE
                    || state.getState() == PlaybackState.STATE_STOPPED
                    || state.getState() == PlaybackState.STATE_ERROR) {
                removeIsland();
                return;
            }
            MediaMetadata metadata = mediaController.getMetadata();
            String title = metadata == null ? "Now playing" :
                    value(metadata.getString(MediaMetadata.METADATA_KEY_TITLE), "Now playing");
            String artist = metadata == null ? "" :
                    value(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST), "");
            showMediaIsland(title, artist, state.getState() == PlaybackState.STATE_PLAYING);
        } catch (Exception ignored) {}
    }

    private String value(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private GradientDrawable islandBackground() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(5, 10, 20));
        shape.setCornerRadius(42);
        shape.setStroke(2, 0xFF4A9FE8);
        return shape;
    }

    private WindowManager.LayoutParams layoutParams() {
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
    }

    private LinearLayout islandContainer() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(22, 12, 22, 12);
        panel.setBackground(islandBackground());
        panel.setElevation(16f);
        return panel;
    }

    private TextView islandText(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        view.setMaxLines(1);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void showNotificationIsland(String title, String detail, PendingIntent intent) {
        try {
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            removeIsland();
            LinearLayout panel = islandContainer();
            panel.addView(islandText(title, 13, true));
            panel.addView(islandText(detail, 11, false));
            panel.setOnClickListener(v -> {
                try { if (intent != null) intent.send(); } catch (PendingIntent.CanceledException ignored) {}
                removeIsland();
            });
            addIsland(panel);
            handler.removeCallbacks(hideIsland);
            handler.postDelayed(hideIsland, 5500);
        } catch (Exception ignored) { removeIsland(); }
    }

    private void showMediaIsland(String title, String artist, boolean playing) {
        try {
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            removeIsland();
            LinearLayout panel = islandContainer();
            panel.addView(islandText(title, 13, true));
            if (!artist.isEmpty()) panel.addView(islandText(artist, 10, false));
            LinearLayout controls = new LinearLayout(this);
            controls.setGravity(Gravity.CENTER);
            controls.addView(mediaButton("‹", () -> {
                if (mediaController != null) mediaController.getTransportControls().skipToPrevious();
            }));
            controls.addView(mediaButton(playing ? "Ⅱ" : "▶", () -> {
                if (mediaController == null) return;
                PlaybackState state = mediaController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING)
                    mediaController.getTransportControls().pause();
                else mediaController.getTransportControls().play();
            }));
            controls.addView(mediaButton("›", () -> {
                if (mediaController != null) mediaController.getTransportControls().skipToNext();
            }));
            panel.addView(controls);
            panel.setOnClickListener(v -> {
                try {
                    if (mediaController != null && mediaController.getSessionActivity() != null)
                        mediaController.getSessionActivity().send();
                } catch (Exception ignored) {}
            });
            addIsland(panel);
            handler.removeCallbacks(hideIsland);
        } catch (Exception ignored) { removeIsland(); }
    }

    private Button mediaButton(String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setMinWidth(64);
        button.setMinHeight(42);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void addIsland(View view) {
        WindowManager.LayoutParams params = layoutParams();
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 24;
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
        windowManager.addView(view, params);
        islandView = view;
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
        if (mediaController != null) {
            try { mediaController.unregisterCallback(mediaCallback); } catch (Exception ignored) {}
        }
        mediaController = null;
        super.onDestroy();
    }
}
