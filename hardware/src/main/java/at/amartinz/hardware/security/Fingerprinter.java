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
package at.amartinz.hardware.security;

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

import at.amartinz.hardware.Constants;

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
