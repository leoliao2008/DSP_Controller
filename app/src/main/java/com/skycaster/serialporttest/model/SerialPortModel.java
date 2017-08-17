package com.skycaster.serialporttest.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import project.SerialPort.SerialPort;

/**
 * Created by 廖华凯 on 2017/8/17.
 */

public class SerialPortModel {
    private SerialPort mSerialPort;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public SerialPortModel(String path,int baudRate) throws IOException,SecurityException {
        mSerialPort=new SerialPort(new File(path),baudRate,0);
        mInputStream=mSerialPort.getInputStream();
        mOutputStream=mSerialPort.getOutputStream();
    }

    public void writeData(byte[] data,int len) throws IOException,NullPointerException {
        mOutputStream.write(data,0,len);
    }

    public int readData(byte[] data) throws IOException,NullPointerException {
        return mInputStream.read(data);
    }

    public void onDestroy() throws IOException,NullPointerException {
        mOutputStream.close();
        mInputStream.close();
        mSerialPort.close();
        mOutputStream=null;
        mInputStream=null;
        mSerialPort=null;
    }
}
