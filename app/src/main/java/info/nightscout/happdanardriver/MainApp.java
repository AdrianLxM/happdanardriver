package info.nightscout.happdanardriver;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.client.NSClient;
import info.nightscout.client.data.NSProfile;
import info.nightscout.danar.DanaConnection;
import info.nightscout.danar.db.DatabaseHelper;

/**
 * Created by Tim on 13/01/2016.
 */
public class MainApp extends Application {
    private static Logger log = LoggerFactory.getLogger(MainApp.class);

    private static MainApp sInstance;
    private static Bus sBus;

    private static Integer downloadedRecords = 0;

    private static DatabaseHelper databaseHelper = null;
    private static DanaConnection sDanaConnection = null;
    private static NSClient nsClient = null;

    public static NSProfile nsProfile;
    public static String nsActiveProfile = null;


    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        //Manually initialize ActiveAndroid
        // TODO: 05/11/2015 appears to be a bug in Active Android where DB version is ignored in Manifest, must be added here as well
        // http://stackoverflow.com/questions/33164456/update-existing-database-table-with-new-column-not-working-in-active-android
        Configuration configuration = new Configuration.Builder(this).setDatabaseVersion(2).create();
        ActiveAndroid.initialize(configuration); //// TODO: 06/01/2016 change to this?

        //connect_to_HAPP(this);
        sBus = new Bus(ThreadEnforcer.ANY);

    }




    public static MainApp instance() {
        return sInstance;
    }

    public static Bus bus() {
        return sBus;
    }


    public static DatabaseHelper getDbHelper() {
        if (databaseHelper == null) {
            databaseHelper = OpenHelperManager.getHelper(sInstance, DatabaseHelper.class);
        }
        return databaseHelper;
    }
    public static void closeDbHelper() {
        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        databaseHelper.close();
    }

    public static DanaConnection getDanaConnection() {
        log.debug("getDanaConnection " + sDanaConnection == null ? "null" : "initialized");
        return sDanaConnection;
    }

    public static void setDanaConnection(DanaConnection con) {
        sDanaConnection = con;
    }

    public static NSClient getNSClient() {
        return nsClient;
    }

    public static void setNSClient(NSClient client) {
        nsClient = client;
    }
    public static void setNsProfile(NSProfile profile) { nsProfile = profile; }

    public static NSProfile getNsProfile() { return nsProfile; }

    public static void setNsActiveProfile(String activeProfile) { nsActiveProfile = activeProfile; }

    public static String getNsActiveProfile() { return nsActiveProfile; }
}
