package info.nightscout.happdanardriver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import info.nightscout.client.NSClient;
import info.nightscout.client.events.NSStatusEvent;
import info.nightscout.danar.DanaConnection;
import info.nightscout.danar.Result;
import info.nightscout.danar.ServiceConnection;
import info.nightscout.danar.event.BolusingEvent;
import info.nightscout.danar.event.CommandEvent;
import info.nightscout.danar.event.ConnectionStatusEvent;
import info.nightscout.danar.event.StatusEvent;
import info.nightscout.happdanardriver.Objects.Basal;
import info.nightscout.happdanardriver.Objects.Treatment;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainActivity extends AppCompatActivity {
    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;
    Fragment bolusFragmentObject;
    Fragment basalFragmentObject;

    BroadcastReceiver happConnected;
    BroadcastReceiver refreshTreatments;
    BroadcastReceiver refreshBasal;

    private static Logger log = LoggerFactory.getLogger(MainActivity.class);

    private static DecimalFormat formatNumber1place = new DecimalFormat("0.00");
    private static DateFormat formatDateToJustTime = new SimpleDateFormat("HH:mm");

    Button sync;
    Button readstatus;
    TextView uRemaining;
    TextView batteryStatus;
    TextView tempBasalRatio;
    TextView tempBasalRemain;
    TextView currentBasal;

    TextView extendedBolusAmount;
    TextView extendedBolusSoFar;
    TextView lastBolusAmount;
    TextView bolusingStatus;
    TextView lastBolusTime;
    TextView lastCheck;

    TextView connection;
    TextView nsConnection;
    TextView mSyncStatus;

    static Handler mHandler;
    static private HandlerThread mHandlerThread;

    private DanaConnection mDanaConnection;
    private NSClient mNSClient;

    private String mBTStatus = "";


    //Our Service that HAPP will connect to
    private Messenger myService = null;
    private android.content.ServiceConnection myConnection = new android.content.ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            myService = new Messenger(service);

            //Broadcast there has been a connection
            Intent intent = new Intent("HAPP_CONNECTED");
            LocalBroadcastManager.getInstance(MainApp.instance()).sendBroadcast(intent);
        }

        public void onServiceDisconnected(ComponentName className) {
            myService = null;
            //FYI, only called if Service crashed or was killed, not on unbind
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the 4 primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        bolusFragmentObject = new bolusFragment();
        basalFragmentObject = new basalFragment();

        MainApp.instance().getApplicationContext().startService(new Intent(MainApp.instance().getApplicationContext(), ServiceConnection.class));
        if(mHandler==null) {
            mHandlerThread = new HandlerThread(MainActivity.class.getSimpleName() + "Handler");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setupAlarmManager();
                log.debug("setupAlarmManager");
            }
        });

        registerBus();
        registerGUIElements();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (happConnected != null){
            LocalBroadcastManager.getInstance(MainApp.instance()).unregisterReceiver(happConnected);
        }
        if (refreshTreatments != null){
            unregisterReceiver(refreshTreatments);
        }
        if (refreshBasal != null){
            unregisterReceiver(refreshBasal);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        //Refresh the treatments list
        refreshTreatments = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                bolusFragment.update();
            }
        };
        registerReceiver(refreshTreatments, new IntentFilter("UPDATE_TREATMENTS"));
        bolusFragment.update();

        //Refresh the Basal list
        refreshBasal = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                basalFragment.update();
            }
        };
        registerReceiver(refreshBasal, new IntentFilter("UPDATE_BASAL"));
        basalFragment.update();

        if (MainApp.getNSClient() != null) MainApp.getNSClient().doPing();
        if (MainApp.getDanaConnection() != null) MainApp.getDanaConnection().pingStatusAsync();
    }

    public void sendMessage()
    {
        //listen out for a successful connection
        happConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Resources appR = getApplicationContext().getResources();
                CharSequence txt = appR.getText(appR.getIdentifier("app_name", "string", getApplicationContext().getPackageName()));

                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putString("ACTION","TEST_MSG");
                bundle.putString("UPDATE", txt.toString());
                msg.setData(bundle);

                try {
                    myService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    //cannot Bind to service
                    Toast toast = Toast
                            .makeText(getApplicationContext(), "error sending msg: " + e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                }

                if (happConnected != null) LocalBroadcastManager.getInstance(MainApp.instance()).unregisterReceiver(happConnected); //Stop listening for new connections
                MainApp.instance().unbindService(myConnection);
            }
        };
        LocalBroadcastManager.getInstance(MainApp.instance()).registerReceiver(happConnected, new IntentFilter("HAPP_CONNECTED"));

        connect_to_HAPP(MainApp.instance());
    }

    //Connect to the HAPP Treatments Service
    private void connect_to_HAPP(Context c){
        Intent intent = new Intent("com.hypodiabetic.happ.services.TreatmentService");
        intent.setPackage("com.hypodiabetic.happ");
        c.bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
    }



    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    return bolusFragmentObject;
                case 1:
                    return basalFragmentObject;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Bolus Requests";
                case 1:
                    return "Basal Requests";
            }
            return null;
        }
    }



    public static class bolusFragment extends Fragment {
        public bolusFragment(){}
        private static ListView list;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_treatments_list, container, false);
            list    =   (ListView) rootView.findViewById(R.id.treatmentsFragmentList);

            update();
            return rootView;
        }

        public static void update(){

            if (list != null) {
                ArrayList<HashMap<String, String>> treatmentsList = new ArrayList<>();
                List<Treatment> treatments = Treatment.getLatestTreatments(10);
                Calendar treatmentDate = Calendar.getInstance();
                SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);

                for (Treatment treatment : treatments) {                                                    //Convert from a List<Object> Array to ArrayList
                    HashMap<String, String> treatmentItem = new HashMap<String, String>();

                    if (treatment.date_requested != null) {
                        treatmentDate.setTime(new Date(treatment.date_requested));
                    } else {
                        treatmentDate.setTime(new Date(0));                                                 //Bad Treatment
                    }
                    treatmentItem.put("type", treatment.type);
                    treatmentItem.put("value", treatment.value.toString() + "U");
                    treatmentItem.put("dateTime", sdfDateTime.format(treatmentDate.getTime()));
                    treatmentItem.put("state", "State:" + treatment.state);
                    treatmentItem.put("delivered", "Delivered:" + treatment.delivered);
                    treatmentItem.put("rejected", "Rejected:" + treatment.rejected);
                    treatmentItem.put("happ_id", "HAPP Integration ID:" + treatment.happ_int_id);
                    treatmentItem.put("happ_update", "Update Needed:" + treatment.happ_update);
                    treatmentItem.put("details", treatment.details);

                    treatmentsList.add(treatmentItem);
                }

                SimpleAdapter adapter = new SimpleAdapter(MainApp.instance(), treatmentsList, R.layout.treatments_list_layout,
                        new String[]{"type", "value", "dateTime", "state", "delivered", "rejected", "happ_id", "happ_update", "details"},
                        new int[]{R.id.treatmentTypeLayout, R.id.treatmentValueLayout, R.id.treatmentDateTimeLayout, R.id.treatmentStateLayout, R.id.treatmentDeliveredLayout, R.id.treatmentRejectedLayout, R.id.treatmentHAPPIDLayout, R.id.treatmentHAPPUpdateLayout, R.id.treatmentDetailsLayout});
                list.setAdapter(adapter);
            }
        }
    }
    public static class basalFragment extends Fragment {
        public basalFragment() {
        }

        private static ListView list;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_treatments_list, container, false);
            list = (ListView) rootView.findViewById(R.id.treatmentsFragmentList);

            update();
            return rootView;
        }

        public static void update() {
            if (list != null) {
                ArrayList<HashMap<String, String>> basalList = new ArrayList<>();
                List<Basal> basals = Basal.getLatest(10);
                Calendar basalDate = Calendar.getInstance();
                SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", MainApp.instance().getResources().getConfiguration().locale);

                for (Basal basal : basals) {                                                    //Convert from a List<Object> Array to ArrayList
                    HashMap<String, String> basalItem = new HashMap<String, String>();

                    basalDate.setTime(basal.start_time);

                    basalItem.put("type", basal.action);
                    basalItem.put("value", basal.rate + "U/h (" + basal.ratePercent + "%) " + basal.duration + "mins");
                    basalItem.put("dateTime", sdfDateTime.format(basalDate.getTime()));
                    basalItem.put("state", "State:" + basal.state);
                    basalItem.put("delivered", "Set:" + basal.been_set);
                    basalItem.put("rejected", "Rejected:" + basal.rejected);
                    basalItem.put("happ_id", "HAPP Integration ID:" + basal.happ_int_id);
                    basalItem.put("happ_update", "Update Needed:" + basal.happ_update);
                    basalItem.put("details", basal.details);

                    basalList.add(basalItem);
                }

                SimpleAdapter adapter = new SimpleAdapter(MainApp.instance(), basalList, R.layout.treatments_list_layout,
                        new String[]{"type", "value", "dateTime", "state", "delivered", "rejected", "happ_id", "happ_update", "details"},
                        new int[]{R.id.treatmentTypeLayout, R.id.treatmentValueLayout, R.id.treatmentDateTimeLayout, R.id.treatmentStateLayout, R.id.treatmentDeliveredLayout, R.id.treatmentRejectedLayout, R.id.treatmentHAPPIDLayout, R.id.treatmentHAPPUpdateLayout, R.id.treatmentDetailsLayout});
                list.setAdapter(adapter);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_readstatus) {
            log.debug("Reading status");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    DanaConnection dc = MainApp.getDanaConnection();
                    dc.connectIfNotConnected("connect req from UI");
                    dc.pingStatus();
                }
            });
            return true;
        }

        if (id == R.id.action_preferences) {
            log.debug("Opening preferences activity");
            Intent i = new Intent(getApplicationContext(), PreferencesActivity.class);
            startActivity(i);
            return true;
        }

        if (id == R.id.action_testhappconnectivity) {
            log.debug("Test action connectivity");
            sendMessage();
            return true;
        }
        if (id == R.id.action_restartnsclient) {
            log.debug("Restart NS client");
            MainApp.bus().post(new CommandEvent("RestartNSClient"));
            return true;
        }
