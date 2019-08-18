package com.itaisapir.yum;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.itaisapir.yum.utils.InnerIds;
import com.itaisapir.yum.utils.Utils;

public class SplashActivity extends AppCompatActivity {
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        ImageView splashImage;
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals(InnerIds.SPLASH_END_CODE)) {
                    finish();
                    unregisterReceiver(broadcastReceiver);
                }
                if (action.equals(InnerIds.SPLASH_LONG_TIME)) {
                    Toast toast = Utils.getInstance().createBigToast(getBaseContext(), getResources().getString(R.string.splash_long_time), Toast.LENGTH_LONG);
                    if(toast != null)
                        toast.show();
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(InnerIds.SPLASH_END_CODE));
        registerReceiver(broadcastReceiver, new IntentFilter(InnerIds.SPLASH_LONG_TIME));

        splashImage = findViewById(R.id.splashImage);
        Glide.with(getBaseContext()).asGif().load(R.drawable.loader_splash_trans).into(splashImage);
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}
