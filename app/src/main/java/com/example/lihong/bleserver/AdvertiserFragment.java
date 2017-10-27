package com.example.lihong.bleserver;

import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

/**
 * Created by lihong on 2017/10/24.
 */

public class AdvertiserFragment extends Fragment implements OnClickListener {

    private Switch mSwitch;

    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        advertisingFailureReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int errorCode=intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE,-1);
                mSwitch.setChecked(false);
                String errorMessage="广告启动失败类型:";
                switch (errorCode){
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage+=" 已经开始";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage+=" 数据包太长";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage+=" 设备不支持广告";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage+=" 整形错误";
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage+=" 太多广告";
                        break;
                    case AdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage+=" 广告超时";
                        break;
                    default:
                        errorMessage+=" 未知错误";
                }
                Toast.makeText(getActivity(),errorMessage,Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.fragment_advertiser,container,false);

        mSwitch=(Switch)view.findViewById(R.id.advertise_switch);
        mSwitch.setOnClickListener(this);

        return view;
    }
    @Override
    public void onResume(){
        super.onResume();

        if(AdvertiserService.running){
            mSwitch.setChecked(true);
        }else{
            mSwitch.setChecked(false);
        }

        IntentFilter failureFilter=new IntentFilter(AdvertiserService.ADVERTISENG_FILED);
        getActivity().registerReceiver(advertisingFailureReceiver,failureFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        getActivity().unregisterReceiver(advertisingFailureReceiver);
    }



    private static Intent getServiceIntent(Context c){
        return new Intent(c,AdvertiserService.class);
    }

    @Override
    public void onClick(View v){
        boolean on=((Switch)v).isChecked();
        if(on){
            startAdvertising();
        }else {
            stopAdvertising();
        }
    }

    private void startAdvertising(){
        Context c=getActivity();
        c.startService(getServiceIntent(c));
    }

    private void stopAdvertising(){
        Context c=getActivity();
        c.startService(getServiceIntent(c));
        mSwitch.setChecked(false);
    }
}
