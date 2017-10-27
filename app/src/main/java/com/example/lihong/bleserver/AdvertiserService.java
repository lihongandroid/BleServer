package com.example.lihong.bleserver;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by lihong on 2017/10/24.
 */

public class AdvertiserService extends Service {

    private static final String TAG="AdvertiserService";
    private static final int FOREGROUND_NOTIFICATION_ID=1;

    public static boolean running=false;
    public static final String ADVERTISENG_FILED="com.examle.lihong.bluetoothadvertisement.advertising_failed";
    public static final String ADVERTISING_FAILED_EXTRA_CODE="failureCode";
    public static final int ADVERTISING_TIMED_OUT=6;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdertiseCallback;
    private Handler mHandler;
    private Runnable timeoutRunnable;
    private long TIMEOUT= TimeUnit.MILLISECONDS.convert(10,TimeUnit.MINUTES);

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattCharacteristic characteristicRead;
    BluetoothManager mBluetoothManager;

    private static UUID UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    @Override
    public void onCreate(){
        running=true;
        initialize();
        startAdvertising();
        setTimeout();
        super.onCreate();

    }

    @Override
    public void onDestroy(){
        running=false;
        stopAdvertising();
        mHandler.removeCallbacks(timeoutRunnable);
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initialize(){
        if(mBluetoothLeAdvertiser==null){
           mBluetoothManager=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            if(mBluetoothManager!=null){
                BluetoothAdapter bluetoothAdapter=mBluetoothManager.getAdapter();
                if(bluetoothAdapter!=null){
                    mBluetoothLeAdvertiser=bluetoothAdapter.getBluetoothLeAdvertiser();
                }else{
                    Toast.makeText(this,"设备不支持蓝牙广播",Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this,"不支持蓝牙",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setTimeout(){
        mHandler=new Handler();
        timeoutRunnable=new Runnable(){
            @Override
            public void run(){
                Log.d(TAG,"广播服务已经运行"+TIMEOUT+"秒，停止停止广播");
                sendFailureIntent(ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable,TIMEOUT);
    }

    private void startAdvertising(){
        goForeground();
        Log.d(TAG,"服务开始广播");
        if(mAdertiseCallback==null){
            AdvertiseSettings settings=buildAdvertiseSettings();
            AdvertiseData data=buildAdvertiseData();
            mAdertiseCallback=new SampleAdvertiseCallback();

            if(mBluetoothLeAdvertiser!=null){
                mBluetoothLeAdvertiser.startAdvertising(settings,data,mAdertiseCallback);
            }
        }
    }

    private void goForeground(){
        Log.d(TAG,"goForegroud运行过了");
       //NotificationManager manager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,notificationIntent,0);
        Notification notification=new NotificationCompat.Builder(this)
                .setContentTitle("通过蓝牙广播该设备")
                .setContentText("该设备会被附近的其他设备发现")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(FOREGROUND_NOTIFICATION_ID,notification);
      //  manager.notify(FOREGROUND_NOTIFICATION_ID,notification);
    }

    private void stopAdvertising(){
        Log.d(TAG,"服务停止广播");
        if(mBluetoothLeAdvertiser!=null){
            mBluetoothLeAdvertiser.stopAdvertising(mAdertiseCallback);
            mAdertiseCallback=null;
        }
    }

    private AdvertiseData buildAdvertiseData(){
        AdvertiseData.Builder dataBuilder=new AdvertiseData.Builder();
       // dataBuilder.addServiceUuid(Constants.Service_UUID);
        dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }

    private AdvertiseSettings buildAdvertiseSettings(){
        AdvertiseSettings.Builder settingsBuilder=new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);

        return settingsBuilder.build();
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback{
        @Override
        public void onStartFailure(int errorCode){
            super.onStartFailure(errorCode);

            Log.d(TAG,"广播失败");
            sendFailureIntent(errorCode);
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect){
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG,"服务端的广播成功开启");
            Log.d(TAG,"BLE服务的广播启动成功后：TxPowerLv="+settingsInEffect.getTxPowerLevel()+"；mode="+settingsInEffect.getMode()+"；timeout="+settingsInEffect.getTimeout());
            initServices(getContext());//该方法是添加一个服务，在此处调用即将服务广播出去
        }
    }

    private void sendFailureIntent(int errorCode){
        Intent failureIntent=new Intent();
        failureIntent.setAction(ADVERTISENG_FILED);
        failureIntent.putExtra(ADVERTISING_FAILED_EXTRA_CODE,errorCode);
        sendBroadcast(failureIntent);
    }

    //添加一个服务，该服务有一个读特征、该特征有一个描述；一个写特征。
    //用BluetoothGattServer添加服务，并实现该类的回调接口
    private void initServices(Context context){
        mBluetoothGattServer=mBluetoothManager.openGattServer(context,bluetoothGattServerCallback);
        BluetoothGattService service=new BluetoothGattService(UUID_SERVER,BluetoothGattService.SERVICE_TYPE_PRIMARY);

        characteristicRead=new BluetoothGattCharacteristic(UUID_CHARREAD,BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor descriptor=new BluetoothGattDescriptor(UUID_DESCRIPTOR,BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicRead.addDescriptor(descriptor);
        service.addCharacteristic(characteristicRead);

        BluetoothGattCharacteristic characteristicWrite=new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE |BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);

        mBluetoothGattServer.addService(service);
        Log.d(TAG,"初始化服务成功：initServices ok");
    }
    //服务事件的回调
    private BluetoothGattServerCallback bluetoothGattServerCallback=new BluetoothGattServerCallback() {
        //1、首先是连接状态的回调
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG,"连接状态发生改变，安卓系统回调onConnectionStateChange:device name="+device.getName()+"address="+device.getAddress()+"status="+status+"newstate="+newState);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG,"客户端有读的请求，安卓系统回调该onCharacteristicReadRequest()方法");

            mBluetoothGattServer.sendResponse(device,requestId, BluetoothGatt.GATT_SUCCESS,offset,characteristic.getValue());
        }

        //接受具体字节，当有特征被写入时，回调该方法，写入的数据为参数中的value
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG,"客户端有写的请求，安卓系统回调该onCharacteristicWriteRequest()方法");

            //特征被读取，在该回调方法中回复客户端响应成功
            mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,value);

            //处理响应内容
            //value:客户端发送过来的数据
            onResponseToClient(value,device,requestId,characteristic);
        }

        //特征被读取。当回复相应成功后，客户端胡读取然后触发本方法
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,null);
        }

        //2、其次，当有描述请求被写入时，回调该方法，
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,value);
           // onResponseToClient(value,device,requestId,descriptor.getCharacteristic());
        }

        @Override
        public void onServiceAdded(int status,BluetoothGattService service){
            super.onServiceAdded(status,service);
            Log.e(TAG,"添加服务成功，安卓系统回调该onServiceAdded()方法");
        }
    };

    //4.处理相应内容,requestBytes是客户端发送过来的数据
    private void onResponseToClient(byte[] requestBytes,BluetoothDevice device,int requestId,BluetoothGattCharacteristic characteristic){
        //在服务端接受数据
        String msg=OutputStringUtil.transferForPrint(requestBytes);
        Log.e(TAG,"收到："+msg);
        //响应客户端
        String str=new String(requestBytes)+"hello>";
        characteristicRead.setValue(str.getBytes());
        mBluetoothGattServer.notifyCharacteristicChanged(device,characteristicRead,false);//用该方法通知characteristicRead数据发生改变
        }

    private Context getContext(){
        return this;
    }
}





