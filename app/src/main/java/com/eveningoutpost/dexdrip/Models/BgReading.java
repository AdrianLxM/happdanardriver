package com.eveningoutpost.dexdrip.Models;

import info.nightscout.client.data.NSCal;
import info.nightscout.client.data.NSSgv;

public class BgReading {
    public double slope;
    public double raw;
    public long timestamp;
    public double value;
    public int battery_level;

    public BgReading(NSSgv sgv, NSCal cal) {
        slope = cal.slope;
        raw = sgv.getUnfiltered();
        timestamp = sgv.getMills();
        value = sgv.getMgdl();
        battery_level = 50;
    }
}
