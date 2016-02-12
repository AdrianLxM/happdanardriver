package info.nightscout.happdanardriver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Tim on 13/01/2016.
 * Receives BOOT_COMPLETED Intent and starts service
 */
public class AutoStart extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, IncomingService.class));

    }
}
