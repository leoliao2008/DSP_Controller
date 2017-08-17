package com.skycaster.serialporttest.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.skycaster.serialporttest.StaticData;
import com.skycaster.serialporttest.bases.SerialPortActivity;
import com.skycaster.serialporttest.bases.SerialPortApplication;
import com.skycaster.serialporttest.bean.SportAck;
import com.skycaster.serialporttest.bean.SportRequest;
import com.skycaster.serialporttest.obj.CommandType;
import com.skycaster.skc_cdradiorx.abstr.BusinessDataListener;
import com.skycaster.skc_cdradiorx.manager.DSPManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;

import static com.skycaster.serialporttest.obj.CommandType.READ_ERROR;
import static com.skycaster.serialporttest.obj.CommandType.RESET_DSP_START;
import static com.skycaster.serialporttest.obj.CommandType.SET_BAUD_RATE;
import static com.skycaster.serialporttest.obj.CommandType.START_BIZ_SERVICE;
import static com.skycaster.serialporttest.obj.CommandType.STOP_BIZ_SERVICE;
import static com.skycaster.serialporttest.obj.SerialPort.getOutputStream;

/**
 * Created by 廖华凯 on 2017/3/3.
 */

public class SerialDataExecutor {
    private static SerialDataExecutor serialDataExecutor;
    private static final String PROCPATH = "/proc/gpio_ctrl/rp_gpio_ctrl";
    private static SportAck ack;
    private double freq;
    private int leftTune;
    private int rightTune;
    private static boolean isTransmittingBizData;
    private static boolean isFirstRun=true;
    private static boolean isResetting;
    private static NumberFormat numberFormat;
    private static long resetRequestID;
    private static SerialPortActivity mSerialPortActivity;
    private int mBaudRate;


    public static synchronized SerialDataExecutor getInstance(SerialPortActivity serialPortActivity){
        if(serialDataExecutor==null){
            serialDataExecutor=new SerialDataExecutor();
            numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(2);
            numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        }
        if(mSerialPortActivity==null){
            mSerialPortActivity=serialPortActivity;
        }
        return serialDataExecutor;
    }

    public synchronized boolean executeRequest(Activity context, SportRequest request){
        boolean isSuccess=false;
        if(!isResetting){
            byte[] dataBody = request.getBytes();
            switch (dataBody[10]){
                case 0:
                    //启动业务
                    ack=new SportAck(START_BIZ_SERVICE);
                    if(!isTransmittingBizData){
                        double f;
                        int lt;
                        int rt;
                        if(dataBody[11]>=0){
                            f=dataBody[11]+dataBody[12]/100.f;
                        }else {
                            f=dataBody[11]-dataBody[12]/100.f;
                        }
                        f=Double.parseDouble(numberFormat.format(f));
                        lt = dataBody[13];
                        rt = dataBody[14];
                        showLog("frq: "+f);
                        showLog("lefttune: "+lt);
                        showLog("righttune:"+rt);
                        isSuccess = startBizService(context, f, lt, rt);

                        Intent intent=new Intent(StaticData.ACTION_CHECK_IF_BIZ_SERVICE_START_SUCCESS);
                        intent.putExtra(StaticData.EXTRA_IS_BIZ_SERVICE_START_SUCCESS,isSuccess);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    }else {
                        ack.updateMessage("Excessive Operation! DSP Service is already running.");
                    }
                    break;
                case 1:
                    //关闭业务
                    ack=new SportAck(STOP_BIZ_SERVICE);
                    isSuccess=stopBizService();
                    break;
                case 2:
                    //重新上电
                    ack=new SportAck(RESET_DSP_START);
                    resetRequestID=System.currentTimeMillis();
                    SixSecondsUtilReleaseForRetry(resetRequestID);
                    isSuccess=resetDspPower();
                    break;
                case 3:
                    //设置波特率
                    ack=new SportAck(SET_BAUD_RATE);
                    mBaudRate = 0;
                    for(int i = 0; i <4; i++) {
                        int shift= i * 8;
                        mBaudRate +=(dataBody[11+i] & 0x000000FF) << shift;//往高位游
                    }
//                    mBaudRate = mBaudRate |(dataBody[11]&0xffff);
//                    mBaudRate = mBaudRate |((dataBody[12]<<8)&0xffff);
//                    mBaudRate = mBaudRate |((dataBody[13]<<16)&0xffff);
//                    mBaudRate = mBaudRate |((dataBody[14]<<24)&0xffff);
                    showLog("new baud rate is"+ mBaudRate);
                    isSuccess=resetBaudRate(mBaudRate);
                    break;
                default:
                    //解析失败
                    ack=new SportAck(READ_ERROR);
                    ack.updateMessage("串口通讯数据丢失，请重新发送。");
                    break;
            }
        }else {
            ack=new SportAck(READ_ERROR);
            ack.updateMessage("DSP重启还未完成，操作无效。");
        }
        if(isSuccess){
            sendSuccessAck();
        }else {
            sendFailureAck();
        }
        return isSuccess;
    }

    private boolean resetBaudRate(int baudRate) {
        return mSerialPortActivity.resetBaudRate(baudRate);
    }

