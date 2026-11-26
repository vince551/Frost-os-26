package com.vince.frostos;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
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
    private String currentScreen = "home";
    private boolean torchOn = false;
    private String torchCameraId;
    private static final int PICK_WALLPAPER = 77;
    private String wallpaperUri;
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
        wallpaperUri = getPreferences(MODE_PRIVATE).getString("wallpaper_uri", null);
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
        currentScreen = subtitle;
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
        applySavedWallpaper();
        root.setPadding(18, 34, 18, 16);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("FROST", 19, white, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(button("⚙", this::showFrostSettings));
        root.addView(top);

        LinearLayout widget = new LinearLayout(this);
        widget.setOrientation(LinearLayout.VERTICAL);
        widget.setPadding(20, 20, 20, 20);
        widget.setGravity(Gravity.CENTER);
        widget.setBackground(gradient(0x887DBEFF));
        TextView clock = text("", 58, white, false);
        clock.setId(1001);
        clock.setGravity(Gravity.CENTER);
        TextView date = text("", 15, 0xFFE0EEFF, false);
        date.setId(1002);
        date.setGravity(Gravity.CENTER);
        widget.addView(clock);
        widget.addView(date);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.topMargin = 18;
        root.addView(widget, wp);

        TextView section = text("FAVOURITES", 11, blue, true);
        LinearLayout.LayoutParams sh = new LinearLayout.LayoutParams(-1, -2);
        sh.topMargin = 22;
        root.addView(section, sh);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        String[] labels = {"Phone", "Messages", "Camera", "Photos", "Settings", "Calendar"};
        String[] glyphs = {"☎", "✉", "◉", "▧", "⚙", "▦"};
        String[] actions = {"phone", "sms", "camera", "photos", "settings", "calendar"};
        for (int i = 0; i < labels.length; i++) {
            final String action = actions[i];
            View tile = appTile(glyphs[i], labels[i], () -> launchShortcut(action));
            GridLayout.LayoutParams tileParams = new GridLayout.LayoutParams();
            tileParams.width = 0;
            tileParams.height = -2;
            tileParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            tileParams.setMargins(2, 2, 2, 2);
            grid.addView(tile, tileParams);
        }
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = 8;
        root.addView(grid, gp);

        TextView hint = text("Swipe up for your apps  •  Swipe down for controls", 11, 0xFFB7CDE6, false);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = 10;
        root.addView(hint, hp);

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(6, 8, 6, 8);
        dock.setBackground(gradient(0x667DBEFF));
        dock.addView(dockItem("Apps", "▦", this::showDrawer));
        dock.addView(dockItem("Controls", "☷", this::showControlCentre));
        dock.addView(dockItem("Wallpaper", "◈", this::chooseWallpaper));
        dock.addView(dockItem("Lock", "⌑", this::showLockScreen));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = 18;
        root.addView(dock, dp);
    }

    private View appTile(String glyph, String label, Runnable action) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(4, 10, 4, 10);
        TextView icon = text(glyph, 27, white, true);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF27649B, 0xFF102D50});
        iconBg.setCornerRadius(22);
        iconBg.setStroke(1, 0x667DBEFF);
        icon.setBackground(iconBg);
        tile.addView(icon, new LinearLayout.LayoutParams(62, 62));
        TextView name = text(label, 11, white, true);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = 6;
        tile.addView(name, np);
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private View dockItem(String label, String glyph, Runnable action) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView icon = text(glyph, 22, white, true);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon);
        TextView caption = text(label, 10, 0xFFD8E8FA, false);
        caption.setGravity(Gravity.CENTER);
        item.addView(caption);
        item.setOnClickListener(v -> action.run());
        item.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        return item;
    }

    private void chooseWallpaper() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        startActivityForResult(pick, PICK_WALLPAPER);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_WALLPAPER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                wallpaperUri = uri.toString();
                getPreferences(MODE_PRIVATE).edit().putString("wallpaper_uri", wallpaperUri).apply();
                showHome();
            } catch (Exception e) {
                showMessage("That wallpaper could not be saved. Please choose another image.");
            }
        }
    }

    private void applySavedWallpaper() {
        if (wallpaperUri == null) return;
        try {
            Uri uri = Uri.parse(wallpaperUri);
            android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 2000) sample *= 2;
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = sample;
            android.graphics.Bitmap bitmap;
            try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(in, null, opts);
            }
            if (bitmap != null) {
                Drawable image = new BitmapDrawable(getResources(), bitmap);
                image.setAlpha(150);
                root.setBackground(image);
            }
        } catch (Exception ignored) {}
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
                if (!lockPreview && "INTELLIGENCE IN EVERY INTERACTION".equals(currentScreen)) {
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
        addVolumeControl();
        addBrightnessControl();
        root.addView(button("Wi-Fi settings", () -> openSettings(Settings.ACTION_WIFI_SETTINGS)));
        root.addView(button("Bluetooth settings", () -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)));
        root.addView(button("Display & brightness", () -> openSettings(Settings.ACTION_DISPLAY_SETTINGS)));
        root.addView(button("Sound & volume", () -> openSettings(Settings.ACTION_SOUND_SETTINGS)));
        root.addView(button("Airplane mode settings", () -> openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)));
        root.addView(button("Enable Dynamic Island overlay", this::requestIslandOverlay));
        root.addView(button("Enable notification access", () -> openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(text("Dynamic Island overlay: " + (Settings.canDrawOverlays(this) ? "Enabled" : "Permission needed"), 12, blue, false));
        root.addView(button("FROST Settings", this::showFrostSettings));
        root.addView(button("All Android settings", () -> openSettings(Settings.ACTION_SETTINGS)));
        root.addView(button("BACK TO FROST HOME", this::showHome));
    }

    private void addVolumeControl() {
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        root.addView(text("Media volume", 13, 0xFFD8E8FA, true));
        SeekBar seek = new SeekBar(this);
        seek.setMax(Math.max(1, max));
        seek.setProgress(current);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser) audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        root.addView(seek, new LinearLayout.LayoutParams(-1, 42));
    }

    private void addBrightnessControl() {
        root.addView(text("Screen brightness", 13, 0xFFD8E8FA, true));
        SeekBar seek = new SeekBar(this);
        seek.setMax(255);
        int current = 128;
        try { current = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
        catch (Exception ignored) {}
        seek.setProgress(Math.max(1, current));
        boolean allowed = Settings.System.canWrite(this);
        seek.setEnabled(allowed);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                if (fromUser && Settings.System.canWrite(MainActivity.this)) {
                    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.max(1, value));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        root.addView(seek, new LinearLayout.LayoutParams(-1, 42));
        if (!allowed) root.addView(button("Allow brightness control", () -> {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
            } catch (Exception e) { openSettings(Settings.ACTION_DISPLAY_SETTINGS); }
        }));
    }

    private void requestIslandOverlay() {
        if (Settings.canDrawOverlays(this)) {
            showMessage("Dynamic Island overlay permission is already enabled.");
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            openSettings(Settings.ACTION_SETTINGS);
        }
    }

    private void showFrostSettings() {
        base("PERSONALISE YOUR EXPERIENCE");
        root.addView(text("FROST Settings", 28, white, true));
        root.addView(text("System permissions", 14, blue, true));
        root.addView(button("Dynamic Island overlay permission", this::requestIslandOverlay));
        root.addView(button("Notification access", () -> openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(button("Choose home wallpaper", this::chooseWallpaper));
        root.addView(button("Android home app settings", () -> openSettings(Settings.ACTION_HOME_SETTINGS)));
        root.addView(text("Android keeps secure unlock, protected quick settings and system notifications under its own control.", 13, 0xFFD8E8FA, false));
        root.addView(button("BACK TO HOME", this::showHome));
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception ex) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    private void showDrawer() {
        base("APP LIBRARY");
        root.addView(text("Your applications", 26, white, true));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(white);
        search.setHintTextColor(0xFF9CB6D2);
        search.setHint("Search apps");
        search.setPadding(18, 8, 18, 8);
        search.setBackground(gradient(0x667DBEFF));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 52);
        sp.topMargin = 12;
        root.addView(search, sp);
        ScrollView scroll = new ScrollView(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        scroll.addView(grid);
        populateAppGrid(grid, "");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                populateAppGrid(grid, s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(button("BACK TO HOME", this::showHome));
    }

    private void populateAppGrid(GridLayout grid, String filter) {
        grid.removeAllViews();
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, (a, b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        for (android.content.pm.ResolveInfo info : apps) {
            String label = String.valueOf(info.loadLabel(pm));
            if (!label.toLowerCase(Locale.ROOT).contains(needle)) continue;
            String pkg = info.activityInfo.packageName;
            View tile = appTile("◉", label, () -> launchPackage(pkg));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = -2;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(2, 2, 2, 2);
            grid.addView(tile, p);
        }
        if (grid.getChildCount() == 0) {
            TextView empty = text("No apps found", 14, 0xFFD8E8FA, false);
            grid.addView(empty);
        }
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
