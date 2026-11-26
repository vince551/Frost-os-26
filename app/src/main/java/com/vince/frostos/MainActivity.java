package com.vince.frostos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private LinearLayout root;
    private float downX, downY;
    private boolean lockPreview = false;
    private boolean torchOn = false;
    private String torchCameraId;
    private final int bg = Color.rgb(7, 19, 38);
    private final int blue = Color.rgb(113, 194, 255);
    private final int white = Color.WHITE;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock(1001, 1002);
            updateClock(2001, 2002);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        showHome();
        handler.post(clockTick);
    }

    private void updateClock(int clockId, int dateId) {
        TextView clock = findViewById(clockId);
        TextView date = findViewById(dateId);
        Date now = new Date();
        if (clock != null) clock.setText(new SimpleDateFormat("h:mm", Locale.getDefault()).format(now));
        if (date != null) date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
    }

    private GradientDrawable gradient(int stroke) {
        GradientDrawable d = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(25, 65, 112), bg, Color.rgb(4, 10, 24)});
        d.setCornerRadius(38);
        d.setStroke(1, stroke);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setBackground(gradient(0x557DBEFF));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void base(String subtitle) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 38, 22, 18);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(11, 39, 72), bg, Color.rgb(3, 8, 19)}));
        setContentView(root);
        root.addView(text("FROST  •  OS 26", 13, blue, true));
        root.addView(text(subtitle, 12, 0xFFB7CDE6, false));
    }

    private void showHome() {
        lockPreview = false;
        base("INTELLIGENCE IN EVERY INTERACTION");
        TextView clock = text("", 62, white, false);
        clock.setId(1001);
        clock.setGravity(Gravity.CENTER);
        TextView date = text("", 16, 0xFFD8E8FA, false);
        date.setId(1002);
        date.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(-1, -2);
        wide.topMargin = 34;
        root.addView(clock, wide);
        root.addView(date);

        TextView hint = text("Swipe up for apps  •  Swipe down for controls", 12, 0xFFB7CDE6, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = 22;
        root.addView(hint, hp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        String[] labels = {"Phone", "Messages", "Camera", "Photos", "Settings", "Calendar"};
        String[] actions = {"phone", "sms", "camera", "photos", "settings", "calendar"};
        for (int i = 0; i < labels.length; i++) {
            final String action = actions[i];
            Button tile = button(labels[i], () -> launchShortcut(action));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = 92;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(6, 6, 6, 6);
            grid.addView(tile, p);
        }
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = 18;
        root.addView(grid, gp);
        root.addView(button("APP LIBRARY", this::showDrawer));
        root.addView(button("PREVIEW LOCK SCREEN", this::showLockScreen));
    }

    private void showLockScreen() {
        lockPreview = true;
        base("LOCK SCREEN EXPERIENCE  •  PREVIEW");
        root.setPadding(22, 70, 22, 24);

        TextView greeting = text("Good " + timeOfDay(), 16, 0xFFD8E8FA, false);
        greeting.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(-1, -2);
        greetingParams.topMargin = 35;
        root.addView(greeting, greetingParams);

        TextView clock = text("", 82, white, false);
        clock.setId(2001);
        clock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = 8;
        root.addView(clock, cp);

        TextView date = text("", 17, 0xFFD8E8FA, false);
        date.setId(2002);
        date.setGravity(Gravity.CENTER);
        root.addView(date);

        TextView status = text("FROST OS 26  •  YOUR DAY, AT A GLANCE", 11, 0xFF9FC8F2, true);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 24;
        root.addView(status, sp);

        LinearLayout spacer = new LinearLayout(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setGravity(Gravity.CENTER);
        Button torch = button(torchOn ? "Turn flashlight off" : "Flashlight", this::toggleTorch);
        Button camera = button("Camera", () -> launchShortcut("camera"));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, 66, 1f);
        bp.setMargins(5, 5, 5, 5);
        shortcuts.addView(torch, bp);
        shortcuts.addView(camera, bp);
        root.addView(shortcuts);

        TextView swipe = text("Swipe up to return to FROST", 13, 0xFFD8E8FA, false);
        swipe.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, -2);
        up.topMargin = 28;
        root.addView(swipe, up);
        root.addView(button("EXIT PREVIEW", this::showHome));
    }

    private String timeOfDay() {
        int hour = Integer.parseInt(new SimpleDateFormat("H", Locale.getDefault()).format(new Date()));
        if (hour < 12) return "morning";
        if (hour < 18) return "afternoon";
        return "evening";
    }

    private void toggleTorch() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 41);
            return;
        }
        setTorch(!torchOn);
    }

    private void setTorch(boolean enabled) {
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (torchCameraId == null) {
                for (String id : manager.getCameraIdList()) {
                    Boolean hasFlash = manager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (Boolean.TRUE.equals(hasFlash)) { torchCameraId = id; break; }
                }
            }
            if (torchCameraId == null) {
                showMessage("This device did not report an available flashlight.");
                return;
            }
            manager.setTorchMode(torchCameraId, enabled);
            torchOn = enabled;
            if (lockPreview) showLockScreen();
        } catch (Exception e) {
            showMessage("Flashlight could not be changed. Check camera permission and try again.");
        }
    }

    private void showMessage(String message) {
        new android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 41 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setTorch(true);
        } else if (requestCode == 41) {
            showMessage("Camera permission is needed to operate the flashlight.");
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (Math.abs(dy) > 90 && Math.abs(dy) > Math.abs(dx) * 1.25f) {
                if (lockPreview && dy < 0) {
                    showHome();
                    return true;
                }
                if (!lockPreview) {
                    if (dy < 0) showDrawer(); else showControlCentre();
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void showControlCentre() {
        base("CONTROL CENTRE");
        root.addView(text("Quick access", 28, white, true));
        root.addView(button("Wi-Fi settings", () -> openSettings(Settings.ACTION_WIFI_SETTINGS)));
        root.addView(button("Bluetooth settings", () -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)));
        root.addView(button("Display & brightness", () -> openSettings(Settings.ACTION_DISPLAY_SETTINGS)));
        root.addView(button("Sound & volume", () -> openSettings(Settings.ACTION_SOUND_SETTINGS)));
        root.addView(button("Airplane mode settings", () -> openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)));
        root.addView(button("All Android settings", () -> openSettings(Settings.ACTION_SETTINGS)));
        root.addView(button("BACK TO FROST HOME", this::showHome));
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception ex) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void showDrawer() {
        base("APP LIBRARY");
        root.addView(text("Your applications", 26, white, true));
        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        scroll.addView(grid);
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, (a, b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        for (android.content.pm.ResolveInfo info : apps) {
            String label = String.valueOf(info.loadLabel(pm));
            String pkg = info.activityInfo.packageName;
            Button tile = button(label, () -> launchPackage(pkg));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0; p.height = 76;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(5, 5, 5, 5);
            grid.addView(tile, p);
        }
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(button("BACK TO HOME", this::showHome));
    }

    private void launchPackage(String pkg) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) startActivity(launch);
    }

    private void launchShortcut(String key) {
        if ("phone".equals(key)) openSettings(Intent.ACTION_DIAL);
        else if ("sms".equals(key)) {
            try { startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))); }
            catch (Exception e) { showDrawer(); }
        } else if ("camera".equals(key)) {
            try { startActivity(new Intent("android.media.action.IMAGE_CAPTURE")); }
            catch (Exception e) { showDrawer(); }
        } else if ("settings".equals(key)) openSettings(Settings.ACTION_SETTINGS);
        else if ("calendar".equals(key)) openFirstApp("calendar");
        else if ("photos".equals(key)) openFirstApp("gallery");
    }

    private void openFirstApp(String keyword) {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (android.content.pm.ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
            String label = String.valueOf(info.loadLabel(pm)).toLowerCase(Locale.ROOT);
            if (label.contains(keyword)) {
                Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launch != null) { startActivity(launch); return; }
            }
        }
        showDrawer();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(clockTick);
        if (torchOn) setTorch(false);
        super.onDestroy();
    }
}
