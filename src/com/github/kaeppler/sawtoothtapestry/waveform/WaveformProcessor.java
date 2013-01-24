package com.github.kaeppler.sawtoothtapestry.waveform;

import com.github.kaeppler.sawtoothtapestry.R;
import com.github.kaeppler.sawtoothtapestry.R.color;
import com.github.kaeppler.sawtoothtapestry.R.dimen;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;

// Processes waveform metadata into a Bitmap that we can render and animate
public class WaveformProcessor {

    private Resources resources;

    public WaveformProcessor(Context context) {
        this.resources = context.getResources();
    }

    // turns waveform vector data into into a beautified bitmap
    public Bitmap process(WaveformData waveformData) {
        final int scaledWidth = resources.getDimensionPixelSize(R.dimen.waveform_width);
        final int scaledHeight = resources.getDimensionPixelSize(R.dimen.waveform_height);

        WaveformData scaledData = waveformData.scale(scaledWidth);

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

        return bitmap;
    }

    private void applyGradient(Canvas canvas) {
        Paint gradientPaint = new Paint();
        int startColor = resources.getColor(R.color.soundcloud_orange_light);
        int centerColor = Color.WHITE;
        int endColor = resources.getColor(R.color.soundcloud_orange_dark);
        LinearGradient gradient = new LinearGradient(0, 0, 0, canvas.getHeight(),
                new int[]{startColor, startColor, centerColor, endColor, endColor}, new float[]{
                0, 0.4f, 0.5f, 0.6f, 1}, TileMode.REPEAT);
        gradientPaint.setShader(gradient);
        canvas.drawPaint(gradientPaint);
    }
}
