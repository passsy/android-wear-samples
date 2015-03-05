package com.pascalwelsch.wearhackathon;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * Created by pascalwelsch on 3/4/15.
 */
public class WearMessageApiService extends WearableListenerService {

    private static final String TAG = WearMessageApiService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        final ConnectionResult connectionResult = mGoogleApiClient
                .blockingConnect(10, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess()) {
            Log.v(TAG, "couldn't connect");
            return;
        }

        final String path = messageEvent.getPath();
        switch (path) {
            case "/hello":
                Toast.makeText(this, new String(messageEvent.getData()), Toast.LENGTH_SHORT).show();
                break;
            default:
                Log.v(TAG, "message for path " + path + " not handled");
                break;
        }
        mGoogleApiClient.disconnect();
    }



}
