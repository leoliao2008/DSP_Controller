package com.skycaster.serialporttest.bean;

import com.skycaster.serialporttest.obj.CommandType;

import java.util.Arrays;

/**
 * Created by 廖华凯 on 2017/3/8.
 */

public class SportRequest {
    private byte[] requestBody;

    public SportRequest(CommandType type) {
        requestBody=new byte[15];
        Arrays.fill(requestBody, (byte) 12);
        switch (type){
            case START_BIZ_SERVICE:
                requestBody[10]=0;
                break;
            case STOP_BIZ_SERVICE:
                requestBody[10]=1;
                break;
            case RESET_DSP_START:
                requestBody[10]=2;
                break;
            case SEND_BIZ_DATA:
                requestBody[10]=3;
                break;
            case READ_ERROR:
                requestBody[10]=99;
            default:
                break;
        }
    }

    public byte[] getBytes(){
        return requestBody;
    }

    public SportRequest(byte[] requestBody) {
        this.requestBody=new byte[15];
        System.arraycopy(requestBody,0,this.requestBody,0,15);
    }
}
