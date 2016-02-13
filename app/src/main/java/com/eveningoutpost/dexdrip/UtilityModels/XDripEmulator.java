package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.BgReading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by stephenblack on 11/7/14.
 * Adapted by mike
 */
public class XDripEmulator {
    private static Logger log = LoggerFactory.getLogger(XDripEmulator.class);

    public void handleNewBgReading(BgReading bgReading, Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "sendQueue");
        wakeLock.acquire();
        try {
            Intent updateIntent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE_NO_DATA);
            context.sendBroadcast(updateIntent);

            log.debug("Broadcast data");
            Bundle bundle = new Bundle();
            bundle.putDouble(Intents.EXTRA_BG_ESTIMATE, bgReading.value);
            bundle.putDouble(Intents.EXTRA_BG_SLOPE, bgReading.slope);
            bundle.putString(Intents.EXTRA_BG_SLOPE_NAME, "9");
            bundle.putInt(Intents.EXTRA_SENSOR_BATTERY, bgReading.battery_level);
            bundle.putLong(Intents.EXTRA_TIMESTAMP, bgReading.timestamp);

            bundle.putDouble(Intents.EXTRA_RAW, bgReading.raw);
            Intent intent = new Intent(Intents.ACTION_NEW_BG_ESTIMATE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent, Intents.RECEIVER_PERMISSION);

        } finally {
            wakeLock.release();
        }
    }

}
