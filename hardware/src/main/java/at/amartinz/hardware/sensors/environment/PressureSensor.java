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

package at.amartinz.hardware.sensors.environment;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.widget.TextView;

import at.amartinz.hardware.R;
import at.amartinz.hardware.sensors.BaseSensor;

public class PressureSensor extends BaseSensor {
    public static final int TYPE = Sensor.TYPE_PRESSURE;

    private Sensor mSensor;

    private TextView mValue;
    private String mNote;

    @Override public int getImageResourceId() {
        return R.drawable.hardware_ic_pressure;
    }

    @Override public Sensor getSensor() {
        return mSensor;
    }

    public PressureSensor(final Context context) {
        super(context);
        getInflater().inflate(R.layout.hardware_merge_sensor_data_single, getDataContainer(), true);

        mSensor = getSensorManager().getDefaultSensor(TYPE);

        setup(R.string.hardware_sensor_pressure);

        mValue = (TextView) findViewById(R.id.sensor_data_single);
        mNote = getResources().getString(R.string.hardware_sensor_pressure_note, "hPa", "mbar");
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (mValue == null || event.values[0] > Integer.MAX_VALUE) {
            return;
        }

        final float value = event.values[0];
        mValue.post(new Runnable() {
            @Override public void run() {
                mValue.setText(String.format("%.3f\n\n%s", value, mNote));
            }
        });
    }

}
