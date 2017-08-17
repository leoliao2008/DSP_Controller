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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.skycaster.serialporttest.R;
import com.skycaster.serialporttest.obj.SerialPort;
import com.skycaster.skc_cdradiorx.bases.CDRadioActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import static com.skycaster.serialporttest.bases.SerialPortApplication.getSerialPort;


public abstract class SerialPortActivity extends CDRadioActivity {

	protected SerialPortApplication mApplication;
	protected SerialPort mSerialPort;
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	protected SharedPreferences mSharedPreferences;
	protected int mBaudRate;

	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while(!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[256];
					if (mInputStream == null) return;
					size = mInputStream.read(buffer);
					if (size > 0) {
						onDataReceived(buffer, size);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}

	private void DisplayError(int resourceId) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle("Error");
		b.setMessage(resourceId);
		b.setPositiveButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SerialPortActivity.this.finish();
			}
		});
		b.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApplication = (SerialPortApplication) getApplication();
		mSharedPreferences=getSharedPreferences("Config",MODE_PRIVATE);
//		mBaudRate=mSharedPreferences.getInt("baud_rate",19200);
//		mBaudRate=mSharedPreferences.getInt("baud_rate",115200);
//		openSerialPort(19200);
        openSerialPort(115200);
	}

	private boolean openSerialPort(int baudRate) {
		boolean isOpen=false;
		closeSerialPort();
		try {
			mSerialPort = getSerialPort(baudRate);
		} catch (SecurityException e) {
			DisplayError(R.string.error_security);
		} catch (IOException e) {
			DisplayError(R.string.error_unknown);
		} catch (InvalidParameterException e) {
			DisplayError(R.string.error_configuration);
		}
		if(mSerialPort!=null&&mSerialPort.getOutputStream()!=null&&mSerialPort.getInputStream()!=null){
			isOpen=true;
			mBaudRate=baudRate;
			runSerialPort();
		}
		return isOpen;
	}

	public void runSerialPort(){
		mOutputStream = mSerialPort.getOutputStream();
		mInputStream = mSerialPort.getInputStream();
			/* Create a receiving thread */
		mReadThread = new ReadThread();
		mReadThread.start();
	}

	public void closeSerialPort(){
		if (mReadThread != null)
			mReadThread.interrupt();
		if(mInputStream!=null){
			try {
				mInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mInputStream=null;
		}
		if(mOutputStream!=null){
			try {
				mOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mOutputStream=null;
		}
		mApplication.closeSerialPort();
		mSerialPort = null;
	}

	public boolean resetBaudRate(int newBaudRate){
		boolean isSuccess;
		if(newBaudRate!=mBaudRate){
			isSuccess=openSerialPort(newBaudRate);
			if(isSuccess){
				mSharedPreferences.edit().putInt("baud_rate",mBaudRate).apply();
				onBaudRateChange("波特率设置成功，新的波特率是"+mBaudRate);
			}
		}else {
			isSuccess=true;
            onBaudRateChange("当前波特率已经是"+mBaudRate+",不需要重新设置。");
		}
		return isSuccess;
	}

	protected abstract void onBaudRateChange(String notice);

	protected abstract void onDataReceived(final byte[] buffer, final int size);

	@Override
	protected void onDestroy() {
		closeSerialPort();
		super.onDestroy();
	}
}
