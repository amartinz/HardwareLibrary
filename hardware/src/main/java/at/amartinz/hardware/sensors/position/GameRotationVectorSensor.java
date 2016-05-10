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

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Build;
import android.widget.TextView;

import at.amartinz.hardware.R;
import at.amartinz.hardware.sensors.BaseSensor;

/**
 * Only available on Jelly Bean MR2 and above
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GameRotationVectorSensor extends BaseSensor {
    public static final int TYPE = Sensor.TYPE_GAME_ROTATION_VECTOR;

    private Sensor mSensor;

    private TextView mValue;

    @Override public int getImageResourceId() {
        return R.drawable.hardware_ic_rotation_sensor;
    }

    @Override public Sensor getSensor() {
        return mSensor;
    }

    public GameRotationVectorSensor(final Context context) {
        super(context);
        getInflater().inflate(R.layout.hardware_merge_sensor_data_single, getDataContainer(), true);

        mSensor = getSensorManager().getDefaultSensor(TYPE);

        setup(R.string.hardware_sensor_game_rotation_vector);

        mValue = (TextView) findViewById(R.id.sensor_data_single);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (mValue == null || event.values[0] > Integer.MAX_VALUE) {
            return;
        }

        final float x = event.values[0];
        final float y = event.values[1];
        final float z = event.values[2];
        mValue.post(new Runnable() {
            @Override public void run() {
                mValue.setText(String.format("x: %.3f\ny: %.3f\nz: %.3f", x, y, z));
            }
        });
    }

}
