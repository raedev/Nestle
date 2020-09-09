package com.github.raedev.sdkhotfix.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.raedev.sdkhotfix.sdk.DemoSdk;
import com.github.raedev.sdkhotfix.sdk.IDemoTestApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Nestle.MainActivity";
    TextView mResultView;

    IDemoTestApi mDemoTestApi;

    public static class Test {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            DemoSdk instance = (DemoSdk) getClassLoader().loadClass(DemoSdk.class.getName()).newInstance();
            String hello = instance.getDemoTestApi().hello();
            Log.i(TAG, "instance: " + hello);
        } catch (Throwable e) {
            e.printStackTrace();
        }


        Log.i("Nestle", "Activity loader: " + MainActivity.class.getClassLoader());
        Log.i("Nestle", "Demo Sdk class loader: " + DemoSdk.class.getClassLoader());
        mDemoTestApi = new DemoSdk(getApplication()).getDemoTestApi();

        findViewById(R.id.btn_demo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onRunDemo();
            }
        });

        findViewById(R.id.btn_patch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onApplyPatch();
            }
        });

        mResultView = findViewById(R.id.tv_result);
        this.onRunDemo();
    }


    private void onRunDemo() {
        showResult("Default");
    }

    private void onApplyPatch() {
//        showResult("Patch");
        String dataDir = getApplicationInfo().dataDir;
        String dexFile = dataDir + "/nestle/nestle_secondary_dexes/nestle.dex";
        String optDir = dataDir + "/nestle/odex";
        DexClassLoader dexClassLoader = new DexClassLoader(dexFile, optDir, null, null);
        Log.i(TAG, "class loader:" + dexClassLoader);
        try {
            Class<?> aClass = dexClassLoader.loadClass(DemoSdk.class.getName());
            Object instance = aClass.getConstructor().newInstance();
            Method method = aClass.getDeclaredMethod("getDemoTestApi");
            Object api = method.invoke(instance);
            Object string = api.getClass().getDeclaredMethod("hello").invoke(api);
            Log.i(TAG, "patch api :" + string);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void showResult(String tag) {
        String text = mDemoTestApi.hello();
        String date = SimpleDateFormat.getTimeInstance().format(new Date());
        mResultView.setText(String.format("[%s][%s] %s", date, tag, text));
    }
}