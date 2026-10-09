package com.example.classicalguitartuner;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.Gravity;

public class MainActivity extends Activity {
    private static final int RATE = 44100, WINDOW = 8192, REQUEST_MIC = 42;
    private static final String[] NAMES = {"E2", "A2", "D3", "G3", "B3", "E4"};
    private static final double[] FREQ = {82.4069, 110.0, 146.8324, 196.0, 246.9417, 329.6276};
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private Thread worker;
    private AudioRecord recorder;
    private TextView note, detail, status;
    private NeedleView needle;
    private int selected = -1;
    private final Button[] strings = new Button[6];

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(13, 24, 29));
        getWindow().setNavigationBarColor(Color.rgb(13, 24, 29));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(20, 32, 20, 20);
        root.setBackgroundColor(Color.rgb(13, 24, 29));
        setContentView(root);
        TextView heading = label("CLASSICAL GUITAR TUNER", 21, Color.WHITE);
        root.addView(heading);
        TextView help = label("Tap a string to lock, or use AUTO", 14, 0xffa4b8bd);
        root.addView(help);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        root.addView(row, new LinearLayout.LayoutParams(-1, 80));
        String[] buttons = {"E", "A", "D", "G", "B", "e"};
        for (int i = 0; i < 6; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(buttons[i]);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setPadding(0, 0, 0, 0);
            row.addView(b, new LinearLayout.LayoutParams(0, 64, 1));
            b.setOnClickListener(v -> { selected = index; updateButtons(); status.setText("Play " + NAMES[index]); });
            strings[i] = b;
        }
        Button auto = new Button(this);
        auto.setText("AUTO DETECT");
        root.addView(auto);
        auto.setOnClickListener(v -> { selected = -1; updateButtons(); status.setText("Play any open string"); });
        note = label("--", 76, Color.WHITE);
        root.addView(note);
        detail = label("-- Hz", 22, 0xff70e4b2);
        root.addView(detail);
        needle = new NeedleView();
        root.addView(needle, new LinearLayout.LayoutParams(-1, 220));
        status = label("Microphone permission needed", 19, 0xff70e4b2);
        root.addView(status);
        TextView legend = label("FLAT  ♭           IN TUNE           ♯  SHARP", 13, 0xffb7c6c9);
        root.addView(legend);
        updateButtons();
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
        } else startListening();
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(0, 12, 0, 12);
        return t;
    }
    private void updateButtons() {
        for (int i = 0; i < 6; i++) strings[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(i == selected ? 0xff178969 : 0xff2b4148));
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == REQUEST_MIC) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startListening();
            else status.setText("Enable microphone access in Android settings");
        }
    }
    private synchronized void startListening() {
        if (running) return;
        int minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minimum <= 0) { status.setText("Microphone unavailable"); return; }
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, WINDOW * 2));
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) { recorder.release(); recorder = null; status.setText("Microphone initialization failed"); return; }
            recorder.startRecording();
        } catch (SecurityException | IllegalStateException e) { status.setText("Cannot start microphone"); return; }
        running = true;
        status.setText("Play any open string");
        worker = new Thread(() -> {
            short[] buffer = new short[WINDOW];
            while (running) {
                int read = 0;
                while (running && read < WINDOW) {
                    int n = recorder.read(buffer, read, WINDOW - read);
                    if (n <= 0) break;
                    read += n;
                }
                if (read != WINDOW) continue;
                double hz = detectPitch(buffer);
                ui.post(() -> showPitch(hz));
            }
        }, "PitchDetection");
        worker.start();
    }
    private void showPitch(double hz) {
        if (!running) return;
        if (hz <= 0) { status.setText("Play a single open string"); return; }
        int idx = selected;
        if (idx < 0) {
            double best = Double.MAX_VALUE;
            for (int i = 0; i < 6; i++) {
                double distance = Math.abs(1200 * Math.log(hz / FREQ[i]) / Math.log(2));
                if (distance < best) { best = distance; idx = i; }
            }
            if (best > 180) { status.setText("Play an open guitar string"); return; }
        }
        double cents = 1200 * Math.log(hz / FREQ[idx]) / Math.log(2);
        note.setText(NAMES[idx]);
        detail.setText(String.format(java.util.Locale.US, "%.1f Hz  •  %+.0f cents", hz, cents));
        needle.setCents(cents);
        status.setText(Math.abs(cents) <= 5 ? "✓ IN TUNE" : cents < 0 ? "FLAT — tighten the string" : "SHARP — loosen the string");
    }
    // Normalized square difference (YIN-style): selects the first strong periodicity,
    // reducing octave errors caused by prominent harmonics on nylon strings.
    private double detectPitch(short[] samples) {
        final int size = samples.length, minLag = RATE / 370, maxLag = RATE / 70;
        double energy = 0;
        for (short s : samples) energy += (double)s * s;
        if (Math.sqrt(energy / size) < 170) return -1;
        double[] diff = new double[maxLag + 2];
        for (int tau = 1; tau <= maxLag + 1; tau++) {
            double sum = 0;
            for (int j = 0; j < size - maxLag - 1; j += 2) {
                double d = samples[j] - samples[j + tau];
                sum += d * d;
            }
            diff[tau] = sum;
        }
        double cumulative = 0;
        double[] cmnd = new double[maxLag + 2];
        cmnd[0] = 1;
        for (int tau = 1; tau < cmnd.length; tau++) {
            cumulative += diff[tau];
            cmnd[tau] = cumulative == 0 ? 1 : diff[tau] * tau / cumulative;
        }
        int lag = -1;
        for (int tau = minLag; tau <= maxLag; tau++) {
            if (cmnd[tau] < 0.14) {
                while (tau + 1 <= maxLag && cmnd[tau + 1] < cmnd[tau]) tau++;
                lag = tau; break;
            }
        }
        if (lag < 0) return -1;
        double a = cmnd[lag - 1], b = cmnd[lag], c = cmnd[lag + 1];
        double offset = (a - 2 * b + c) == 0 ? 0 : 0.5 * (a - c) / (a - 2 * b + c);
        if (Math.abs(offset) > 1) offset = 0;
        return RATE / (lag + offset);
    }
    @Override protected void onStop() { super.onStop(); stopListening(); }
    @Override protected void onStart() { super.onStart(); if (note != null && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening(); }
    private synchronized void stopListening() {
        running = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (IllegalStateException ignored) {}
            if (worker != null) { try { worker.join(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } }
            recorder.release(); recorder = null;
        }
    }
    private class NeedleView extends View {
        private final Paint paint = new Paint(3);
        private double cents = 0;
        NeedleView() { super(MainActivity.this); }
        void setCents(double value) { cents = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth()/2f, cy = getHeight()*0.78f;
            float radius = Math.min(getWidth()*0.43f, getHeight()*0.68f);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(13); paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xff344e53);
            canvas.drawArc(cx-radius, cy-radius, cx+radius, cy+radius, 200, 140, false, paint);
            paint.setColor(0xff70e4b2);
            canvas.drawArc(cx-radius, cy-radius, cx+radius, cy+radius, 262, 16, false, paint);
            double angle = Math.toRadians(270 + Math.max(-50, Math.min(50, cents)) * 1.4);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5);
            canvas.drawLine(cx, cy, cx + (float)Math.cos(angle)*radius*0.90f, cy + (float)Math.sin(angle)*radius*0.90f, paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawCircle(cx, cy, 9, paint);
        }
    }
}
