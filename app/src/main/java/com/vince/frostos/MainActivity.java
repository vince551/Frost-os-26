package com.vince.frostos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private LinearLayout root;
    private float downX, downY;
    private boolean gestureHandled = false;
    private final int bg = Color.rgb(7, 19, 38);
    private final int blue = Color.rgb(93, 184, 255);
    private final int white = Color.WHITE;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            TextView clock = findViewById(1001);
            TextView date = findViewById(1002);
            if (clock != null) clock.setText(new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date()));
            if (date != null) date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));
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
    }

    private GradientDrawable gradient(int color, int stroke) {
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
        b.setBackground(gradient(Color.TRANSPARENT, 0x557DBEFF));
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
        base("INTELLIGENCE IN EVERY INTERACTION");
        TextView clock = text("", 62, white, false);
        clock.setId(1001);
        clock.setGravity(Gravity.CENTER);
        TextView date = text("", 16, 0xFFD8E8FA, false);
        date.setId(1002);
        date.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams wide = new LinearLayout.LayoutParams(-1, -2);
        wide.topMargin = 46;
        root.addView(clock, wide);
        root.addView(date);
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);

        TextView hint = text("Swipe up for apps  •  Swipe down for controls", 12, 0xFFB7CDE6, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = 34;
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
            p.height = 112;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(6, 6, 6, 6);
            grid.addView(tile, p);
        }
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, 0, 1f);
        gp.topMargin = 28;
        root.addView(grid, gp);
        root.addView(button("APP LIBRARY", this::showDrawer));
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (Math.abs(dy) > 90 && Math.abs(dy) > Math.abs(dx) * 1.25f) {
                if (dy < 0) showDrawer(); else showControlCentre();
                return true;
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
        }
        else if ("camera".equals(key)) {
            try { startActivity(new Intent("android.media.action.IMAGE_CAPTURE")); }
            catch (Exception e) { showDrawer(); }
        } else if ("settings".equals(key)) openSettings(Settings.ACTION_SETTINGS);
        else if ("calendar".equals(key)) {
            openFirstApp("calendar");
        } else if ("photos".equals(key)) {
            openFirstApp("gallery");
        }
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
        super.onDestroy();
    }
}
