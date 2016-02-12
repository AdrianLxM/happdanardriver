package info.nightscout.danar.comm;

import com.squareup.otto.Bus;

import info.nightscout.danar.event.BolusingEvent;
import info.nightscout.happdanardriver.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgBolusStop extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusStop.class);
    private String _id;
    private static Bus bus = null;

    public boolean stopped = false;

    public MsgBolusStop() {
        super("CMD_MEALINS_STOP");
        SetCommand(SerialParam.CTRL_CMD_BOLUS);
        SetSubCommand(SerialParam.CTRL_SUB_BOLUS_STOP);
    }

    public MsgBolusStop(String cmdName) {
        super(cmdName);
    }

    public MsgBolusStop(Bus bus, String _id) {
        this();
        this.bus = bus;
        this._id = _id;
    }


    public void handleMessage(byte[] bytes) {

        stopped = true;
//        bolusUI.bolusFinished();
        BolusingEvent bolusingEvent = BolusingEvent.getInstance();
        bolusingEvent.sStatus = "Delivered";
        bus.post(bolusingEvent);
        MainApp.getNSClient().sendTreatmentStatusUpdate(this._id, "Delivered");
    }


}
