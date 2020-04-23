/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.synconset;
import android.Manifest;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static android.Manifest.permission.READ_CALENDAR;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class ImagePicker extends CordovaPlugin {

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";
    Intent imagePickerIntent;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private CallbackContext callbackContext;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            if (hasReadPermission()){
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true )); }
            else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, false));
            }

            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
            final Intent imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);
            int max = 20;
            int desiredWidth = 0;
            int desiredHeight = 0;
            int quality = 100;
            int outputType = 0;
            if (params.has("maximumImagesCount")) {
                max = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                outputType = params.getInt("outputType");
            }

            imagePickerIntent.putExtra("MAX_IMAGES", max);
            imagePickerIntent.putExtra("WIDTH", desiredWidth);
            imagePickerIntent.putExtra("HEIGHT", desiredHeight);
            imagePickerIntent.putExtra("QUALITY", quality);
            imagePickerIntent.putExtra("OUTPUT_TYPE", outputType);
            this.imagePickerIntent = imagePickerIntent;
            // some day, when everybody uses a cordova version supporting 'hasPermission', enable this:

            if (cordova != null) {
                 if (cordova.hasPermission(READ_EXTERNAL_STORAGE)) {
                    cordova.startActivityForResult(this, imagePickerIntent, 0);
                 } else {
                     SharedPreferences sp = cordova.getContext().getSharedPreferences("ImagePicker", Context.MODE_PRIVATE);
                     // 不是第一次请求权限直接返回
                     if (sp.getString("request","") == "true") {
                         this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"show setting"));
                     } else {
                         SharedPreferences.Editor editor = sp.edit();
                         editor.putString("request", "true");
                         editor.commit();
                         Log.d("ImagePicker", "Requesting permissions for READ_EXTERNAL_STORAGE");
                         PermissionHelper.requestPermission(this, PERMISSION_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
                     }

                 }
             }

            // .. until then use:
            // if (hasReadPermission()) {
            //     cordova.startActivityForResult(this, imagePickerIntent, 0);
            // } else {
            //     requestReadPermission();
            //     // The downside is the user needs to re-invoke this picker method.
            //     // The best thing to do for the dev is check 'hasReadPermission' manually and
            //     // run 'requestReadPermission' or 'getPictures' based on the outcome.
            // }
            return true;
        }
        return false;
    }

    @SuppressLint("InlinedApi")
    private boolean hasReadPermission() {
//        return Build.VERSION.SDK_INT < 23 ||
//            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this.cordova.getActivity(), READ_EXTERNAL_STORAGE);

        if (PermissionHelper.hasPermission(this, READ_EXTERNAL_STORAGE)) {
            Log.d("ImagePicker", "Permissions already granted, or Android version is lower than 6");
            return true;
        } else {
            return false;
        }
    }

    @SuppressLint("InlinedApi")
    private void requestReadPermission() {
//        if (!hasReadPermission()) {
//            ActivityCompat.requestPermissions(
//                this.cordova.getActivity(),
//                new String[] {READ_EXTERNAL_STORAGE},
//                PERMISSION_REQUEST_CODE);
//        }
        // This method executes async and we seem to have no known way to receive the result
        // (that's why these methods were later added to Cordova), so simply returning ok now.

        if (PermissionHelper.hasPermission(this, READ_EXTERNAL_STORAGE)) {
            Log.d("ImagePicker", "Permissions already granted, or Android version is lower than 6");
            callbackContext.success();
        } else {
            SharedPreferences sp = cordova.getContext().getSharedPreferences("ImagePicker", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("request", "true");
            editor.commit();
            if (sp.getString("request","") == "true") {

            }
            Log.d("ImagePicker", "Requesting permissions for READ_EXTERNAL_STORAGE");
            PermissionHelper.requestPermission(this, PERMISSION_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            int sync = data.getIntExtra("bigdata:synccode", -1);
            final Bundle bigData = ResultIPC.get().getLargeData(sync);
      
            ArrayList<String> fileNames = bigData.getStringArrayList("MULTIPLEFILENAMES");
    
            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            callbackContext.error(error);

        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("No images selected");
        }
    }

    /**
     * Choosing a picture launches another Activity, so we need to implement the
     * save/restore APIs to handle the case where the CordovaActivity is killed by the OS
     * before we get the launched Activity's result.
     *
     *
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }


    public void onRequestPermissionResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) throws JSONException {

        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                Log.d("ImagePicker", "Permission not granted by the user");
                // 复选了不在询问
                if (!ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(), String.valueOf(r))){
                    // callbackContext.error("show setting");
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "复选了不在询问 -不准确的"));

                } else {
                    // Tell the JS layer that something went wrong...
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "some error"));
                    // callbackContext.error("some error");
                }
                return;
            }
        }


        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                Log.d("ImagePicker", "User granted the permission for READ_EXTERNAL_STORAGE");
                cordova.startActivityForResult(this, this.imagePickerIntent, 0);

                break;
        }
    }
}
