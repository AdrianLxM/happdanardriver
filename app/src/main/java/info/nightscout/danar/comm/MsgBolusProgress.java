package info.nightscout.danar.comm;

import com.squareup.otto.Bus;

import info.nightscout.danar.event.BolusingEvent;
import info.nightscout.happdanardriver.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class MsgBolusProgress extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusProgress.class);
    public static final DecimalFormat bolusNumberFormat = new DecimalFormat("0.0");
    private static Bus bus = null;

//    private BolusUI bolusUI;
    private String _id;
    private double amount;

    public int progress = -1;

    public MsgBolusProgress() {
        super("CMD_PUMP_THIS_REMAINDER_MEAL_INS");
        SetCommand(SerialParam.CTRL_CMD_STATUS);
        SetSubCommand(SerialParam.CTRL_SUB_STATUS_BOLUS_PROGRESS);
    }

    public MsgBolusProgress(String cmdName) {
        super(cmdName);
    }


    public MsgBolusProgress(Bus bus, double amount, String _id) {
        this();
        this.amount = amount;
        this. _id = _id;
        this.bus = bus;
    }

    public void handleMessage(byte[] bytes) {
        progress = DanaRMessages.byteArrayToInt(bytes, 0, 2);
        log.debug("remaining "+progress);
//        bolusUI.bolusDeliveredAmountSoFar = progress/100d;
//        bolusUI.bolusDelivering();
        BolusingEvent bolusingEvent = BolusingEvent.getInstance();
        bolusingEvent.sStatus = "Delivering " + bolusNumberFormat.format((amount * 100 - progress) / 100d) + "U";
        bus.post(bolusingEvent);
        MainApp.getNSClient().sendTreatmentStatusUpdate(this._id, bolusingEvent.sStatus);
    }


}
