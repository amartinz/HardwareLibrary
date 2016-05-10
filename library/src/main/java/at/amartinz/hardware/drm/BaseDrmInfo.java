/*
 * Copyright (C) 2013 - 2015 Alexander Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.amartinz.hardware.drm;

import android.annotation.TargetApi;
import android.media.MediaDrm;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.UUID;

/**
 * Created by amartinz on 27.04.16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BaseDrmInfo {
    private final MediaDrm mediaDrm;

    private String vendor;
    private String version;
    private String description;

    private String systemId;
    private String deviceId;

    private String algorithms;

    private String securityLevel;
    private String hdcpLevel;
    private String hdcpLevelMax;

    private boolean isUsageReportingSupported;

    private String sessionNumberMax;
    private String sessionNumberOpen;

    public BaseDrmInfo() {
        this.mediaDrm = setup();
    }

    private MediaDrm setup() {
        MediaDrm drm;
        try {
            drm = new MediaDrm(getUuid());
        } catch (UnsupportedSchemeException use) {
            drm = null;
        }

        if (drm == null) {
            return null;
        }

        vendor = getProperty(drm, MediaDrm.PROPERTY_VENDOR);
        version = getProperty(drm, MediaDrm.PROPERTY_VERSION);
        description = getProperty(drm, MediaDrm.PROPERTY_DESCRIPTION);

        systemId = getProperty(drm, "systemId");
        deviceId = getProperty(drm, MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);

        algorithms = getProperty(drm, MediaDrm.PROPERTY_ALGORITHMS);
        algorithms = splitList(algorithms);

        securityLevel = getProperty(drm, "securityLevel");
        hdcpLevel = getProperty(drm, "hdcpLevel");
        hdcpLevelMax = getProperty(drm, "maxHdcpLevel");

        isUsageReportingSupported = getBooleanProperty(drm, "usageReportingSupport");

        sessionNumberMax = getProperty(drm, "maxNumberOfSessions");
        sessionNumberOpen = getProperty(drm, "numberOfOpenSessions");

        return drm;
    }

    public abstract UUID getUuid();

    private static boolean getBooleanProperty(MediaDrm drm, String propertyName) {
        String property;
        try {
            property = drm.getPropertyString(propertyName);
        } catch (Exception exc) {
            property = null;
        }
        property = BaseDrmInfo.sanitize(property);

        if (TextUtils.isEmpty(property)) {
            return false;
        }

        property = property.toLowerCase();

        return "true".equals(property) || "1".equals(property) || "yes".equals(property) || Boolean.parseBoolean(property);
    }

    @Nullable private static String getProperty(MediaDrm drm, String propertyName) {
        String property;
        try {
            property = drm.getPropertyString(propertyName);
        } catch (Exception exc) {
            property = null;
        }

        return BaseDrmInfo.sanitize(property);
    }

    @Nullable private static String sanitize(@Nullable String toSanitize) {
        // if we are empty, return null instead of an empty value
        if (TextUtils.isEmpty(toSanitize)) {
            return null;
        }

        // XXX: investigate properties in the wild and extend

        return toSanitize.trim();
    }

    @Nullable private static String splitList(String property) {
        if (TextUtils.isEmpty(property)) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        final TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(property);

        final Iterator<String> stringIterator = splitter.iterator();
        while (stringIterator.hasNext()) {
            final String next = stringIterator.next();
            sb.append(next);
            if (stringIterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override public String toString() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("vendor", vendor);
            jsonObject.put("version", version);
            jsonObject.put("description", description);

            jsonObject.put("systemId", systemId);
            jsonObject.put("deviceId", deviceId);

            jsonObject.put("algorithms", algorithms);

            jsonObject.put("securityLevel", securityLevel);
            jsonObject.put("hdcpLevel", hdcpLevel);
            jsonObject.put("hdcpLevelMax", hdcpLevelMax);

            jsonObject.put("isUsageReportingSupported", isUsageReportingSupported);

            jsonObject.put("sessionNumberMax", sessionNumberMax);
            jsonObject.put("sessionNumberOpen", sessionNumberOpen);
        } catch (Exception ignored) { }

        try {
            return jsonObject.toString(2);
        } catch (Exception ignored) { }
        return super.toString();
    }

    public boolean isSupported() {
        return mediaDrm != null;
    }

    @Nullable public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getDescription() {
        return description;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAlgorithms() {
        return algorithms;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public String getHdcpLevel() {
        return hdcpLevel;
    }

    public String getHdcpLevelMax() {
        return hdcpLevelMax;
    }

    public boolean isUsageReportingSupported() {
        return isUsageReportingSupported;
    }

    public String getSessionNumberMax() {
        return sessionNumberMax;
    }

    public String getSessionNumberOpen() {
        return sessionNumberOpen;
    }
}
