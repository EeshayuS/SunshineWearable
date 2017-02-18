package com.example.android.sunshine;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

import static com.example.android.sunshine.utilities.SunshineWeatherUtils.formatTemperature;
import static com.example.android.sunshine.utilities.SunshineWeatherUtils.getSmallArtResourceIdForWeatherCondition;


public class WearableDataSender implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private static WearableDataSender dataSender;

    private static final String DATA_PATH = "/data";
    private static final String MAX_TEMP = "max";
    private static final String MIN_TEMP = "min";
    private static final String WEATHER_ID = "weather_id";
    private static final String ICON_RESOURCE_ID = "icon_resource_id";
    private static final String WEATHER_ICON = "weather_icon";

    private WearableDataSender() {}

    public static synchronized WearableDataSender getInstance(){
        if(dataSender == null)
            dataSender = new WearableDataSender();
        return dataSender;
    }

    public void initialise(Context context){
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    public void sendData(Context context, ContentValues[] contentValues){
        ContentValues values = contentValues[0];

        String maxTemp = formatTemperature(context, values.getAsDouble(MAX_TEMP)).trim();
        String minTemp = formatTemperature(context, values.getAsDouble(MIN_TEMP)).trim();
        int weatherId = values.getAsInteger(WEATHER_ID);
        int iconResourceId = getSmallArtResourceIdForWeatherCondition(weatherId);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(DATA_PATH);
        putDataMapRequest.getDataMap().putString(MAX_TEMP, maxTemp);
        putDataMapRequest.getDataMap().putString(MIN_TEMP, minTemp);
        putDataMapRequest.getDataMap().putInt(ICON_RESOURCE_ID, iconResourceId);

        if(iconResourceId != -1){
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), iconResourceId);
            Asset icon = createAssetFromBitmap(bitmap);
            putDataMapRequest.getDataMap().putAsset(WEATHER_ICON, icon);
        }

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest().setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.e("Phone", "Data sent: " + dataItemResult.getStatus()
                                .isSuccess());
                    }

                });

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}