    private void SixSecondsUtilReleaseForRetry(final long requestId) {
        isResetting =true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(6000);
                if(isResetting&&resetRequestID==requestId){
                    isResetting=false;
                    SportAck temp=new SportAck(READ_ERROR);
                    temp.updateMessage("DSP重启超时，请重新尝试。。。");
                    sendAck(temp.getBytes());
                }
            }
        }).start();
    }

    //    帧头	    10帧类型	11命令码	 12状态码	13有效数据总包数	14有效数据当前包号	15当前数据包有效数据长度	17有效数据	     246帧尾
//     10 byte	    1byte	1 byte	 1byte	     1byte	             1byte	           2bytes	             229 bytes	    10 bytes
    private static synchronized void sendSuccessAck(){
        byte[] dataBody = ack.getBytes();
        dataBody[12]=0;
        sendAck(dataBody);
    }

    private synchronized void sendFailureAck(){
        byte[] dataBody = ack.getBytes();
        dataBody[12]=1;
        sendAck(dataBody);
    }


    private synchronized boolean startBizService(Activity context, final double f, final int lt, final int rt){
        showLog("new frq: "+f);
        showLog("new lefttune: "+lt);
        showLog("new righttune:"+rt);
        showLog("old frq: "+freq);
        showLog("old lefttune: "+leftTune);
        showLog("old righttune:"+rightTune);
        boolean result;
        if(isFirstRun){
            result=startBizWithNewParams(context,f,lt,rt);
            showLog("First time to set params");
        }else {
            if(freq==f&&leftTune==lt&&rightTune==rt){
                showLog("same params, start biz without changing params.");
                result= startBizWithOutParams(context);
            }else {
                showLog("different params, start biz with new params.");
                result= startBizWithNewParams(context,f,lt,rt);
            }
        }
        return result;
    }

    private synchronized boolean startBizWithNewParams(Activity context, double freq,int leftTune,int rightTune){
        boolean result=false;
        try {
            result=DSPManager.getDSPManager().apiOpenCDRadio(freq,leftTune,rightTune);
            if(!result){
                ack.updateMessage("Fail to init DSP setting...");
            }
        } catch (DSPManager.FreqOutOfRangeException e) {
            e.printStackTrace();
            ack.updateMessage("DSP Freq is out of range...");
        }
        if(result){
           result= startBizWithOutParams(context);
        }
        if(result){
            this.freq=freq;
            this.leftTune=leftTune;
            this.rightTune=rightTune;
            isFirstRun=false;
        }
        return result;
    }

    private synchronized boolean startBizWithOutParams(Activity context){
        boolean result;
        SystemClock.sleep(200);
        result=DSPManager.getDSPManager().apiGetService(context, 229, (byte) 33, new BusinessDataListener() {
            @Override
            public void preTask() {
                isTransmittingBizData=true;
                if(listener!=null){
                    listener.onBizServiceStart(freq,leftTune,rightTune);
                }
            }

            @Override
            public void onGetBizData(byte[] bytes) {
                if(listener!=null){
                    listener.onGetBizData(bytes);
                }
                sendBizDate(bytes);
            }

            @Override
            public void onServiceStop() {
                isTransmittingBizData=false;
                if(listener!=null){
                    listener.onBizServiceStop();
                }
            }
        });
        if(!result){
            ack.updateMessage("DSP setting is complete but fails to start service.");
        }
        return result;
    }

    private static synchronized void sendBizDate(byte[] bytes) {
        //    帧头	    10帧类型 	11命令码	 12状态码	13有效数据总包数	14有效数据当前包号	15当前数据包有效数据长度	17有效数据	     246帧尾
      //     10 byte	    1byte	1 byte	 1byte	     1byte	             1byte	           2bytes	             229 bytes	    10 bytes
//        byte[] dataBody = new SportAck(SEND_BIZ_DATA).getBytes();
//        short len= (short) bytes.length;
//        dataBody[10]=1;
//        dataBody[15]= (byte) len;
//        dataBody[16]= (byte) (len>>8);
//        System.arraycopy(bytes,0,dataBody,17,len);
//        try {
//            getOutputStream().write(dataBody);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        try {
            if(getOutputStream()!=null){
                getOutputStream().write(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean stopBizService(){
        boolean isSuccess=DSPManager.getDSPManager().apiStopService();
        showLog("isSuccess="+isSuccess);
        if(!isSuccess){
            ack.updateMessage("Stop Service fail, please check if it's already stopped.");
        }
        return isSuccess;
    }

    private synchronized boolean resetDspPower() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                cutOffDSP();
                SerialPortApplication.postDelay(new Runnable() {
                    @Override
                    public void run() {
                        reconnectDSP();
                    }
                },2000);
            }
        }).start();
        return true;
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

    private static DataExecuteListener listener;

    public abstract static class DataExecuteListener{
        public abstract void onBizServiceStart(double freq, int leftTune, int rightTune);
        public abstract void onGetBizData(byte[] bytes);
        public abstract void onBizServiceStop();
        public void onResetComplete(String msg){
            isResetting=false;
            isFirstRun=true;
            SportAck sportAck=new SportAck(CommandType.RESET_DSP_COMPLETE);
            sportAck.updateMessage(msg);
            try {
                getOutputStream().write(sportAck.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private SerialDataExecutor() {}

    public synchronized void setExecuteListener(DataExecuteListener lst){
        listener=lst;
    }

    private void showLog(String msg){
        Log.e(getClass().getName(),msg);
    }

    private static synchronized boolean sendAck(byte[] bytes){
        try {
            bytes[10]=1;
            getOutputStream().write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
