package com.skycaster.serialporttest.bases;

import android.os.Bundle;

/**
 * Created by 廖华凯 on 2017/2/9.
 */

public abstract class BaseActivity extends SerialPortActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(setLayoutId());
        initView();
        initData();
        initListener();
        showLog("activity created");
    }

    protected abstract void initListener();

    protected abstract void initData();

    protected abstract void initView();

    protected abstract int setLayoutId();

}
