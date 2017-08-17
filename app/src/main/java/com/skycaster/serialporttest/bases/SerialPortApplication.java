/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.skycaster.serialporttest.bases;

import android.util.Log;

import com.skycaster.serialporttest.obj.SerialPort;
import com.skycaster.serialporttest.utils.SerialPortFinder;
import com.skycaster.skc_cdradiorx.bases.CDRadioApplication;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;


public class SerialPortApplication extends CDRadioApplication {

	public static SerialPortFinder mSerialPortFinder = new SerialPortFinder();
	private static SerialPort mSerialPort = null;
    private static android.os.Handler handler;


    @Override
    public void onCreate() {
        super.onCreate();
        handler=new android.os.Handler();
    }

    public static SerialPort getSerialPort(int baudRate) throws SecurityException, IOException, InvalidParameterException {
	if (mSerialPort == null) {
        String[] allDevicesPath = mSerialPortFinder.getAllDevicesPath();
		Log.e("serial counts :",allDevicesPath.length+"");
        for(String path:allDevicesPath){
            Log.e("serial port :",path);
        }
//		mSerialPort = new SerialPort(new File("/dev/ttymxc4"), 115200, 0);
//      mSerialPort = new SerialPort(new File("/dev/ttyAMA0"), 115200, 0);//荣品4418专用
        mSerialPort = new SerialPort(new File("/dev/ttyAMA0"), baudRate, 0);//荣品4418专用
//		mSerialPort = new SerialPort(new File("/dev/ttyAMA2"), 115200, 0);//友谊之臂4418专用
	}
	return mSerialPort;
    }

    public static void post(Runnable runnable){
        handler.post(runnable);
    }

    public static void postDelay(Runnable runnable,long millis){
        handler.postDelayed(runnable,millis);
    }

	public void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}
}
