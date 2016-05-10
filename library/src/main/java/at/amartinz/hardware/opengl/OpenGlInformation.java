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

package at.amartinz.hardware.opengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import at.amartinz.hardware.Constants;
import alexander.martinz.libs.hardware.R;

import static android.opengl.GLES20.GL_EXTENSIONS;
import static android.opengl.GLES20.GL_RENDERER;
import static android.opengl.GLES20.GL_SHADING_LANGUAGE_VERSION;
import static android.opengl.GLES20.GL_VENDOR;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.glGetString;

/**
 * Created by alex on 25.08.15.
 */
public class OpenGlInformation {
    private static final String TAG = OpenGlInformation.class.getSimpleName();

    public static final int[] GL_INFO = new int[]{
            GL_VENDOR,                  // gpu vendor
            GL_RENDERER,                // gpu renderer
            GL_VERSION,                 // opengl version
            GL_EXTENSIONS,              // opengl extensions
            GL_SHADING_LANGUAGE_VERSION // shader language version
    };

    public static final int[] GL_STRINGS = new int[]{
            R.string.hardware_gpu_vendor,        // gpu vendor
            R.string.hardware_gpu_renderer,      // gpu renderer
            R.string.hardware_opengl_version,    // opengl version
            R.string.hardware_opengl_extensions, // opengl extensions
            R.string.hardware_shader_version     // shader language version
    };

    public static boolean isOpenGLES20Supported(@NonNull final Context context) {
        final ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo info = am.getDeviceConfigurationInfo();
        if (info == null) {
            // we could not get the configuration information, let's return false
            return false;
        }
        final int glEsVersion = ((info.reqGlEsVersion & 0xffff0000) >> 16);
        if (Constants.DEBUG) {
            Log.v(TAG, String.format("glEsVersion: %s (%s)", glEsVersion, info.getGlEsVersion()));
        }
        return (glEsVersion >= 2);
    }

    @NonNull public static ArrayList<String> getOpenGLESInformation() {
        final ArrayList<String> glesInformation = new ArrayList<>(GL_INFO.length);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // get a hold of the display and initialize
            final EGLDisplay dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            final int[] vers = new int[2];
            EGL14.eglInitialize(dpy, vers, 0, vers, 1);

            // find a suitable OpenGL config. since we do not render, we are not that strict about the exact attributes
            final int[] configAttr = {
                    EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                    EGL14.EGL_LEVEL, 0,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
            };
            final EGLConfig[] configs = new EGLConfig[1];
            final int[] numConfig = new int[1];
            EGL14.eglChooseConfig(dpy, configAttr, 0, configs, 0, 1, numConfig, 0);
            if (numConfig[0] == 0) {
                if (Constants.DEBUG) {
                    Log.w("getOpenGLESInformation", "no config found! PANIC!");
                }
            }
            final EGLConfig config = configs[0];

            // we need a surface for our context, even if we do not render anything so let's create a little offset surface
            final int[] surfAttr = {
                    EGL14.EGL_WIDTH, 64,
                    EGL14.EGL_HEIGHT, 64,
                    EGL14.EGL_NONE
            };
            final EGLSurface surf = EGL14.eglCreatePbufferSurface(dpy, config, surfAttr, 0);

            // finally let's create our context
            final int[] ctxAttrib = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE };
            final EGLContext ctx = EGL14.eglCreateContext(dpy, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);

            // set up everything, make the context our current context
            EGL14.eglMakeCurrent(dpy, surf, surf, ctx);

            // get the information we desire
            for (final int glInfo : GL_INFO) {
                try {
                    final String infoString = glGetString(glInfo);
                    if (!TextUtils.isEmpty(infoString)) {
                        glesInformation.add(infoString);
                    }
                } catch (Exception ignored) { }
            }

            // free and destroy everything
            EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(dpy, surf);
            EGL14.eglDestroyContext(dpy, ctx);
            EGL14.eglTerminate(dpy);
        } else {
            // ... no comment
            for (final int glInfo : GL_INFO) {
                try {
                    final String infoString = glGetString(glInfo);
                    if (!TextUtils.isEmpty(infoString)) {
                        glesInformation.add(infoString);
                    }
                } catch (Exception ignored) { }
            }
        }

        return glesInformation;
    }
}
