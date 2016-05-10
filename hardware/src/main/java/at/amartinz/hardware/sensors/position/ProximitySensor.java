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

package at.amartinz.hardware.sensors.position;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.widget.TextView;

import at.amartinz.hardware.R;
import at.amartinz.hardware.sensors.BaseSensor;

public class ProximitySensor extends BaseSensor {
    public static final int TYPE = Sensor.TYPE_PROXIMITY;

    private Sensor mSensor;
    private float mMaxRange;

    private TextView mState;

    @Override public int getImageResourceId() {
        // TODO: icon
        return R.drawable.hardware_empty_icon;
    }

    @Override public Sensor getSensor() {
        return mSensor;
    }

    public ProximitySensor(final Context context) {
        super(context);
        getInflater().inflate(R.layout.hardware_merge_sensor_data_single, getDataContainer(), true);

        mSensor = getSensorManager().getDefaultSensor(TYPE);
        mMaxRange = mSensor.getMaximumRange();

        setup(R.string.hardware_sensor_proximity);

        mState = (TextView) findViewById(R.id.sensor_data_single);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (mState == null || event.values[0] > Integer.MAX_VALUE) {
            return;
        }

        final float state = event.values[0];
        final boolean isFar = state >= mMaxRange;
        final String stateString = isFar
                ? getResources().getString(R.string.hardware_far) : getResources().getString(R.string.hardware_near);
        mState.post(new Runnable() {
            @Override public void run() {
                mState.setText(String.format("%s (%s)", stateString, String.valueOf(state)));
            }
        });
    }

}
