package com.skycaster.serialporttest.bean;

import com.skycaster.serialporttest.obj.CommandType;

import java.util.Arrays;


/**
 * Created by 廖华凯 on 2017/3/3.
 */

public class SportAck {

    private byte[] dataBody;

    public SportAck(CommandType type) {
        dataBody=new byte[256];
        Arrays.fill(dataBody,0,256, (byte) 0);//默认全部是0
        Arrays.fill(dataBody,0,10, (byte) 12);//帧头全部是12
        Arrays.fill(dataBody,246,256, (byte) 13);//帧尾全部是13
        dataBody[10]=1;//类型为应答
        dataBody[13]=1;//默认有效数据总包数1
        dataBody[14]=1;//默认有效数据当前包号1
        switch (type){
            case START_BIZ_SERVICE:
                dataBody[11]=0;
                break;
            case STOP_BIZ_SERVICE:
                dataBody[11]=1;
                break;
            case RESET_DSP_START:
                dataBody[11]=2;
                break;
            case RESET_DSP_COMPLETE:
                dataBody[11]=4;
                break;
            case SEND_BIZ_DATA:
                dataBody[11]=3;
                break;
            case SET_BAUD_RATE:
                dataBody[11]=5;
                break;
            case READ_ERROR:
                dataBody[11]=99;
            default:
                break;
        }
    }

    public SportAck(byte[] dataBody) {
        this.dataBody = new byte[256];
        System.arraycopy(dataBody,0,this.dataBody,0,256);
    }

    public byte[] getBytes(){
        return dataBody;
    }

    public void updateMessage(String msg){
        byte[] bytes = msg.trim().getBytes();
        int len=bytes.length;
        if(len>0){
            System.arraycopy(bytes,0,dataBody,17,len);
            dataBody[15]= (byte) len;
            dataBody[16]= (byte) (len>>8);
        }
    }



}
