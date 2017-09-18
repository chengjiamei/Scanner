package com.isport.isportlibrary.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by  Carmy Cheng on 2017/8/23.
 *
 */

public class ScanManager implements BleScanCallback.OnScanCallback, BleLeScanCallback.OnLeScanCallback {

    /**
     * Fails to start scan as Ble scan with the  same settings is already started by the app
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;
    /**
     * Fails to start scan sa app cannot be regstered
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;
    /**
     * Fails to start scan due an internal error
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;
    /**
     * Fails to start power optimized scan as this feature is not supported
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;


    public static ScanManager sInstance;
    private Context mContext;
    private BleLeScanCallback bleLeScanCallback;
    private BleScanCallback bleScanCallback;
    private static ScanHandler scanHandler;
    private long scanTime = 10000;
    private boolean isScaning = false;
    private OnScanManagerListener mScanListener;
    private ScanSettings scanSettings;

    private ScanManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            settingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            }
            settingsBuilder.setReportDelay(1000);
            scanSettings = settingsBuilder.build();
        }
        mContext = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bleScanCallback = new BleScanCallback();
            bleScanCallback.setOnScanCallback(this);
        } else {
            bleLeScanCallback = new BleLeScanCallback();
            bleLeScanCallback.setScanCallback(this);
        }
        scanHandler = new ScanHandler(this);
    }

    public static ScanManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (ScanManager.class) {
                sInstance = new ScanManager(context.getApplicationContext());
            }
        }
        return sInstance;
    }

    public void setScanTime(long scanTime) {
        this.scanTime = scanTime;
    }

    public void setScanListener(OnScanManagerListener listener) {
        this.mScanListener = listener;
    }

    public void setScanSettings(ScanSettings scanSettings) {
        this.scanSettings = scanSettings;
    }

    /**
     * @return start le scan or not
     */
    public boolean startLeScan() {
        BluetoothAdapter bleAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            return false;
        }
        if (isScaning) {
            isScaning = false;
            cancelLeScan();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner scanner = bleAdapter.getBluetoothLeScanner();
            scanner.startScan(null, scanSettings, bleScanCallback);
        } else {
            bleAdapter.startLeScan(bleLeScanCallback);
        }

        isScaning = true;
        scanHandler.sendEmptyMessageDelayed(0x01, scanTime);
        return true;
    }

    public boolean cancelLeScan() {
        BluetoothAdapter bleAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bleAdapter == null || !bleAdapter.isEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner scanner = bleAdapter.getBluetoothLeScanner();
            scanner.stopScan(bleScanCallback);
        } else {
            bleAdapter.stopLeScan(bleLeScanCallback);
        }
        if (scanHandler.hasMessages(0x01)) {
            scanHandler.removeMessages(0x01);
        }
        if (isScaning) {
            if (mScanListener != null) {
                mScanListener.onScanFinished();
            }
        }
        isScaning = false;
        return true;
    }

    /**
     * called on version that below{@link android.os.Build.VERSION_CODES#LOLLIPOP}
     *
     * @param result scan result
     */
    @Override
    public void onScanResult(ScanResult result) {
        if (mScanListener != null) {
            List<ScanResult> list = new ArrayList<>();
            list.add(result);
            mScanListener.onBatchScanResults(list);
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        if (mScanListener != null) {
            mScanListener.onBatchScanResults(results);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if(mScanListener != null) {
            mScanListener.onScanFailed(errorCode);
        }
    }

    class ScanHandler extends Handler {
        private WeakReference<ScanManager> scanManager;

        public ScanHandler(ScanManager manager) {
            scanManager = new WeakReference<ScanManager>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (scanManager.get() != null) {
                switch (msg.what) {
                    case 0x01:
                        scanManager.get().cancelLeScan();
                        break;
                }
            }
        }
    }

    public interface OnScanManagerListener {
        void onBatchScanResults(List<ScanResult> results);
        void onScanFinished();

        /**
         * see {@link #SCAN_FAILED_ALREADY_STARTED},{@link #SCAN_FAILED_APPLICATION_REGISTRATION_FAILED},
         * {@link #SCAN_FAILED_FEATURE_UNSUPPORTED},{@link #SCAN_FAILED_INTERNAL_ERROR}
         * @param errorCode
         */
        void onScanFailed(int errorCode);
    }

}
