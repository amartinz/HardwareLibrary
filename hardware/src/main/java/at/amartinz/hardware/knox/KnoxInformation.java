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
package at.amartinz.hardware.knox;

import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import org.json.JSONObject;

import at.amartinz.execution.NormalShell;

/**
 * Created by amartinz on 28.04.16.
 * <p/>
 * Note: for more information, use the KNOX sdk
 */
public class KnoxInformation {
    private String warrantyBit;
    private String csc;
    private String pda;

    private boolean isKnoxAppRunning;

    private String versionEnterpriseBilling;
    private String versionEnterpriseOtp;
    private String versionScep;
    private String versionSso;
    private String versionTima;
    private String versionVpn;

    private String selinuxVersion;

    @WorkerThread public static KnoxInformation get() {
        final KnoxInformation knoxInformation = new KnoxInformation();

        String warrantyBit = getProp("ro.boot.warranty_bit", null);
        if (TextUtils.isEmpty(warrantyBit)) {
            warrantyBit = getProp("ro.warranty_bit", null);
        }
        if (TextUtils.isEmpty(warrantyBit)) {
            warrantyBit = "-";
        } else {
            if (!warrantyBit.startsWith("0x") || "0".equals(warrantyBit) || "1".equals(warrantyBit)) {
                warrantyBit = String.format("0x%s", warrantyBit);
            }
        }
        knoxInformation.warrantyBit = warrantyBit;
        knoxInformation.csc = getProp("ril.official_cscver", "-");
        knoxInformation.pda = getProp("ro.build.PDA", "-");

        knoxInformation.isKnoxAppRunning = getBooleanProp("dev.knoxapp.running");

        knoxInformation.versionEnterpriseBilling = getProp("sys.enterprise.billing.version", "-");
        knoxInformation.versionEnterpriseOtp = getProp("sys.enterprise.otp.version", "-");
        knoxInformation.versionScep = getProp("net.knoxscep.version", "-");
        knoxInformation.versionSso = getProp("net.knoxsso.version", "-");
        knoxInformation.versionTima = getProp("ro.config.timaversion", "-");
        knoxInformation.versionVpn = getProp("net.knoxvpn.version", "-");

        knoxInformation.selinuxVersion = getProp("selinux.policy_version", "-");

        return knoxInformation;
    }

    private static boolean getBooleanProp(String property) {
        String result = getProp(property, "false");
        if (TextUtils.isEmpty(result)) {
            return false;
        }
        result = result.toLowerCase();
        return "true".equals(result) || "1".equals(result) || Boolean.parseBoolean(result);
    }

    @Nullable private static String getProp(String property, String defaultValue) {
        final String result = NormalShell.fireAndBlockString(getPropCommand(property));
        if (TextUtils.isEmpty(result)) {
            return defaultValue;
        }
        return result.trim();
    }

    private static String getPropCommand(String property) {
        return String.format("getprop %s", property);
    }

    public String getWarrantyBit() {
        return warrantyBit;
    }

    public String getCsc() {
        return csc;
    }

    public String getPda() {
        return pda;
    }

    public boolean isKnoxAppRunning() {
        return isKnoxAppRunning;
    }

    public String getVersionEnterpriseBilling() {
        return versionEnterpriseBilling;
    }

    public String getVersionEnterpriseOtp() {
        return versionEnterpriseOtp;
    }

    public String getVersionScep() {
        return versionScep;
    }

    public String getVersionSso() {
        return versionSso;
    }

    public String getVersionTima() {
        return versionTima;
    }

    public String getVersionVpn() {
        return versionVpn;
    }

    public String getSelinuxVersion() {
        return selinuxVersion;
    }

    @Override public String toString() {
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("warrantyBit", warrantyBit);
            jsonObject.put("csc", csc);
            jsonObject.put("pda", pda);

            jsonObject.put("isKnoxAppRunning", isKnoxAppRunning);

            jsonObject.put("versionEnterpriseBilling", versionEnterpriseBilling);
            jsonObject.put("versionEnterpriseOtp", versionEnterpriseOtp);
            jsonObject.put("versionScep", versionScep);
            jsonObject.put("versionSso", versionSso);
            jsonObject.put("versionTima", versionTima);
            jsonObject.put("versionVpn", versionVpn);

            jsonObject.put("selinuxVersion", selinuxVersion);
        } catch (Exception ignored) { }

        try {
            return jsonObject.toString(2);
        } catch (Exception ignored) { }
        return super.toString();
    }
}
