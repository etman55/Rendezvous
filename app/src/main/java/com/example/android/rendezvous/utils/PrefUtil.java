package com.example.android.rendezvous.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

/**
 * Created by Etman on 8/4/2017.
 */

public class PrefUtil {
    private final static String PREFERENCES_NAME = "com.example.android.rendezvous.utils.PREFERENCES";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static PrefUtil instance;

    private PrefUtil(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static PrefUtil with(Context context) {
        if (instance == null) {
            instance = new PrefUtil(context);
        }
        return instance;
    }

    public static void saveObjectToPref(Object object, Class<?> targetClass, String key) {
        String json = new Gson().toJson(object, targetClass);
        editor.putString(key, json).apply();
    }

    public static Object getObjectFromSharedPreferences(Class<?> targetClass, String key) {
        String json = sharedPreferences.getString(key, "");
        if (!json.isEmpty()) {
            return new Gson().fromJson(json, targetClass);
        }
        return null;
    }

    public static void saveStringToPref(String string, String key) {
        editor.putString(key, string).apply();
    }

    public static String getStringFromPref(String key) {
        return sharedPreferences.getString(key, "");
    }

    public static int getIntToPref() {
        return sharedPreferences.getInt("Radius", 0);
    }

    public static void saveIntToPref(int i) {
        editor.putInt("Radius", i).apply();
    }
}
