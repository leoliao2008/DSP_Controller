package com.skycaster.serialporttest.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.skycaster.serialporttest.R;
import com.skycaster.serialporttest.StaticData;
import com.skycaster.serialporttest.bases.BaseActivity;
import com.skycaster.serialporttest.bean.SportRequest;
import com.skycaster.serialporttest.obj.CommandType;
import com.skycaster.serialporttest.utils.SerialDataExecutor;
import com.skycaster.skc_cdradiorx.bases.CDRadioApplication;
import com.skycaster.skc_cdradiorx.beans.DSP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {


    private Handler handler=new Handler();
    private Button btn_startDSP;
    private Button btn_stopDSP;
    private ListView lv_console;
    private ArrayList<String> messages =new ArrayList<>();
    private ArrayAdapter<String>adapter;
    private Button btn_sendCommand;
    private Button btn_reconnectDSP;
    private Button btn_resetDSP;
    private static final String PROCPATH = "/proc/gpio_ctrl/rp_gpio_ctrl";
    private static final String DSP_CONNECTION_CHANGES ="com.skycaster.action.DSP_CONNECTION_CHANGES";
    private static final String IS_DSP_CONNECTED="IS_DSP_CONNECTED";
    private DSPConnectStatusUpdateReceiver receiver;
    private int startThresholdCount;
    private boolean isRequestConfirmed;
    byte[] request =new SportRequest(CommandType.READ_ERROR).getBytes();
    int mainLen;
    private boolean isPassAllFrameHead;
    private SerialDataExecutor serialDataExecutor;
    private SerialDataExecutor.DataExecuteListener dataExecuteListener;
    private Handler mHandler;
    private BizServicesStartSuccessReceiver mBizServicesStartSuccessReceiver;
    private Runnable mRunnableStartBizService=new Runnable() {
        @Override
        public void run() {
            startBizService();
        }
    };
    private Runnable mRunnableStopBizService=new Runnable() {
        @Override
        public void run() {
            stopBizService();
        }
    };


    @Override
    protected void onBaudRateChange(String msg) {
        updateConsole(msg);
    }

    @Override
    protected void onDataReceived(final byte[] buffer, final int size) {
//        showLog("current baud rate is"+mBaudRate);
//        for(int i=0;i<size;i++){
//            showLog("0x"+Integer.toHexString(buffer[i]));
//        }

        synchronized (this){
            for(int i=0;i<size;i++){
                byte b=buffer[i];
                if(!isRequestConfirmed){
                    if(b==12){
                        startThresholdCount++;
                        if(startThresholdCount==5){
                            isRequestConfirmed=true;
                            startThresholdCount=0;
                        }
                    }else {
                        startThresholdCount=0;
                    }
                }else {
                    if(b!=12&&!isPassAllFrameHead){
                        isPassAllFrameHead=true;
                    }
                    if(isPassAllFrameHead){
                        //加入缓存
                        request[10+mainLen]=b;
                        mainLen++;
                        if(mainLen ==5){
                            //如果缓存够了就复位各种判断参数，同时解析之前的缓存
                            isRequestConfirmed=false;
                            isPassAllFrameHead=false;
                            mainLen =0;
                            serialDataExecutor.executeRequest(MainActivity.this,new SportRequest(request));
                            //清空缓存，准备下一次
                            request =new SportRequest(CommandType.READ_ERROR).getBytes();
                        }
                    }
                }
            }
        }
    }




    private void updateConsole(final String msg){
        handler.post(new Runnable() {
            @Override
            public void run() {
                messages.add(msg);
                if(messages.size()>26){
                    messages.remove(0);
                }
                adapter.notifyDataSetChanged();
                lv_console.smoothScrollToPosition(messages.size()-1);
            }
        });
    }

    @Override
    protected void initListener() {

        btn_startDSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                帧头	帧类型	命令码	状态码	数据
//                0x0C	0x00	0x00	0x00	容器里面的16位数组
                startBizService();

            }
        });

        btn_stopDSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopBizService();
            }
        });

        btn_sendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                cutOffDSP();
                sendCommandToZhongHaiDa();
            }
        });

        btn_reconnectDSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reconnectDSP();
            }
        });

        btn_resetDSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetDSP();
            }
        });

        dataExecuteListener = new SerialDataExecutor.DataExecuteListener() {
            @Override
            public void onBizServiceStart(double freq, int leftTune, int rightTune) {
                updateConsole("启动业务数据成功！主频为： " + freq + "，左频为：" + leftTune + " ,右频为：" + rightTune + "。");
            }

            @Override
            public void onGetBizData(byte[] bytes) {
                updateConsole("Sending: " + new String(bytes));
                //8/17日修改，把数据发送回串口
                //// TODO: 2017/8/17  
//                try {
//                    mOutputStream.write(bytes);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

            }

            @Override
            public void onBizServiceStop() {
                updateConsole("业务数据停止了。");
            }


        };

        serialDataExecutor.setExecuteListener(dataExecuteListener);
    }

    private void sendCommandToZhongHaiDa() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        DSP dsp = CDRadioApplication.getDsp();
        if(dsp ==null||!dsp.isReadyToCommu()){
            showLog("dsp is not ready, reset dsp in 2 seconds");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetDSP();
                }
            },2000);
        }else {
            showLog("dsp is ready");
            mHandler.postDelayed(mRunnableStartBizService,2000);
        }

    }

    private void resetDSP(){
        onDataReceived(new SportRequest(CommandType.RESET_DSP_START).getBytes(),15);
    }


    @Override
    protected void onDestroy() {
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        //新增
        unRegisterReceiver();

        mHandler.post(mRunnableStopBizService);
        super.onDestroy();
    }

    private void startBizService(){
        byte[] bytes = new SportRequest(CommandType.START_BIZ_SERVICE).getBytes();
        int i1= Double.valueOf(91.2).intValue();
        int i2= (int) ((91.2-i1)*100);
        bytes[11]= (byte) i1;
        bytes[12]= (byte) i2;
        bytes[13]= (byte) 36;
        bytes[14]= (byte) 45;
        onDataReceived(bytes,15);

    }

    private void stopBizService(){
        onDataReceived(new SportRequest(CommandType.STOP_BIZ_SERVICE).getBytes(),15);
    }

    private void cutOffDSP(){
        try {
            writeLedFile(PROCPATH,"10".getBytes());
            showLog("DSP cut off complete!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reconnectDSP(){
        try {
            writeLedFile(PROCPATH,"9".getBytes());
            showLog("DSP reconnect complete!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLedFile(String path, byte[] buffer) throws IOException {
        File file = new File(path);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(buffer);
        fos.close();
    }

    @Override
    protected void initData() {
        mHandler=new Handler();
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, messages);
        lv_console.setAdapter(adapter);
        
        receiver=new DSPConnectStatusUpdateReceiver();
        IntentFilter intentFilter=new IntentFilter(DSP_CONNECTION_CHANGES);
        registerReceiver(receiver,intentFilter);
        
        //新增
        registerReceiver();
        serialDataExecutor=SerialDataExecutor.getInstance(this);
    }


    @Override
    protected void initView() {
        btn_startDSP= (Button) findViewById(R.id.main_btn_start_dsp);
        btn_stopDSP= (Button) findViewById(R.id.main_btn_stop_dsp);
        lv_console= (ListView) findViewById(R.id.main_lv_console);
        btn_sendCommand = (Button) findViewById(R.id.main_btn_send_command_to_zhong_hai_da);
        btn_reconnectDSP= (Button) findViewById(R.id.main_btn_reconnect_dsp);
        btn_resetDSP= (Button) findViewById(R.id.main_btn_reset_dsp);
    }

    @Override
    protected int setLayoutId() {
        return R.layout.activity_main;
    }



    private class DSPConnectStatusUpdateReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String IS_DSP_CONNECTED="IS_DSP_CONNECTED";
            boolean isConnectSuccess = intent.getBooleanExtra(IS_DSP_CONNECTED, false);
            if(isConnectSuccess){
                if(dataExecuteListener!=null){
                    dataExecuteListener.onResetComplete("DSP重启完成，可以接收新命令了。");
                }
                updateConsole("DSP重启完成，可以接收新命令了。");
                mHandler.postDelayed(mRunnableStartBizService,2000);
            }
        }
    }

    private void registerReceiver(){
        mBizServicesStartSuccessReceiver=new BizServicesStartSuccessReceiver();
        IntentFilter filter=new IntentFilter(StaticData.ACTION_CHECK_IF_BIZ_SERVICE_START_SUCCESS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBizServicesStartSuccessReceiver,filter);
    }

    private void unRegisterReceiver(){
        if(mBizServicesStartSuccessReceiver==null){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBizServicesStartSuccessReceiver);
            mBizServicesStartSuccessReceiver=null;
        }
    }

    private class BizServicesStartSuccessReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isStartSuccess = intent.getBooleanExtra(StaticData.EXTRA_IS_BIZ_SERVICE_START_SUCCESS, false);
            if(isStartSuccess){
                updateConsole("业务数据启动成功。");
                mHandler.removeCallbacks(mRunnableStartBizService);
                unRegisterReceiver();
            }else {
                updateConsole("业务数据启动失败，重新尝试启动。");
                mHandler.postDelayed(mRunnableStartBizService,2000);
            }

        }
    }




}
