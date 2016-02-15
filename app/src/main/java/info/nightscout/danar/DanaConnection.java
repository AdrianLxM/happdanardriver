package info.nightscout.danar;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.Toast;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.squareup.otto.Bus;

import info.nightscout.client.data.NSProfile;
import info.nightscout.danar.event.BolusingEvent;
import info.nightscout.happdanardriver.MainApp;
import info.nightscout.client.NSHistorySync;
import info.nightscout.danar.alarm.ServiceAlarm;
import info.nightscout.danar.comm.*;
import info.nightscout.danar.db.HistoryRecord;
import info.nightscout.danar.db.PumpStatus;
import info.nightscout.danar.event.ConnectionStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;

import info.nightscout.danar.event.StatusEvent;
import info.nightscout.happdanardriver.Objects.Basal;

public class DanaConnection {

    private static Logger log = LoggerFactory.getLogger(DanaConnection.class);

    Handler mHandler;
    public static HandlerThread mHandlerThread;

    private final Bus mBus;
    private SerialEngine mSerialEngine;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private BluetoothSocket mRfcommSocket;
    private BluetoothDevice mDevice;
    private boolean connectionEnabled = false;
    PowerManager.WakeLock mWakeLock;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
    String devName = SP.getString("danar_bt_name", "");

    public DanaConnection(BluetoothDevice bDevice, Bus bus) {
        MainApp.setDanaConnection(this);

        mHandlerThread = new HandlerThread(DanaConnection.class.getSimpleName());
        mHandlerThread.start();

        this.mHandler = new Handler(mHandlerThread.getLooper());

        this.mBus = bus;

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter!=null) {
            Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : devices) {
                String dName = device.getName();
                if (devName.equals(dName)) {
                    device.getAddress();
                    mDevice = device;

                    try {
                        mRfcommSocket = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                    } catch (IOException e) {
                        log.error("err", e);
                    }

                    break;
                }
            }


