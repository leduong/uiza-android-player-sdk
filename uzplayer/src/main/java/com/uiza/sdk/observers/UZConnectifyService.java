package com.uiza.sdk.observers;


import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;

import com.uiza.sdk.events.EventBusData;
import com.uiza.sdk.utils.ConnectivityUtils;

import timber.log.Timber;

/**
 * Created by Loitp on 5/6/2017.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class UZConnectifyService extends JobService implements ConnectivityReceiver.ConnectivityReceiverListener {

    private ConnectivityReceiver mConnectivityReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mConnectivityReceiver = new ConnectivityReceiver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        unregisterReceiver(mConnectivityReceiver);
        return true;
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        Timber.d("onNetworkConnectionChanged isConnected: %b", isConnected);
        if (isConnected) {
            boolean isConnectedMobile = false;
            boolean isConnectedWifi = false;
            boolean isConnectedFast = false;
            if (ConnectivityUtils.isConnectedMobile(this)) {
                isConnectedMobile = true;
            }
            if (ConnectivityUtils.isConnectedWifi(this)) {
                isConnectedWifi = true;
            }
            if (ConnectivityUtils.isConnectedFast(this)) {
                isConnectedFast = true;
            }
            EventBusData.getInstance().sendConnectChange(true, isConnectedFast, isConnectedWifi, isConnectedMobile);
        } else {
            EventBusData.getInstance().sendConnectChange(false, false, false, false);
        }
    }
}