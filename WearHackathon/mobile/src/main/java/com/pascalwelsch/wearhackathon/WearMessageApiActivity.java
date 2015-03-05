package com.pascalwelsch.wearhackathon;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class WearMessageApiActivity extends BaseActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener {

    private static final String TAG = WearMessageApiActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_api);
        findViewById(R.id.open_button).setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            Log.v(TAG, "connecting to the Google API");
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.open_button:
                sendHelloMessage();
                break;
            default:
                Toast.makeText(this, "view not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(final Bundle bundle) {
        Log.v(TAG, "Google Api connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(final ConnectionResult result) {
        Log.d(TAG, "Connect Failed");

        if (mResolvingError) {
            // currently resolving an error
            return;
        }

        if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, 1001);
            } catch (IntentSender.SendIntentException e) {
                // Error with resolution intent. Try again
                mGoogleApiClient.connect();
            }
        } else {
            // no resolution, display Error dialog
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 1001);
        }

    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "Connection suspended");
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        final String path = messageEvent.getPath();
        switch (path) {
            default:
                Log.v(TAG, "Message " + path + " > " + new String(messageEvent.getData()));
                break;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        Log.v(TAG, "found " + results.size() + " nodes");
        return results;
    }

    private void sendHelloMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (String nodeId : getNodes()) {
                    sendToDevice(nodeId, "/open", "open");
                }
            }
        }).start();
    }

    private void sendToDevice(final String nodeId, final String path, final String message) {
        final ResultCallback<MessageApi.SendMessageResult> resultCallback
                = new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(final MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.v(TAG, "sending message failed");
                    Toast.makeText(WearMessageApiActivity.this, "sending message failed",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.v(TAG, "message successfully sent");
                    Toast.makeText(WearMessageApiActivity.this, "sent",
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        Wearable.MessageApi
                .sendMessage(mGoogleApiClient, nodeId, path, message.getBytes())
                .setResultCallback(resultCallback, 10, TimeUnit.SECONDS);
    }
}