            registerBTconnectionBroadcastReceiver();
        } else {
            Toast.makeText(MainApp.instance().getApplicationContext(), "No BT adapter", Toast.LENGTH_LONG).show();
        }
        if(mDevice==null) {
            Toast.makeText(MainApp.instance().getApplicationContext(), "No device found", Toast.LENGTH_LONG).show();
        }

        PowerManager powerManager = (PowerManager) MainApp.instance().getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DanaConnection");
    }

    public boolean isConnectionEnabled() {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        boolean isEnabled = SP.getBoolean("danar_enable", false);
        devName = SP.getString("danar_bt_name", "");

        if (!isEnabled || devName == "" || mDevice == null) return false;
        return true;
    }

    private void registerBTconnectionBroadcastReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String action = intent.getAction();
                Log.d("ConnectionBroadcast ", "Device  " + action + " " + device.getName());//Device has disconnected
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    Log.d("ConnectionBroadcast", "Device has disconnected " + device.getName());//Device has disconnected
                    if(mDevice.getName().equals(device.getName())) {
                        if(mRfcommSocket!=null) {

                            try {mInputStream.close();} catch (Exception e)  {log.debug(e.getMessage());}
                            try {mOutputStream.close();} catch (Exception e) {log.debug(e.getMessage());}
                            try {mRfcommSocket.close(); } catch (Exception e) {log.debug(e.getMessage());}


                        }
                        connectionEnabled = false;
                        mBus.post(new ConnectionStatusEvent(false,false, 0));
                        //connectionCheckAsync();
//                        MainApp.setDanaConnection(null);
                    }
                }

            }
        };
        MainApp.instance().getApplicationContext().registerReceiver(receiver,new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        MainApp.instance().getApplicationContext().registerReceiver(receiver,new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));
        MainApp.instance().getApplicationContext().registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
    }

    public void connectionCheckAsync(final String callerName) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connectionCheck(callerName);
            }
        }, 100);
    }

    public synchronized void connectIfNotConnected(String callerName) {
//        log.debug("connectIfNotConnected caller:"+callerName);
        if (!isConnectionEnabled()) return;
        mWakeLock.acquire();
        long startTime = System.currentTimeMillis();
        short connectionAttemptCount = 0;
        if(!(isConnected())) {
            long timeToConnectTimeSoFar = 0;
            while (!(isConnected())) {
                timeToConnectTimeSoFar = (System.currentTimeMillis() - startTime) / 1000;
                mBus.post(new ConnectionStatusEvent(true,false, connectionAttemptCount));
                connectionCheck(callerName);
                log.debug("connectIfNotConnected waiting " + timeToConnectTimeSoFar + "s attempts:" + connectionAttemptCount + " caller:"+callerName);
                connectionAttemptCount++;

                if(timeToConnectTimeSoFar/60>15 || connectionAttemptCount >180) {
                    Intent alarmServiceIntent = new Intent(MainApp.instance().getApplicationContext(), ServiceAlarm.class);
                    alarmServiceIntent.putExtra("alarmText","Connection error");
                    MainApp.instance().getApplicationContext().startService(alarmServiceIntent);
                }
            }
            log.debug("connectIfNotConnected took " + timeToConnectTimeSoFar + "s attempts:" + connectionAttemptCount);
        } else {
            mBus.post(new ConnectionStatusEvent(false, true, 0));
        }
        mWakeLock.release();
    }

    private synchronized void  connectionCheck(String callerName) {
        if(mDevice==null) {
            Toast.makeText(MainApp.instance().getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
            return;
        }

        if(mRfcommSocket == null) {
            try {
                mRfcommSocket = mDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                log.error("err", e);
            }
            if(mRfcommSocket==null) {
                log.warn("connectionCheck() mRfcommSocket is null ");
                return;
            }
        }
        if( !mRfcommSocket.isConnected()) {
//            log.debug("not connected");
            try {
                mRfcommSocket.connect();
                log.debug( "connected");

                mOutputStream = mRfcommSocket.getOutputStream();
                mInputStream =  mRfcommSocket.getInputStream();
                if(mSerialEngine!=null) {
                    mSerialEngine.stopIt();
                };
                mSerialEngine = new SerialEngine(mInputStream,mOutputStream,mRfcommSocket   );
                mBus.post(new ConnectionStatusEvent(false,true, 0));

            } catch (IOException e) {
                log.warn( "connectionCheck() ConnectionStatusEvent attempt failed: " + e.getMessage());
                mRfcommSocket = null;
                //connectionCheckAsync("connectionCheck retry");
            }
        }


        if(isConnected()) {
            mBus.post(new ConnectionStatusEvent(false,true, 0));
            pingStatus();
        }
    }

    private boolean isConnected() {
        return mRfcommSocket!=null && mRfcommSocket.isConnected();
    }

    private void pingKeepAlive() {
        try {
            if (!isConnectionEnabled()) return;
            StatusEvent statusEvent = StatusEvent.getInstance();
            if(new Date().getTime() - statusEvent.timeLastSync.getTime() > 240_000) {
                pingStatus();
            } else {
                mSerialEngine.sendMessage(new MsgDummy());
            }
        } catch (Exception e) {
            log.error("err", e);
        }

    }

    public void pingStatusAsync() {
        Thread ping = new Thread() {
            public void run() {
                connectIfNotConnected("pingStatusAsync");
                pingStatus();
            }
        };
        ping.start();
    }

    public void pingStatus() {
        if (!isConnectionEnabled()) return;
        Thread ping = new Thread() {
            public void run() {
                try {
                    mSerialEngine.sendMessage(new MsgStatus());
                    mSerialEngine.sendMessage(new MsgStatusBasic());
                    mSerialEngine.sendMessage(new MsgStatusTempBasal());
                    mSerialEngine.sendMessage(new MsgStatusTime());
                    mSerialEngine.sendMessage(new MsgStatusBolusExtended());



                    StatusEvent statusEvent = StatusEvent.getInstance();
                    PumpStatus pumpStatus = new PumpStatus();
                    pumpStatus.remainBattery = statusEvent.remainBattery;
                    pumpStatus.remainUnits = statusEvent.remainUnits;
                    pumpStatus.currentBasal = statusEvent.currentBasal;
                    pumpStatus.last_bolus_amount = statusEvent.last_bolus_amount;
                    pumpStatus.last_bolus_time = statusEvent.last_bolus_time;
                    pumpStatus.tempBasalInProgress = statusEvent.tempBasalInProgress;
                    pumpStatus.tempBasalRatio = statusEvent.tempBasalRatio;
                    pumpStatus.tempBasalRemainMin = statusEvent.tempBasalRemainMin;
                    pumpStatus.tempBasalStart = statusEvent.tempBasalStart;
                    pumpStatus.time = statusEvent.time;//Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
                    statusEvent.timeLastSync = statusEvent.time;

                    try {
                        MainApp.getDbHelper().getDaoPumpStatus().createOrUpdate(pumpStatus);
                    } catch (SQLException e) {
                        log.error("SQLException",e);
                    }
                    synchronized (this) {
                        this.notify();
                    }
                    mBus.post(statusEvent);

                } catch (Exception e) {
                    log.error("err",e);
                }
            }
        };
        ping.start();
        // wait for finish
        try {
            ping.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void tempBasal(final int percent, final int durationInHours) throws Exception {
        if (!isConnectionEnabled()) return;
        Thread temp = new Thread() {
            public void run() {
                MsgTempBasalStart msg = new MsgTempBasalStart(percent, durationInHours);
                mSerialEngine.sendMessage(msg);

                pingStatus();
            }
        };
        temp.start();
    }

    public void extendedBolus(final double amount, final byte durationInHalfHours) throws Exception {
        if (!isConnectionEnabled()) return;
        Thread temp = new Thread() {
            public void run() {
                MsgExtendedBolusStart msg = new MsgExtendedBolusStart(amount, durationInHalfHours);
                mSerialEngine.sendMessage(msg);

                pingStatus();
            }
        };
        temp.start();
    }

    public void extendedBolusStop() throws Exception {
        if (!isConnectionEnabled()) return;
        Thread temp = new Thread() {
            public void run() {
                MsgExtendedBolusStop msg = new MsgExtendedBolusStop();
                mSerialEngine.sendMessage(msg);

                pingStatus();
            }
        };
        temp.start();
    }

    public Result setTempBasalFromHAPP(final Basal basal) {
        final SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        final Result result = new Result();
        final StatusEvent ev = StatusEvent.getInstance();

        if (!isConnectionEnabled()) {
            result.result = false;
            result.comment = "Pump connection disabled";
            log.debug("setTempBasalFromHAPP: Pump connection disabled");
            return result;
        }
        Thread temp = new Thread() {
            public void run() {
                log.debug("setTempBasalFromHAPP: " + basal.ratePercent + "% " + basal.rate + "u/h " + basal.duration + " minutes");
                if (basal.ratePercent == 100) {
                    cancelTempBasalFromHAPP();
                    return;
                }
                // Load statuses from pump
                connectIfNotConnected("setTempBasalFromHAPP");
                // Query temp basal
                MsgStatusTempBasal statusMsg = new MsgStatusTempBasal();
                mSerialEngine.sendMessage(statusMsg);
                // Query extended bolus
                MsgStatusBolusExtended exStatusMsg = new MsgStatusBolusExtended();
                mSerialEngine.sendMessage(exStatusMsg);
                if (!statusMsg.received) {
                    // Load of status of current basal rate failed, give one more try
                    mSerialEngine.sendMessage(exStatusMsg);
                }
                if (!exStatusMsg.received) {
                    // Load of status of current extended bolus failed, give one more try
                    mSerialEngine.sendMessage(statusMsg);
                }

                // Check we have really current status of pump
                if (!statusMsg.received || !exStatusMsg.received) {
                    // Refuse any action because we don't know status of pump
                    result.result = false;
                    result.comment = "Unable to retreive pump status";
                    pingStatus();
                    log.debug("setTempBasalFromHAPP: Unable to retreive pump status");
                    return;
                }
                // Get max basal rate in settings (by default harcoded to 400)
                Integer maxBasalRate = SP.getInt("safety_maxbasalrate",400);
                // Check percentRate but absolute rate too, because we know real current basal in pump
                if (basal.ratePercent > maxBasalRate || basal.rate > ev.currentBasal * maxBasalRate / 100d) {
                    result.result = false;
                    result.comment = "Basal rate exceeds allowed limit";
                    pingStatus();
                    log.debug("setTempBasalFromHAPP: Basal rate exceeds allowed limit ratePercent: " + basal.ratePercent + " rate: " + basal.rate + " currentBasal: " + ev.currentBasal + " limit: " + maxBasalRate);
                    return;
                }

                // Temp basals under 100% are processed as temp basals in pump
                if (basal.ratePercent <= 100) {
                    // Check if some temp is already in progress
                    if (ev.tempBasalInProgress == 1) {
                        // Correct basal already set ?
                        if (ev.tempBasalRatio == basal.ratePercent) {
                            result.result = true;
                            pingStatus();
                            log.debug("setTempBasalFromHAPP: <100% correct basal already set");
                            return;
                        } else {
                            MsgTempBasalStop msgStop = new MsgTempBasalStop();
                            mSerialEngine.sendMessage(msgStop);
                            log.debug("setTempBasalFromHAPP: <100% stopping basal");
                            // Check for proper result
                            if (msgStop.failed || !msgStop.received) {
                                result.result = false;
                                result.comment = "Failed to stop previous temp basal";
                                pingStatus();
                                log.debug("setTempBasalFromHAPP: <100% failed to stop previous basal");
                                return;
                            }
                        }
                    }
                    if (ev.statusBolusExtendedInProgress) {
                        MsgExtendedBolusStop msgExStop = new MsgExtendedBolusStop();
                        mSerialEngine.sendMessage(msgExStop);
                        log.debug("setTempBasalFromHAPP: <100% stopping extended bolus");
                        // Check for proper result
                        if (msgExStop.failed || !msgExStop.received) {
                            result.result = false;
                            result.comment = "Failed to stop previous extended bolus";
                            pingStatus();
                            log.debug("setTempBasalFromHAPP: <100% failed to stop previous extended bolus");
                            return;
                        }
                    }
                    // Convert duration from minutes to hours
                    Integer duration = (int) Math.ceil(basal.duration / 60.0);
                    MsgTempBasalStart msgTempStart = new MsgTempBasalStart(basal.ratePercent, duration);
                    mSerialEngine.sendMessage(msgTempStart);
                    log.debug("setTempBasalFromHAPP: <100% setting temp "+ basal.ratePercent + "% for " + duration + " hours");
                    if (msgTempStart.failed || !msgTempStart.received) {
                        result.result = false;
                        result.comment = "Failed to set temp basal";
                        pingStatus();
                        log.debug("setTempBasalFromHAPP: <100% failed to set temp basal");
                        return;
                    }
                    result.result = true;
                    pingStatus();
                    log.debug("setTempBasalFromHAPP: basal set ok");
                    return;

                } else {
                    // Temp basal >100% are treated as extended boluses
                    // Calculate # of halfHours from minutes
                    Integer halfHours = (int) Math.floor(basal.duration/30);
                    // We keep current basal running so need to sub current basal
                    Double rate = basal.rate - ev.currentBasal;
                    // What is current rate of extended bolusing in u/h?
                    Double rateInProgress = ev.statusBolusExtendedInProgress ? ev.statusBolusExtendedPlannedAmount / ev.statusBolusExtendedDurationInMinutes * 60 : 0d;
                    log.debug("setTempBasalFromHAPP: >100% extended in progess: "+ ev.statusBolusExtendedInProgress + " amount: " + ev.statusBolusExtendedPlannedAmount +  "u duration: " + ev.statusBolusExtendedDurationInMinutes + "min");
                    log.debug("setTempBasalFromHAPP: >100% current rate: "+ ev.currentBasal + "u/h  rate to set: " + rate +  "u/h extended rate in progress: " + rateInProgress + "u/h");

                    // Compare with extended rate in progress
                    if (Math.abs(rateInProgress - rate) < 0.02D) { // Allow some rounding diff
                        // correct extended already set
                        result.result = true;
                        pingStatus();
                        log.debug("setTempBasalFromHAPP: >100% correct basal already set");
                        return;
                    }
                    // Stop normal temp if needed because we calculate with 100% of basal rate
                    if (ev.tempBasalInProgress == 1) {
                        MsgTempBasalStop msgStop1 = new MsgTempBasalStop();
                        mSerialEngine.sendMessage(msgStop1);
                        log.debug("setTempBasalFromHAPP: >100% stopping basal");
                        if (msgStop1.failed ||  !msgStop1.received) {
                            // If something goes wrong try to stop extened too
                            MsgExtendedBolusStop msgStop2 = new MsgExtendedBolusStop();
                            mSerialEngine.sendMessage(msgStop2);
                            result.result = false;
                            result.comment = "Failed to stop previous temp basal";
                            pingStatus();
                            log.debug("setTempBasalFromHAPP: >100% failed to stop basal");
                            return;
                        }
                    }

                    // Now set new extended, no need to to stop previous (if running) because it's replaced
                    MsgExtendedBolusStart msgStartExt = new MsgExtendedBolusStart(rate / 2 * halfHours, (byte) (halfHours & 0xFF));
                    mSerialEngine.sendMessage(msgStartExt);
                    log.debug("setTempBasalFromHAPP: >100% setting extended: " + (rate / 2 * halfHours) + "u  halfhours: " + (halfHours & 0xFF));
                    if (msgStartExt.failed || !msgStartExt.received) {
                        result.result = false;
                        result.comment = "Failed to set extended bolus";
                        pingStatus();
                        log.debug("setTempBasalFromHAPP: >100% failed to set extended");
                        return;
                    }
                    result.result = true;
                    pingStatus();
                    log.debug("setTempBasalFromHAPP: >100% extended set ok");
                    return;
                }
            }
        };
        // Run everything in separate thread to not block UI
        temp.start();
        // and wait for thread finish
        try {
            temp.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Result cancelTempBasalFromHAPP()  {
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        final StatusEvent ev = StatusEvent.getInstance();
        final Result result = new Result();
        log.debug("cancelTempBasalFromHAPP: enter");

        if (!isConnectionEnabled()) {
            result.result = false;
            result.comment = "Pump connection disabled";
            log.debug("cancelTempBasalFromHAPP: Pump connection disabled");
            return result;
        }
        Thread temp = new Thread() {
            public void run() {
                connectIfNotConnected("cancelTempBasalFromHAPP");
                // Query extended bolus status
                MsgStatusBolusExtended exStatusMsg = new MsgStatusBolusExtended();
                mSerialEngine.sendMessage(exStatusMsg);
                if (exStatusMsg.failed || !exStatusMsg.received) {
                    // Give one more try
                    mSerialEngine.sendMessage(exStatusMsg);
                }
                // If extended in progress or failed read status send cancel extended message
                if (ev.statusBolusExtendedInProgress || exStatusMsg.failed || !exStatusMsg.received) {
                    MsgExtendedBolusStop msgStop = new MsgExtendedBolusStop();
                    mSerialEngine.sendMessage(msgStop);
                    if (msgStop.failed || !msgStop.received) {
                        result.result = false;
                        result.comment = "Failed to stop previous extended bolus";
                        return;
                    }
                }
                // Query temp basal status
                MsgStatusTempBasal statusMsg = new MsgStatusTempBasal();
                mSerialEngine.sendMessage(statusMsg);
                if (statusMsg.failed || !statusMsg.received) {
                    // Give one more try
                    mSerialEngine.sendMessage(statusMsg);
                }
                // If temp in progress or failed read status send cancel temp message
                if (ev.tempBasalInProgress == 1 || statusMsg.failed || !statusMsg.received) {
                    MsgTempBasalStop msgStop = new MsgTempBasalStop();
                    mSerialEngine.sendMessage(msgStop);
                    if (msgStop.failed || !msgStop.received) {
                        result.result = false;
                        result.comment = "Failed to stop previous temp basal";
                        return;
                    }
                }
                result.result = true;
                pingStatus();
                return;
            }
        };
        // Run the thread
        temp.start();
        // Wait for finish
        try {
            temp.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void tempBasalOff() throws Exception {

        if (!isConnectionEnabled()) return;
        Thread temp = new Thread() {
            public void run() {
                StatusEvent statusEvent = StatusEvent.getInstance();
                if (statusEvent.tempBasalInProgress == 1) {
                    MsgTempBasalStop msg = new MsgTempBasalStop();
                    mSerialEngine.sendMessage(msg);
                }

                pingStatus();
            }
        };
        temp.start();
    }

    public static int byteArrayToInt(byte[] bArr,int offset, int lenght) {
        switch (lenght) {
            case 1:
                return bArr[0+offset] & 255;
            case 2:
                return ((bArr[0+offset] & 255) << 8) + (bArr[1+offset] & 255);
            case 3:
                return (((bArr[2+offset] & 255) << 16) + ((bArr[1+offset] & 255) << 8)) + (bArr[0+offset] & 255);
            case 4:
                return ((((bArr[3+offset] & 255) << 24) + ((bArr[2+offset] & 255) << 16)) + ((bArr[1+offset] & 255) << 8)) + (bArr[0+offset] & 255);
            default:
                return -1;
        }
    }

    public void stop() {
        try {mInputStream.close();} catch (Exception e)  {log.debug(e.getMessage());}
        try {mOutputStream.close();} catch (Exception e) {log.debug(e.getMessage());}
        try {mRfcommSocket.close();} catch (Exception e) {log.debug(e.getMessage());}
        if(mSerialEngine!=null) mSerialEngine.stopIt();
    }

    public Result bolusFromHAPP(final double amount, final String _id) {
        log.debug("bolusFromHAPP: bolus start " + amount + " _id: " + _id);
        final SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        final StatusEvent ev = StatusEvent.getInstance();
        final Result result = new Result();
        final BolusingEvent bolusingEvent = BolusingEvent.getInstance();

        if (!isConnectionEnabled()) {
            log.debug("bolusFromHAPP: Pump connection disabled");
            result.result = false;
            result.comment = "Pump connection disabled";
            return result;
        }
        Thread bolus = new Thread() {
            public void run() {
                connectIfNotConnected("bolus");
                // Request last bolus status
                MsgStatus msgStatus = new MsgStatus();
                mSerialEngine.sendMessage(msgStatus);
                if (!msgStatus.received) {
                    // If failed give on more try
                    mSerialEngine.sendMessage(msgStatus);
                }
                // Check if we have latest data
                if (!msgStatus.received) {
                    log.debug("bolusFromHAPP: Failed to read bolus status");
                    result.result = false;
                    result.comment = "Failed to read bolus status";
                    return;
                }
                Integer maxBolusRate = SP.getInt("safety_maxbolusrate", 60);
                Double minsFromLastBolus = ((new java.util.Date()).getTime() - ev.last_bolus_time.getTime()) / 1000d / 60;
                if ( minsFromLastBolus < maxBolusRate) {
                    log.debug("bolusFromHAPP: Max bolus ratio exceeded maxBolusRate: " + maxBolusRate + " minsFromLastBolus: " + minsFromLastBolus);
                    result.result = false;
                    result.comment = "Max bolus ratio exceeded";
                    return;
                }
                MsgBolusStart msg = new MsgBolusStart(amount, _id);
                MsgBolusProgress progress = new MsgBolusProgress(mBus, amount, _id);
                MsgBolusStop stop = new MsgBolusStop(mBus, _id);

                mSerialEngine.expectMessage(progress);
                mSerialEngine.expectMessage(stop);

                bolusingEvent.sStatus = "Starting";
                mBus.post(bolusingEvent);

                mSerialEngine.sendMessage(msg);
                while (!stop.stopped) {
                    mSerialEngine.expectMessage(progress);
                }

                bolusingEvent.sStatus = "Delivered " + amount + "U";
                mBus.post(bolusingEvent);

                if (progress.progress != 0) {
                    log.debug("bolusFromHAPP: Failed to send bolus");
                    result.result = false;
                    result.comment = "Failed to send bolus";
                } else {
                    log.debug("bolusFromHAPP: Result OK");
                    result.result = true;
                }
            };
        };
        bolus.start();
        try {
            bolus.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pingStatus();
        bolusingEvent.sStatus = "";
        mBus.post(bolusingEvent);
        return result;
    }

     public void historyAll() throws Exception {
        if (!isConnectionEnabled()) return;
        Thread history = new Thread() {
            public void run() {
                MsgPCCommStart start = new MsgPCCommStart();
                MsgPCCommStop stop = new MsgPCCommStop();
                MsgHistoryAll hist = new MsgHistoryAll();
                MsgHistoryAllDone done = new MsgHistoryAllDone();

                mSerialEngine.expectMessage(done);

                mSerialEngine.sendMessage(start);
                mSerialEngine.sendMessage(hist);
                while(!done.done) {
                    mSerialEngine.expectMessage(hist);
                }

                mSerialEngine.sendMessage(stop);

                pingStatus();
                Dao<HistoryRecord, String> daoHistoryRecord;
                try {
                    daoHistoryRecord = MainApp.getDbHelper().getDaoHistoryRecords();
                } catch (SQLException e) {
                    return;
                }
                NSHistorySync syncer = new NSHistorySync(daoHistoryRecord,MainApp.getNSClient());
                syncer.sync();
            }
        };
        history.start();
    }

    public void updateBasalsInPump() {
        if (!isConnectionEnabled()) return;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        final boolean syncBasal = SP.getBoolean("ns_sync_profile", false);
        if (syncBasal && MainApp.getNsProfile() != null) {
            double[] basal = buildDanaRProfileRecord(MainApp.getNsProfile());
            connectIfNotConnected("updateBasalsInPump");
            MsgSetBasalProfile msgSet = new MsgSetBasalProfile((byte) 0, basal);
            mSerialEngine.sendMessage(msgSet);
            MsgSetActivateBasalProfile msgActivate = new MsgSetActivateBasalProfile((byte) 0);
            mSerialEngine.sendMessage(msgActivate);
            pingStatus();
        }
    }

    public double[] buildDanaRProfileRecord (NSProfile nsProfile) {
        double [] record = new double[24];
        for (Integer hour = 0; hour < 24; hour ++) {
            double value = nsProfile.getBasal(hour * 60 * 60);
            log.debug("NS basal value for " + hour + ":00 is " + value);
            record[hour] = value;
        }
        return record;
    }
/*
    public void bolusStop( BolusUI bolusUI) throws Exception {
        MsgBolusStop stop = new MsgBolusStop(bolusUI, null);
        mSerialEngine.sendMessage(stop);
        while(!stop.stopped) {
            mSerialEngine.sendMessage(stop);
        }
        pingStatus();
    }

    public static void broadcastTempBasal(TempBasal tempBasal) {
        Intent intent = new Intent("danaR.action.TEMP_BASAL_DATA");

        Bundle bundle = new Bundle();

        bundle.putLong("timeStart", tempBasal.timeStart.getTime());
        bundle.putLong("timeEnd", tempBasal.getCurrentTimeEnd().getTime());
        bundle.putInt("baseRatio",tempBasal.baseRatio);
        bundle.putInt("tempRatio",tempBasal.tempRatio);
        bundle.putInt("percent",tempBasal.percent);

        intent.putExtras(bundle);
        MainApp.instance().getApplicationContext().sendBroadcast(intent);
    }
*/
}
