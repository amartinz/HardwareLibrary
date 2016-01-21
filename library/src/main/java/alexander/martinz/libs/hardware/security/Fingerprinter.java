/*
 * Copyright (C) 2013 - 2016 Alexander Martinz
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
package alexander.martinz.libs.hardware.security;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

import javax.crypto.Cipher;

import alexander.martinz.libs.hardware.Constants;

@TargetApi(Build.VERSION_CODES.M)
public class Fingerprinter {
    private static final String TAG = Fingerprinter.class.getSimpleName();

    public static final int SETUP_OK = 0;
    public static final int SETUP_NO_HARDWARE = 1;
    public static final int SETUP_NO_SECURE_LOCK_SCREEN = 2;
    public static final int SETUP_NO_FINGERPRINTS = 3;

    public final Context context;
    private final CryptoHelper cryptoHelper;

    private final KeyguardManager keyguardManager;
    private final FingerprintManagerCompat fingerprintManager;
    private final FingerprinterCallback authenticationCallback;

    private Cipher cipherEnc;
    private FingerprintManagerCompat.CryptoObject cryptoObjectEnc;

    private CancellationSignal cancellationSignal;

    private boolean selfCanceled;

    public static class FingerprinterCallback extends FingerprintManagerCompat.AuthenticationCallback { }

    public Fingerprinter(@NonNull Context context, FingerprinterCallback authenticationCallback) {
        this(context, authenticationCallback, null);
    }

    public Fingerprinter(@NonNull Context context, FingerprinterCallback authenticationCallback, @Nullable String keyName) {
        this.context = context;
        this.cryptoHelper = new CryptoHelper(context, keyName);

        this.keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        this.fingerprintManager = FingerprintManagerCompat.from(context);

        this.authenticationCallback = authenticationCallback;
    }

    public boolean init() {
        if (cipherEnc == null) {
            cipherEnc = cryptoHelper.initCipherEncryption();
        }
        if (cipherEnc == null) {
            return false;
        }
        if (cryptoObjectEnc == null) {
            cryptoObjectEnc = new FingerprintManagerCompat.CryptoObject(cipherEnc);
        }

        return true;
    }

    public void onDestroy() {
        stopListening();
    }

    public int hasFingerprintsSetup() {
        if (!fingerprintManager.isHardwareDetected()) {
            if (Constants.DEBUG) {
                Log.d(TAG, "No fingerprint hardware detected!");
            }
            return SETUP_NO_HARDWARE;
        }
        if (!keyguardManager.isKeyguardSecure()) {
            if (Constants.DEBUG) {
                Log.d(TAG, "No secure lock screen set up!");
            }
            return SETUP_NO_SECURE_LOCK_SCREEN;
        }
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            if (Constants.DEBUG) {
                Log.d(TAG, "User did not setup fingerprints!");
            }
            return SETUP_NO_FINGERPRINTS;
        }
        return SETUP_OK;
    }

    public void startListening() {
        if (hasFingerprintsSetup() != SETUP_OK) {
            return;
        }

        cancellationSignal = new CancellationSignal();
        selfCanceled = false;
        fingerprintManager.authenticate(cryptoObjectEnc, 0 /* flags */, cancellationSignal, fingerprinterCallback, null);
    }

    public void stopListening() {
        if (cancellationSignal != null) {
            selfCanceled = true;
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    public Cipher getCipherEnc() {
        return cipherEnc;
    }

    private final FingerprinterCallback fingerprinterCallback = new FingerprinterCallback() {
        @Override public void onAuthenticationError(int errMsgId, CharSequence errString) {
            if (!selfCanceled) {
                if (authenticationCallback != null) {
                    authenticationCallback.onAuthenticationError(errMsgId, errString);
                }
            }
        }

        @Override public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            if (authenticationCallback != null) {
                authenticationCallback.onAuthenticationHelp(helpMsgId, helpString);
            }
        }

        @Override public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            if (authenticationCallback != null) {
                authenticationCallback.onAuthenticationSucceeded(result);
            }
        }

        @Override public void onAuthenticationFailed() {
            if (authenticationCallback != null) {
                authenticationCallback.onAuthenticationFailed();
            }
        }
    };

}