/*
        if (id == R.id.action_exit) {
            log.debug("Exiting");

            //MainApp.closeDbHelper(); !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            MainApp.getNSClient().destroy();
            MainApp.setNSClient(null);
            finish();
            System.runFinalization();
            System.exit(0);
            return true;
        }
*/
        return super.onOptionsItemSelected(item);
    }

    private void registerGUIElements(){

        ((TextView) findViewById(R.id.lastconnclock)).setText("{fa-clock-o}");
        ((TextView) findViewById(R.id.lastbolusclock)).setText("{fa-clock-o}");
        ((TextView) findViewById(R.id.tempBasalclock)).setText("{fa-hourglass-o}");

        uRemaining = (TextView) findViewById(R.id.uRemaining);
        batteryStatus = (TextView) findViewById(R.id.batteryStatus);
        tempBasalRatio = (TextView) findViewById(R.id.tempBasalRatio);
        currentBasal = (TextView) findViewById(R.id.currentBasal);
        tempBasalRemain = (TextView) findViewById(R.id.tempBasalRemain);

        extendedBolusAmount =(TextView) findViewById(R.id.extendedBolusAmount);
        extendedBolusSoFar =(TextView) findViewById(R.id.extendedBolusSoFar);
        lastBolusAmount =   (TextView) findViewById(R.id.lastBolusAmount);
        bolusingStatus =    (TextView) findViewById(R.id.bolusingStatus);
        lastBolusTime =     (TextView) findViewById(R.id.lastBolusTime);
        lastCheck =         (TextView) findViewById(R.id.lastCheck);
        nsConnection =      (TextView) findViewById(R.id.nsConnection);
        connection =        (TextView) findViewById(R.id.connection);
        connection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      DanaConnection dc = MainApp.getDanaConnection();
                                      dc.connectIfNotConnected("connect req from UI");
                                  }}
                );
            }
        });

    }

    @Subscribe
    public void onStatusEvent(final NSStatusEvent e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nsConnection.setText(e.status);
            }
        });

    }

    @Subscribe
    public void onStatusEvent(final ConnectionStatusEvent c) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (c.sConnecting) {
                                  connection.setText("{fa-bluetooth-b spin} " + c.sConnectionAttemptNo);
                                  mBTStatus = "Connecting " + +c.sConnectionAttemptNo;
                              } else {
                                  if (c.sConnected) {
                                      connection.setText("{fa-bluetooth}");
                                      mBTStatus = "Connected";
                                  } else {
                                      connection.setText("{fa-bluetooth-b}");
                                      mBTStatus = "Disconnected";
                                  }
                              }
                          }
                      }
        );

    }

    private void registerBus() {
        try {
            MainApp.bus().unregister(this);
        } catch (RuntimeException x) {
        }
        MainApp.bus().register(this);
    }

    @Subscribe
    public void onStatusEvent(final StatusEvent ev) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                uRemaining.setText(formatNumber1place.format(ev.remainUnits) + "u");
                updateBatteryStatus(ev);

                currentBasal.setText(formatNumber1place.format(ev.currentBasal) + "u/h");
                tempBasalRemain.setText((ev.tempBasalRemainMin / 60) + ":" + ev.tempBasalRemainMin % 60);

                lastBolusAmount.setText(formatNumber1place.format(ev.last_bolus_amount) + "u");
                lastBolusTime.setText(formatDateToJustTime.format(ev.last_bolus_time));

                extendedBolusAmount.setText(formatNumber1place.format(ev.statusBolusExtendedPlannedAmount) + "u");
                extendedBolusSoFar.setText(ev.statusBolusExtendedDurationSoFarInMinutes + "of" + ev.statusBolusExtendedDurationInMinutes + "min");

                lastCheck.setText(formatDateToJustTime.format(ev.timeLastSync));
                if (ev.tempBasalRatio != -1) {
                    tempBasalRatio.setText(ev.tempBasalRatio + "%");
                } else {
                    tempBasalRatio.setText("100%");
                }

                if (MainApp.getNSClient() != null) MainApp.getNSClient().sendStatus(ev, mBTStatus);
            }
        });
    }

    @Subscribe
    public void onStatusEvent(final BolusingEvent ev) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                bolusingStatus.setText(ev.sStatus);
            }
        });
    }

    private void updateBatteryStatus(StatusEvent ev) {
        batteryStatus.setText("{fa-battery-" + (ev.remainBattery / 25) + "}");
    }

    private void setupAlarmManager() {
        AlarmManager am = ( AlarmManager ) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent( "info.nightscout.danar.ReceiverKeepAlive.action.PING"  );
        PendingIntent pi = PendingIntent.getBroadcast( this, 0, intent, 0 );

        long interval = 30*60_000L;
        long triggerTime = SystemClock.elapsedRealtime() + interval;

        try {
            pi.send();
        } catch (PendingIntent.CanceledException e) {
        }

        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(), interval, pi);

        List<ResolveInfo> queryBroadcastReceivers = getPackageManager().queryBroadcastReceivers(intent, 0);

        log.debug("queryBroadcastReceivers " + queryBroadcastReceivers);

    }

    public void testBolus(View view) {
        /*
        BolusingEvent ev = BolusingEvent.getInstance();
        ev.sStatus = "Connecting";
        MainApp.bus().post(ev);
        DanaConnection dc = MainApp.getDanaConnection();
        dc.connectIfNotConnected("testBolus");
        try {
            dc.bolus(1D,"aaa");
        } catch (Exception e) {
            e.printStackTrace();
        }
*/

        DanaConnection dc = MainApp.getDanaConnection();
        dc.connectIfNotConnected("happbasal");
        try {
            Basal basal = new Basal();
            basal.rate = 0.7;
            basal.ratePercent = 190;
            basal.duration = 60;
            Result r = dc.setTempBasalFromHAPP(basal);
            log.debug("Basal result " + r.result + " " + r.comment);
        } catch (Exception e) {
            e.printStackTrace();
        }
 /*
        DanaConnection dc = MainApp.getDanaConnection();
        dc.connectIfNotConnected("happbasal");
        Result r = dc.cancelTempBasalFromHAPP();
         */
    }
}

