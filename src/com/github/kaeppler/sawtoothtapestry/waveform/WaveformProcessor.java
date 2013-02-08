package com.github.kaeppler.sawtoothtapestry.waveform;

import com.github.kaeppler.sawtoothtapestry.R;
import com.github.kaeppler.sawtoothtapestry.model.Waveform;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.preference.PreferenceManager;

// Processes waveform metadata into a Bitmap that we can render and animate
public class WaveformProcessor {

    private Resources resources;
    private SharedPreferences preferences;

    public WaveformProcessor(Context context) {
        this.resources = context.getResources();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // turns waveform vector data into into a beautified bitmap
    public Waveform process(SampleData sampleData) {
        final int scaledWidth = resources.getDimensionPixelSize(R.dimen.waveform_width);
        final int scaledHeight = resources.getDimensionPixelSize(R.dimen.waveform_height);

        SampleData scaledData = sampleData.scale(scaledWidth);

        Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // make the entire bitmap a nice gradient
        applyGradient(canvas);

        // using CLEAR, we cut out the waveform by drawing transparent vertical bars
        Paint waveformPaint = new Paint();
        waveformPaint.setColor(Color.TRANSPARENT);
        waveformPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        for (int i = 0; i < scaledData.samples.length; i++) {
            final float scaledAmp = scaledData.samples[i];
            // etch out upper waveform side
            canvas.drawLine(
                    i, 0, // start (x,y)
                    i, scaledHeight / 2 - scaledAmp // end (x,y)
                    , waveformPaint);
            // etch out lower waveform side
            canvas.drawLine(
                    i, scaledHeight / 2 + scaledAmp,
                    i, scaledHeight
                    , waveformPaint);
        }

        return new Waveform(sampleData, bitmap);
    }

    public void applyGradient(Canvas canvas) {
        Paint gradientPaint = new Paint();
        int startColor = preferences.getInt(resources.getString(R.string.settings_key_waveform_color),
                resources.getColor(R.color.soundcloud_orange_light));
        int centerColor = Color.WHITE;

        float hsv[] = new float[3];
        Color.colorToHSV(startColor, hsv);
        hsv[0] -= 10;
        int endColor = Color.HSVToColor(128, hsv);

        LinearGradient gradient = new LinearGradient(0, 0, 0, canvas.getHeight(),
                new int[]{startColor, startColor, centerColor, endColor, endColor}, new float[]{
                0, 0.4f, 0.5f, 0.6f, 1}, TileMode.REPEAT);
        gradientPaint.setShader(gradient);
        canvas.drawPaint(gradientPaint);
    }
}
