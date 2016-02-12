package info.nightscout.danar.event;

import info.nightscout.danar.db.PumpStatus;

/**
 * Created by mike on 05.02.2016.
 */
public class BolusingEvent {
    public String sStatus = "";
    private static BolusingEvent bolusingEvent = null;

    public BolusingEvent(String status) {
        sStatus = status;
    }

    public BolusingEvent() {

    }
    public static BolusingEvent getInstance() {
        if(bolusingEvent == null) {
            bolusingEvent = new BolusingEvent();
        }
        return bolusingEvent;
    }
}
