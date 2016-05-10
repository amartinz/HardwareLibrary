/*
 * The MIT License
 *
 * Copyright (c) 2016 Alexander Martinz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package at.amartinz.hardware.sensors;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import at.amartinz.hardware.Constants;
import alexander.martinz.libs.hardware.R;

public abstract class BaseSensor extends FrameLayout implements SensorEventListener {
    private SensorManager mSensorManager;
    private LayoutInflater mInflater;

    private CardView mCardView;
    private ImageView mIcon;
    private TextView mTitle;
    private TextView mName;
    private TextView mVendor;
    private TextView mPowerUsage;
    private LinearLayout mDataContainer;

    private static int sBackgroundColorGlobal = Integer.MAX_VALUE;

    private static int sIconTintGlobal = Color.WHITE;
    private int mIconTint = Integer.MIN_VALUE;

    public abstract Sensor getSensor();

    public static boolean isSupported(final Context context, final int type) {
        return ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(type) != null;
    }

    public int getImageResourceId() {
        return R.drawable.hardware_ic_walk;
    }

    public Drawable getSensorImage() {
        final int color;

        // local tint overrides global tint
        if (mIconTint != Integer.MIN_VALUE) {
            color = mIconTint;
        } else {
            color = sIconTintGlobal;
        }

        final Drawable drawable = ContextCompat.getDrawable(getContext(), getImageResourceId()).mutate();
        drawable.setColorFilter(new LightingColorFilter(Color.BLACK, color));
        return drawable;
    }

    public void setIconTint(@ColorInt int iconTint) {
        mIconTint = iconTint;
    }

    public static void setIconTintGlobal(@ColorInt int iconTint) {
        sIconTintGlobal = iconTint;
    }

    public void setBackgroundColor(@ColorInt int backgroundColor) {
        mCardView.setCardBackgroundColor(backgroundColor);
    }

    public static void setBackgroundColorGlobal(@ColorInt int backgroundColor) {
        sBackgroundColorGlobal = backgroundColor;
    }

    public void registerSensor() {
        getSensorManager().registerListener(this, getSensor(), getSensorDelay());
    }

    public void unregisterSensor() {
        getSensorManager().unregisterListener(this, getSensor());
    }

    public BaseSensor(final Context context) {
        super(context, null);
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mInflater = LayoutInflater.from(context);

        final View v = LayoutInflater.from(context).inflate(R.layout.hardware_card_with_container, this, false);
        super.addView(v);

        mCardView = (CardView) findViewById(R.id.card_view_root);
        if (sBackgroundColorGlobal != Integer.MIN_VALUE) {
            mCardView.setCardBackgroundColor(sBackgroundColorGlobal);
        }

        final FrameLayout container = (FrameLayout) findViewById(R.id.layout_container);
        mInflater.inflate(R.layout.hardware_item_sensor, container, true);

        mIcon = (ImageView) findViewById(R.id.sensor_icon);
        mIcon.setImageDrawable(getSensorImage());

        mTitle = (TextView) findViewById(R.id.sensor_label);
        mName = (TextView) findViewById(R.id.sensor_name);
        mVendor = (TextView) findViewById(R.id.sensor_vendor);
        mPowerUsage = (TextView) findViewById(R.id.sensor_power_usage);

        mDataContainer = (LinearLayout) findViewById(R.id.sensor_data_container);
    }

    public void setup(final int titleResId) {
        getTitle().setText(titleResId);
        // set sensor name
        getName().setText(getSensor().getName());
        // set sensor vendor
        getVendor().setText(String.format("(%s)", getSensor().getVendor()));
        // set power usage
        getPowerUsage().setText(getPowerUsageString());
    }

    public void onResume() {
        registerSensor();
    }

    public void onPause() {
        unregisterSensor();
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }

    public SensorManager getSensorManager() {
        return mSensorManager;
    }

    public ImageView getIcon() {
        return mIcon;
    }

    public TextView getTitle() {
        return mTitle;
    }

    public TextView getName() {
        return mName;
    }

    public TextView getVendor() {
        return mVendor;
    }

    public TextView getPowerUsage() {
        return mPowerUsage;
    }

    public LinearLayout getDataContainer() {
        return mDataContainer;
    }

    public String getPowerUsageString() {
        return String.format("%s: %s mA", getResources().getString(R.string.hardware_power_usage), getSensor().getPower());
    }

    public int getSensorDelay() {
        return SensorManager.SENSOR_DELAY_UI;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (Constants.DEBUG) {
            Log.d(this.getClass().getSimpleName(),
                    String.format("onAccuracyChanged: %s (%s), %s", sensor.getName(), sensor.getVendor(), accuracy));
        }
    }

}
