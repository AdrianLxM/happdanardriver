package info.nightscout.happdanardriver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;

import info.nightscout.danar.DanaConnection;
import info.nightscout.danar.Result;
import info.nightscout.happdanardriver.Objects.Basal;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.happdanardriver.Objects.ObjectToSync;
import info.nightscout.happdanardriver.Objects.Treatment;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Created by Tim on 13/01/2016.
 */
public class IncomingService extends android.app.Service {
    private static Logger log = LoggerFactory.getLogger(IncomingService.class);

    public IncomingService(){
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            log.info("handleMessage start");
            String action="";
            Long requested=0L;
            String dataString="";
            Bundle data = new Bundle();

            Toast.makeText(MainApp.instance(), "START", Toast.LENGTH_LONG).show();
            try {
                data = msg.getData();
                action = data.getString("ACTION");
                log.debug("RECEIVED: ACTION" + action);
                requested = data.getLong("DATE_REQUESTED", 0);
                log.debug("RECEIVED: DATE" + requested.toString());
                dataString = data.getString("DATA");
                log.debug("RECEIVED: DATA" + dataString);

            } catch (Exception e){
                e.printStackTrace();
                // TODO: 16/01/2016 Issue getting treatment details from HAPPs msg

            } finally {
                //Toast.makeText(MainApp.instance(), action, Toast.LENGTH_LONG).show();

                switch (action){
                    case "TEST_MSG":
                        Resources appR = MainApp.instance().getResources();
                        CharSequence txt = appR.getText(appR.getIdentifier("app_name", "string", MainApp.instance().getPackageName()));
                        Toast.makeText(MainApp.instance(), txt + ": HAPP has connected successfully. ", Toast.LENGTH_LONG).show();

                        break;
                    case "temp_basal":
                    case "cancel_temp_basal":
                        ObjectToSync basalSync = new Gson().fromJson(dataString, ObjectToSync.class);

                        Basal basal = new Basal();
                        basal.rate          =   basalSync.value1;
                        basal.ratePercent   =   Integer.parseInt(basalSync.value2);
                        basal.duration      =   Integer.parseInt(basalSync.value3);
                        basal.start_time    =   basalSync.requested;

                        basal.action        =   basalSync.action;
                        basal.been_set      =   false;
                        basal.happ_int_id   =   basalSync.happ_integration_id;
                        basal.auth_code     =   basalSync.integrationSecretCode;
                        basal.state         =   "received";
                        basal.save();

                        //We have now saved the requested treatments from HAPP to our local DB, now action them
                        connect_to_HAPP();
                        actionBasal();

                        break;
                    case "bolus_delivery":
                        Type type = new TypeToken<List<ObjectToSync>>(){}.getType();
                        List<ObjectToSync> bolusList = new Gson().fromJson(dataString, type);

                        for (ObjectToSync newBolus : bolusList){
                                try {
                                    Treatment newTreatment = new Treatment();
                                    newTreatment.type           =   newBolus.value3;
                                    newTreatment.date_requested =   newBolus.requested.getTime();
                                    newTreatment.value          =   newBolus.value1;

                                    newTreatment.delivered      =   false;
                                    newTreatment.happ_int_id    =   newBolus.happ_integration_id;
                                    newTreatment.auth_code      =   newBolus.integrationSecretCode;
                                    newTreatment.state          =   "received";
                                    newTreatment.save();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // TODO: 16/01/2016 Issue getting treatment details
                                }
                            }


                        //We have now saved the requested treatments from HAPP to our local DB, now action them
                        connect_to_HAPP();
                        actionTreatments();
                        break;
                }
            }
        }
    }

    public void actionTreatments(){
        log.info("actionTreatments start");
        DanaConnection dc = MainApp.getDanaConnection();

        List<Treatment> treatments = Treatment.getToBeActioned();                                   //Get all Treatments yet to be processed
        for (Treatment treatment : treatments){
            Long ageInMins = (new Date().getTime() - treatment.date_requested) /1000/60;
            if (ageInMins > 10){                                                                    //Treatment was requested as > 10mins old, its too old to be processed
                treatment.state         = "error";
                treatment.details       = "Treatment is older than 10mins, too old to be automated";
                treatment.delivered     = false;
                treatment.rejected      = true;
                treatment.happ_update   = true;
                treatment.save();

            } else {
                if (!dc.isConnectionEnabled()) {                                                  //Cannot use pump right now
                    treatment.state         = "error";
                    treatment.details       = "Pump connection not enabled";
                    treatment.delivered     = false;
                    treatment.happ_update   = true;
                    treatment.save();

                } else {                                                                            //Pump is online and available to accept a treatment
                    try {
                        Result result = dc.bolusFromHAPP(treatment.value, ""); // NS _id here
                        if (result.result) {
                            treatment.state         = "delivered";
                            treatment.details       = "Treatment has been sent to the pump";
                            treatment.delivered     = true;                                                 // TODO: 16/01/2016 logic should be set to check that the treatment was successfully delivered before setting this to true
                            treatment.happ_update   = true;
                            treatment.save();
                        } else {
                            treatment.state         = "error";
                            treatment.details       = result.comment;
                            treatment.delivered     = false;
                            treatment.happ_update   = true;
                            treatment.save();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (treatments.size() > 0) {
            connect_to_HAPP();
            Intent intent = new Intent("UPDATE_TREATMENTS");                                        //sends result to update UI if loaded
            MainApp.instance().sendBroadcast(intent);
        }
    }

    public void actionBasal(){
        log.info("actionBasal start");
        DanaConnection dc = MainApp.getDanaConnection();

        Basal lastActive = Basal.lastActive();                                                      //Basal that is active or last active
        Basal recentRequest = Basal.lastRequested();                                                //Basal most recently requested and is not active

        if (recentRequest != null){

            Boolean isPumpOnline    = true;                                                     // TODO: 16/01/2016 this should be a function to check if the pump is online
            Boolean isPumpBusy      = false;                                                    // TODO: 16/01/2016 this should be a function to check if the pump is busy, for example if delivering a treatment

            if (!dc.isConnectionEnabled()) {                                                  //Cannot use pump right now
                recentRequest.state         = "error";
                recentRequest.details       = "Pump connection disabled";
                recentRequest.been_set      = false;
                recentRequest.happ_update   = true;
                recentRequest.save();

            } else {
                                                                                     //Pump is online and available to accept the new Temp Basal
                switch (recentRequest.action) {
                    case "new":
                        Long ageInMins = (new Date().getTime() - recentRequest.start_time.getTime()) / 1000 / 60;

                        if (ageInMins > 10){
                            recentRequest.state         = "error";
                            recentRequest.details       = "NEW Basal Request is older than 10mins, too old to be set";
                            recentRequest.been_set      = false;
                            recentRequest.rejected      = true;
                            recentRequest.happ_update   = true;
                            recentRequest.save();

                        } else {

                            Result result = null;
                            result = dc.setTempBasalFromHAPP(recentRequest);
                            if (result.result) {
                                recentRequest.state = "set";
                                recentRequest.details = "Temp Basal Set " + recentRequest.rate + "U (" + recentRequest.ratePercent + "%)";
                                recentRequest.been_set = true;
                                recentRequest.happ_update = true;
                                recentRequest.save();
                            } else {
                                recentRequest.state         = "error";
                                recentRequest.details       = result.comment;
                                recentRequest.been_set      = false;
                                recentRequest.rejected      = true;
                                recentRequest.happ_update   = true;
                                recentRequest.save();
                            }
                            break;
                        }

                        break;

                    case "cancel":
                        if (lastActive == null) {
                            recentRequest.state = "error";
                            recentRequest.details = "Current Running Temp Basal does not match this Cancel request, Temp Basal has not been canceled";
                            recentRequest.been_set = false;
                            recentRequest.happ_update = true;
                            recentRequest.rejected = true;
                            recentRequest.save();
                        } else {
                            if (lastActive.happ_int_id.equals(recentRequest.happ_int_id)) {
                                Result result = null;
                                result = dc.cancelTempBasalFromHAPP();
                                if (result.result) {
                                    recentRequest.state = "canceled";
                                    recentRequest.details = "This Temp Basal was running and has now been Canceled";
                                    recentRequest.been_set = true;
                                    recentRequest.happ_update = true;
                                    recentRequest.save();
                                } else {
                                    recentRequest.state = "error";
                                    recentRequest.details = result.comment;
                                    recentRequest.been_set = false;
                                    recentRequest.happ_update = true;
                                    recentRequest.save();
                                }
                            } else {
                                recentRequest.state = "error";
                                recentRequest.details = "Current Running Temp Basal does not match this Cancel request, Temp Basal has not been canceled";
                                recentRequest.been_set = false;
                                recentRequest.happ_update = true;
                                recentRequest.rejected = true;
                                recentRequest.save();
                            }
                        }
                        break;

                }
            }

            connect_to_HAPP();
            Intent intent = new Intent("UPDATE_BASAL");                                             //sends result to update UI if loaded
            MainApp.instance().sendBroadcast(intent);
        }
    }
    
    final Messenger myMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return myMessenger.getBinder();
    }



    public void updateHAPPBolus(){
        log.info("updateHAPPBolus start");
        List<Treatment> treatments = Treatment.getToUpdateHAPP();
        log.debug("UPDATE HAPP:" + treatments.size() + " treatments");

        for (Treatment bolus : treatments) {

            ObjectToSync bolusSync = new ObjectToSync(bolus,null);

            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("ACTION", "bolus_delivery");
            bundle.putString("UPDATE", bolusSync.asJSONString());
            msg.setData(bundle);

            try {
                log.debug("UPDATE HAPP:" + "HAPP INT ID " + bolusSync.happ_integration_id);
                myService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                log.debug("UPDATE HAPP:" + "Update FAILED for " + bolusSync.happ_integration_id);
            } finally {
                log.debug("UPDATE HAPP:" + "Update sent for " + bolusSync.happ_integration_id);
                bolus.happ_update = false;
                bolus.save();
            }
        }

        try {
            if (isBound) IncomingService.this.unbindService(myConnection);
        } catch (IllegalArgumentException e) {
            //catch if service was killed in a unclean way
        }
    }

    public void updateHAPPBasal(){
        log.info("updateHAPPBasal start");
        List<Basal> basals = Basal.getToUpdateHAPP();
        log.debug("UPDATE HAPP:" + basals.size() + " basals");

        for (Basal basal : basals) {

            ObjectToSync basalSync = new ObjectToSync(null,basal);

            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("ACTION", "temp_basal");
            bundle.putString("UPDATE", basalSync.asJSONString());
            msg.setData(bundle);

            try {
                log.debug("UPDATE HAPP:" + "HAPP INT ID " + basalSync.happ_integration_id);
                myService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                log.debug("UPDATE HAPP:" + "Update FAILED for " + basalSync.happ_integration_id);
            } finally {
                log.debug("UPDATE HAPP:" + "Update sent for " + basalSync.happ_integration_id);
                basal.happ_update = false;
                basal.save();
            }
        }

        try {
            if (isBound) IncomingService.this.unbindService(myConnection);
        } catch (IllegalArgumentException e) {
            //catch if service was killed in a unclean way
        }
    }

    //Connect to the HAPP Treatments Service
    private void connect_to_HAPP(){
        log.info("connect_to_HAPP start");
        Intent intent = new Intent("com.hypodiabetic.happ.services.TreatmentService");
        intent.setPackage("com.hypodiabetic.happ");
        IncomingService.this.bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
    }
    //Our Service that HAPP will connect to
    private Messenger myService = null;
    private Boolean isBound = false;
    private ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            log.info("onServiceConnected start");
            myService = new Messenger(service);
            isBound = true;

            updateHAPPBolus();
            updateHAPPBasal();
        }

        public void onServiceDisconnected(ComponentName className) {
            log.info("onServiceDisconnected start");
            myService = null;
            isBound = false;
        }
    };

}
