package info.nightscout.client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.XDripEmulator;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.squareup.otto.Bus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import info.nightscout.client.acks.NSAck;
import info.nightscout.client.acks.NSAuthAck;
import info.nightscout.client.acks.NSPingAck;
import info.nightscout.client.data.NSCal;
import info.nightscout.client.data.NSSgv;
import info.nightscout.client.data.NSTreatment;
import info.nightscout.danar.DanaConnection;
import info.nightscout.danar.event.CommandEvent;
import info.nightscout.client.events.NSStatusEvent;
import info.nightscout.happdanardriver.MainApp;
import info.nightscout.happdanardriver.Objects.ObjectToSync;
import info.nightscout.happdanardriver.PreferencesActivity;
import info.nightscout.danar.event.StatusEvent;
import info.nightscout.client.data.NSProfile;
import info.nightscout.client.utils.DateUtil;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NSClient {
    DecimalFormat formatNumber1place = new DecimalFormat("0.00");
    public Handler mPingHandler = null;
    public static Runnable mPingRunnable;
    private static Integer dataCounter = 0;

    private Bus mBus;
    private static Logger log = LoggerFactory.getLogger(NSClient.class);
    private Socket mSocket;
    private boolean isConnected = false;
    private String connectionStatus = "Not connected";
    private long mTimeDiff;

    private SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
    private boolean nsEnabled = SP.getBoolean("ns_enable", false);
    private String nsURL = SP.getString("ns_url", "");
    private String nsAPISecret = SP.getString("ns_api_secret", "");
    private boolean nsSyncProfile = SP.getBoolean("ns_sync_profile", false);


    private String nsAPIhashCode = "";

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private JSONObject mPreparedStatus = null;
    private ScheduledFuture<?> mOutgoingStatus = null;

    public NSClient(Bus bus) {
        log.debug("NSCLIENT start");
        MainApp.setNSClient(this);
        mBus = bus;

        dataCounter = 0;
        mPingHandler = new Handler();
        mPingRunnable = new Runnable() {
            @Override
            public void run() {
                doPing();
                mPingHandler.postDelayed(mPingRunnable, 60000);
            }
        };
        mPingHandler.postDelayed(mPingRunnable, 60000);

        readPreferences();

        if (nsAPISecret!="") nsAPIhashCode = Hashing.sha1().hashString(nsAPISecret, Charsets.UTF_8).toString();

        mBus.post(new NSStatusEvent(connectionStatus));
        if (nsEnabled && nsURL != "") {
            try {
                connectionStatus = "Connecting ...";
                mBus.post(new NSStatusEvent(connectionStatus));
                IO.Options opt = new IO.Options();
                opt.forceNew = true;
                mSocket = IO.socket(nsURL, opt);
                log.debug("NSCLIENT connect");
                mSocket.connect();
                mSocket.on("dataUpdate", onDataUpdate);
//                mSocket.on("bolus", onBolus);
                // resend auth on reconnect is needed on server restart
                mSocket.on("reconnect", resendAuth);
                sendAuthMessage(new NSAuthAck());

            } catch (URISyntaxException e) {
                connectionStatus = "Wrong URL syntax";
                mBus.post(new NSStatusEvent(connectionStatus));
            }
        } else {
            log.debug("NSCLIENT disabled");
            connectionStatus = "Disabled";
            mBus.post(new NSStatusEvent(connectionStatus));

            Toast.makeText(MainApp.instance().getApplicationContext(), "No NS URL specified or NS connection disabled", Toast.LENGTH_LONG).show();
            if (nsEnabled) {
                log.debug("NSCLIENT No NS URL specified");
                Intent i = new Intent(MainApp.instance().getApplicationContext(), PreferencesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainApp.instance().getApplicationContext().startActivity(i);
            }
        }
    }

    public void destroy() {
        log.debug("NSCLIENT destroy");
        mPingHandler.removeCallbacks(mPingRunnable);
        mSocket.disconnect();
        MainApp.setNSClient(null);
    }

    private void sendAuthMessage(NSAuthAck ack) {
        JSONObject authMessage = new JSONObject();
        try {
            authMessage.put("client", "HAPPdriver");
            authMessage.put("history", 1); // 1 hour only
            authMessage.put("status", true); // receive status
            authMessage.put("secret", nsAPIhashCode);
        } catch (JSONException e) {
            return;
        }
        log.debug("NSCLIENT authorize");
        mSocket.emit("authorize", authMessage, ack);
        synchronized(ack) {
            try {
                // Calling wait() will block this thread until another thread
                // calls notify() on the object.
                ack.wait(60000);
            } catch (InterruptedException e) {
                // Happens if someone interrupts your thread.
                ack.interrupted = true;
            }
        }
        if (ack.interrupted) {
            log.debug("NSCLIENT Auth interrupted");
            isConnected = false;
            connectionStatus = "Auth interrupted";
            mBus.post(new NSStatusEvent(connectionStatus));
        }
        else if (ack.received){
            log.debug("NSCLIENT Authenticated");
            connectionStatus = "Authenticated (";
            if (ack.read) connectionStatus += "R";
            if (ack.write) connectionStatus += "W";
            if (ack.write_treatment) connectionStatus += "T";
            connectionStatus += ')';
            isConnected = true;
            mBus.post(new NSStatusEvent(connectionStatus));
        } else {
            log.debug("NSCLIENT Auth timed out");
            isConnected = true;
            connectionStatus = "Auth timed out";
            mBus.post(new NSStatusEvent(connectionStatus));
            sendAuthMessage(ack);
            return;
        }
    }

    public void readPreferences() {
        SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
        nsEnabled = SP.getBoolean("ns_enable", false);
        nsURL = SP.getString("ns_url", "");
        nsAPISecret = SP.getString("ns_api_secret", "");
        nsSyncProfile = SP.getBoolean("ns_sync_profile", false);

    }

    private Emitter.Listener resendAuth = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            NSAuthAck ack = new NSAuthAck();
            sendAuthMessage(ack);
        }
   };

    private Emitter.Listener onDataUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            log.debug("NSCLIENT onDataUpdate");
            SP = PreferenceManager.getDefaultSharedPreferences(MainApp.instance().getApplicationContext());
            boolean emulatexDrip = SP.getBoolean("ns_emulate_xdrip", false);
            connectionStatus = "Data packet " + dataCounter++;
            mBus.post(new NSStatusEvent(connectionStatus));

            JSONObject data = (JSONObject) args[0];
            String activeProfile = MainApp.getNsActiveProfile();
            NSCal actualCal = new NSCal();
            try {
                // delta means only increment/changes are comming
                boolean isDelta =  data.has("delta");

                if (data.has("status")) {
                    JSONObject status = data.getJSONObject("status");
                    if (status.has("activeProfile")) {
                        activeProfile = status.getString("activeProfile");
                        if (activeProfile != "null") {
                            MainApp.setNsActiveProfile(activeProfile);
                            log.debug("NSCLIENT status activeProfile received: " + activeProfile);
                        } else {
                            activeProfile = null;
                            MainApp.setNsActiveProfile(null);
                        }
                    }
                    /*  Other received data to 2016/02/10
                        {
                          status: 'ok'
                          , name: env.name
                          , version: env.version
                          , versionNum: versionNum (for ver 1.2.3 contains 10203)
                          , serverTime: new Date().toISOString()
                          , apiEnabled: apiEnabled
                          , careportalEnabled: apiEnabled && env.settings.enable.indexOf('careportal') > -1
                          , boluscalcEnabled: apiEnabled && env.settings.enable.indexOf('boluscalc') > -1
                          , head: env.head
                          , settings: env.settings
                          , extendedSettings: ctx.plugins && ctx.plugins.extendedClientSettings ? ctx.plugins.extendedClientSettings(env.extendedSettings) : {}
                          , activeProfile ..... calculated from treatments or missing
                        }
                     */
                }
                if (data.has("profiles")) {
                    JSONArray profiles = (JSONArray) data.getJSONArray("profiles");
                    if (profiles.length() > 0) {
                        JSONObject profile = (JSONObject) profiles.get(profiles.length() - 1);
                        NSProfile nsProfile = new NSProfile(profile,activeProfile);
                        MainApp.setNsProfile(nsProfile);
                        Thread updatePumpBasal = new Thread() {
                            public void run() {
                                Object o = new Object();
                                synchronized (o) {
                                    try {
                                        o.wait(5000);
                                    } catch (InterruptedException e) {
                                    }
                                    DanaConnection dc = MainApp.getDanaConnection();
                                    if (dc != null) dc.updateBasalsInPump();
                                }
                            }
                        };
                        updatePumpBasal.start();
                        log.debug("NSCLIENT profile received: " + nsProfile.log());
                    }
                }
                if (data.has("treatments")) {
                    JSONArray treatments = (JSONArray) data.getJSONArray("treatments");
                    log.debug("NSCLIENT received " + treatments.length() + " treatments");
                    for (Integer index = 0; index < treatments.length(); index++) {
                        NSTreatment treatment =  new NSTreatment(treatments.getJSONObject(index));
                        if (treatment.getAction() == null ) {
                            // new treatment action
                        } else if (treatment.getAction() == "update" ) {
                            // update treatment action, record was set earlier and now is changed (find it by _id)
                        } if (treatment.getAction() == "remove" ) {
                            // remove treatment action, record was set earlier and now is removed from database (find it by _id)
                        }
                    }
                }
                if (data.has("devicestatus")) {
                    JSONArray devicestatuses = (JSONArray) data.getJSONArray("devicestatus");
                    log.debug("NSCLIENT received " + devicestatuses.length() + " devicestatuses");
                }
                if (data.has("mbgs")) {
                    JSONArray mbgs = (JSONArray) data.getJSONArray("mbgs");
                    log.debug("NSCLIENT received " + mbgs.length() + " mbgs");
                    for (Integer index = 0; index < mbgs.length(); index++) {
                    }
                }
                if (data.has("cals")) {
                    JSONArray cals = (JSONArray) data.getJSONArray("cals");
                    log.debug("NSCLIENT received " + cals.length() + " cals");
                    // Retreive actual calibration
                    for (Integer index = 0; index < cals.length(); index++) {
                        if (index ==0) {
                            actualCal.set(cals.optJSONObject(index));
                        }
                    }
                }
                if (data.has("sgvs")) {
                    XDripEmulator emulator = new XDripEmulator();
                    JSONArray sgvs = (JSONArray) data.getJSONArray("sgvs");
                    log.debug("NSCLIENT received " + sgvs.length() + " sgvs");
                    for (Integer index = 0; index < sgvs.length(); index++) {
                        // log.debug("NSCLIENT svg " + sgvs.getJSONObject(index).toString());
                        NSSgv sgv = new NSSgv(sgvs.getJSONObject(index));
                        // Handle new sgv here
                        if (emulatexDrip) {
                            BgReading bgReading = new BgReading(sgv, actualCal);
                            emulator.handleNewBgReading(bgReading, MainApp.instance().getApplicationContext());
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //log.debug("NSCLIENT onDataUpdate end");
        }
    };

    public void sendTreatmentStatusUpdate(String _id, String status) {
        try {
            if (!isConnected) return;
            JSONObject message = new JSONObject();
            message.put("_id", _id);
            message.put("collection", "treatments");
            JSONObject messageData = new JSONObject();
            messageData.put("status", status);
            message.put("data", messageData);
            mSocket.emit("dbUpdate", message);
        } catch (JSONException e) {
            return;
        }
    };

    public void sendAddTreatment(JSONObject data, NSAck ack) {
        try {
            if (!isConnected) return;
            JSONObject message = new JSONObject();
            message.put("collection", "treatments");
            message.put("data", data);
            mSocket.emit("dbAdd", message, ack);
            synchronized(ack) {
                try {
                    ack.wait(3000);
                } catch (InterruptedException e) {
                }
            }

        } catch (JSONException e) {
            return;
        }
    };

    public void sendAddTreatment(JSONObject data) {
        try {
            if (!isConnected) return;
            JSONObject message = new JSONObject();
            message.put("collection", "treatments");
            message.put("data", data);
            mSocket.emit("dbAdd", message);
        } catch (JSONException e) {
            return;
        }
    };

    private void sendAddStatus(JSONObject data) {
        try {
            if (!isConnected) return;
            JSONObject message = new JSONObject();
            message.put("collection", "devicestatus");
            message.put("data", data);
            mSocket.emit("dbAdd", message);
        } catch (JSONException e) {
            return;
        }
    };

    public void sendStatus(StatusEvent ev, String btStatus){
        //log.debug("NSCLIENT sendStatus enter");
        if (!isConnected) return;
        JSONObject nsStatus = new JSONObject();
        JSONObject pump = new JSONObject();
        JSONObject battery = new JSONObject();
        JSONObject status = new JSONObject();
        try {
            battery.put("percent", ev.remainBattery);
            pump.put("battery", battery);

            status.put("lastbolus", ev.last_bolus_amount);
            status.put("lastbolustime", DateUtil.toISOString(ev.last_bolus_time));
            if (ev.tempBasalRatio != -1) {
                status.put("tempbasalpct", ev.tempBasalRatio);
                if (ev.tempBasalStart != null) status.put("tempbasalstart", DateUtil.toISOString(ev.tempBasalStart));
                if (ev.tempBasalRemainMin != 0) status.put("tempbasalremainmin", ev.tempBasalRemainMin);
            }
            status.put("connection", btStatus);
            pump.put("status", status);

            pump.put("reservoir", formatNumber1place.format(ev.remainUnits));
            pump.put("clock", DateUtil.toISOString(new Date()));
            nsStatus.put("pump", pump);
        } catch (JSONException e) {
        }

        class RunnableWithParam implements Runnable {
            JSONObject param;
            RunnableWithParam(JSONObject param) {
                this.param = param;
            }
            public void run(){
                sendAddStatus(param);
                mPreparedStatus = null;
                log.debug("NSCLIENT sendStatus sending");
            };
        }

        // prepare task for execution in 3 sec
        // cancel waiting task to prevent sending multiple statuses
        if (mPreparedStatus != null) mOutgoingStatus.cancel(false);
        Runnable task = new RunnableWithParam(nsStatus);
        mPreparedStatus = nsStatus;
        mOutgoingStatus = worker.schedule(task, 3, TimeUnit.SECONDS);
    }

    public void doPing() {
        if (isConnected) {
            log.debug("NSCLIENT ping connected");
            try {
                NSPingAck ack = new NSPingAck();
                JSONObject message = new JSONObject();
                message.put("mills", System.currentTimeMillis());
                mSocket.emit("ping", message, ack);
                synchronized (ack) {
                    try {
                        // Calling wait() will block this thread until another thread
                        // calls notify() on the object.
                        ack.wait(10000);
                    } catch (InterruptedException e) {
                        // Happens if someone interrupts your thread.
                    }
                }
                mTimeDiff = System.currentTimeMillis() - ack.mills;
                if (!ack.received) {
                    connectionStatus = "Not responding";
                } else {
                    connectionStatus = "Connected";
                }
                mBus.post(new NSStatusEvent(connectionStatus));
            } catch (JSONException e) {
             }
        } else {
            log.debug("NSCLIENT ping disconnected");
        }
    };

}
