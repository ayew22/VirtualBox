package com.fun.vbox.client.natives;

import android.annotation.SuppressLint;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Parcelable;

import com.fun.vbox.client.hook.utils.MethodParameterUtils;
import com.fun.vbox.helper.compat.BuildCompat;

import java.lang.reflect.Method;

import dalvik.system.DexFile;

/**
 * @author Lody
 */
public class NativeMethods {

    public static int gCameraMethodType;

    public static Method gCameraNativeSetup;

    public static Method gOpenDexFileNative;

    public static Method gAudioRecordNativeCheckPermission;

    //ActivityThread.currentOpPackageName
    public static Method gMediaRecorderNativeSetup;

    public static Method gAudioRecordNativeSetup;

    public static int gAudioRecordMethodType;

    static {
        try {
            init();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("PrivateApi")
    private static void init() {

        gMediaRecorderNativeSetup = getMediaRecorderNativeSetup();

        gAudioRecordNativeSetup = getAudioRecordNativeSetup();

        if (gAudioRecordNativeSetup != null && gAudioRecordNativeSetup.getParameterTypes().length == 10) {
            gAudioRecordMethodType = 2;
        } else {
            gAudioRecordMethodType = 1;
        }

        String methodName =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? "openDexFileNative" : "openDexFile";
        for (Method method : DexFile.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                gOpenDexFileNative = method;
                break;
            }
        }
        if (gOpenDexFileNative == null) {
            throw new RuntimeException("Unable to find method : " + methodName);
        }
        gOpenDexFileNative.setAccessible(true);

        gCameraMethodType = -1;
        Method method = getCameraNativeSetup();
        if (method != null) {
            int index = MethodParameterUtils.getParamsIndex(
                    method.getParameterTypes(), String.class);
            gCameraNativeSetup = method;
            gCameraMethodType = 0x10 + index;
        }

        for (Method mth : AudioRecord.class.getDeclaredMethods()) {
            if (mth.getName().equals("native_check_permission")
                    && mth.getParameterTypes().length == 1
                    && mth.getParameterTypes()[0] == String.class) {
                gAudioRecordNativeCheckPermission = mth;
                mth.setAccessible(true);
                break;
            }
        }
    }

    private static Method getCameraNativeSetup() {
        Method[] methods = Camera.class.getDeclaredMethods();
        if (methods != null) {
            for (Method method : methods) {
                if ("native_setup".equals(method.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

    @SuppressLint("PrivateApi")
    private static Method getMediaRecorderNativeSetup(){
        Method native_setup = null;
        try {
            if (BuildCompat.isS()) {
                native_setup = MediaRecorder.class.getDeclaredMethod("native_setup", Object.class, String.class, Parcelable.class);
            } else {
                native_setup = MediaRecorder.class.getDeclaredMethod("native_setup",  Object.class, String.class, String.class );
            }
        } catch (NoSuchMethodException e) {
            //ignore
        }
        if (native_setup == null) {
            try {
                native_setup = MediaRecorder.class.getDeclaredMethod("native_setup",
                        Object.class, String.class);
            } catch (NoSuchMethodException e) {
                //ignore
            }
        }
        return native_setup;
    }

    @SuppressLint("PrivateApi")
    private static Method getAudioRecordNativeSetup() {
        Method native_setup = null;
        /**
         * Object audiorecord_this,
         Object  attributes,
        int[] sampleRate, int channelMask, int channelIndexMask, int audioFormat,
        int buffSizeInBytes, int[] sessionId, String opPackageName,
        long nativeRecordInJavaObj
         */
        try {
            native_setup = AudioRecord.class.getDeclaredMethod("native_setup",
                    Object.class, Object.class, int[].class, int.class, int.class, int.class,
                    int.class, int[].class, String.class, long.class);
        } catch (NoSuchMethodException e) {
            //ignore
        }
        /**
         * Object audiorecord_this,
         Object attributes,
        int sampleRate, int channelMask, int channelIndexMask, int audioFormat,
        int buffSizeInBytes, int[] sessionId, String opPackageName
         */
        if (native_setup == null) {
            try {
                native_setup = AudioRecord.class.getDeclaredMethod("native_setup",
                        Object.class, Object.class, int.class, int.class, int.class, int.class,
                        int.class, int[].class, String.class);
            } catch (NoSuchMethodException e) {
                //ignore
            }
        }
        return native_setup;
    }

}
