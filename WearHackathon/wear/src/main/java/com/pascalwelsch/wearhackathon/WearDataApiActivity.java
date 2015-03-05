package com.pascalwelsch.wearhackathon;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;


public class WearDataApiActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, DataApi.DataListener {

    public static final String PATH_COUNT = "/count";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    private static final String TAG = "WearDataApiActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    private static final String COUNT = "shared_count";

    int mCount = 0;

    private TextView mCountLabel;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    private Button mIncreaseButton;

    /**
     * Determines if the client is in a resolution state, and waiting for resolution intent to
     * return.
     */
    private boolean mIsInResolution;

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_api);
        mIncreaseButton = (Button) findViewById(R.id.increase_count);
        mIncreaseButton.setOnClickListener(this);
        mCountLabel = (TextView) findViewById(R.id.count);

        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Called when the Activity is made visible. A connection to Play Services need to be initiated
     * as soon as the activity is visible. Registers {@code ConnectionCallbacks} and {@code
     * OnConnectionFailedListener} on the activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to be disconnected as
     * soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.increase_count:
                increaseCount();
                break;
            default:
                // not implemented
                break;
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.

        Wearable.DataApi.addListener(mGoogleApiClient, this);
        requestCurrentCount();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed. Handle {@code
     * result.getResolution()} if there is a resolution available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        mIncreaseButton.setEnabled(false);
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEvents) {
        Log.v(TAG, "data changed");
        for (DataEvent dataEvent : dataEvents) {
            switch (dataEvent.getType()) {
                case DataEvent.TYPE_CHANGED:
                    final DataItem item = dataEvent.getDataItem();
                    onDataItemChanged(item);
                    break;
                case DataEvent.TYPE_DELETED:
                    //TODO
                    break;
                default:
                    // not implemented
                    break;
            }
        }
    }

    private void increaseCount() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH_COUNT);
        putDataMapReq.getDataMap().putInt(COUNT, ++mCount);
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapReq.asPutDataRequest());
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.v(TAG, "success: put count " + mCount);
                } else {
                    Log.w(TAG, "unable to put count");
                    Toast.makeText(WearDataApiActivity.this, "unable to put", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }, 10, TimeUnit.SECONDS);
        Log.v(TAG, "send count: " + mCount);
    }

    private void onDataItemChanged(@NonNull final DataItem item) {
        final String path = item.getUri().getPath();
        switch (path) {
            case PATH_COUNT:
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                final int count = dataMap.getInt(COUNT);
                Log.v(TAG, "new count: " + count);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCounter(count);
                    }
                });
                break;
            default:
                // not implemented
                break;
        }
    }

    private void requestCurrentCount() {
        final PendingResult<DataItemBuffer> pendingResult = Wearable.DataApi
                .getDataItems(mGoogleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(final DataItemBuffer dataItems) {
                for (DataItem dataItem : dataItems) {
                    onDataItemChanged(dataItem);
                }
                dataItems.release();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mIncreaseButton.setEnabled(true);
                    }
                });
            }
        }, 10, TimeUnit.SECONDS);
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    private void updateCounter(final int count) {
        mCount = count;
        mCountLabel.setText("count: " + count);
    }
}
