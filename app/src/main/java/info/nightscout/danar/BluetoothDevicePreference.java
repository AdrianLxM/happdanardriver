package info.nightscout.danar;

/**
 * Created by mike on 23.01.2016.
 */
import android.bluetooth.*;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import java.util.Set;

public class BluetoothDevicePreference extends ListPreference {

    public BluetoothDevicePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta != null) {
            Set<BluetoothDevice> pairedDevices = bta.getBondedDevices();
            CharSequence[] entries = new CharSequence[pairedDevices.size()];
            int i = 0;
            for (BluetoothDevice dev : pairedDevices) {
                entries[i] = dev.getName();
                //entryValues[i] = dev.getAddress();
                i++;
            }
            setEntries(entries);
            //setEntryValues(entryValues);
            setEntryValues(entries);
        } else {
            setEntries(new CharSequence[0]);
            setEntryValues(new CharSequence[0]);
        }
    }

    public BluetoothDevicePreference(Context context) {
        this(context, null);
    }

}