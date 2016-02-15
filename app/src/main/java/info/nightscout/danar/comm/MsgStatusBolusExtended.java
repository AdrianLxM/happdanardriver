package info.nightscout.danar.comm;

import info.nightscout.happdanardriver.MainApp;
import info.nightscout.danar.event.StatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgStatusBolusExtended extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgStatusBolusExtended.class);

    public MsgStatusBolusExtended() {
        super("CMD_PUMP_EXPANS_INS_I");
        SetCommand(SerialParam.CTRL_CMD_STATUS);
        SetSubCommand(SerialParam.CTRL_SUB_STATUS_EXTBOLUS);
    }

    public MsgStatusBolusExtended(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
        received = true;
        int statusBolusExtendedInProgress = DanaRMessages.byteArrayToInt(bytes, 0, 1);
        int statusBolusExtendedDurationInHalfHours = DanaRMessages.byteArrayToInt(bytes, 1, 1);
        int statusBolusExtendedDurationInMinutes = statusBolusExtendedDurationInHalfHours * 30;

        double statusBolusExtendedPlannedAmount = DanaRMessages.byteArrayToInt(bytes, 2, 2) *0.01d;
        int statusBolusExtendedDurationSoFarInSecs = DanaRMessages.byteArrayToInt(bytes, 4, 3);
        int statusBolusExtendedDurationSoFarInMinutes = statusBolusExtendedDurationSoFarInSecs / 60;

        StatusEvent ev = StatusEvent.getInstance();
        ev.statusBolusExtendedInProgress =  statusBolusExtendedInProgress != 0;
        ev.statusBolusExtendedPlannedAmount = ev.statusBolusExtendedInProgress ? statusBolusExtendedPlannedAmount : 0;
        ev.statusBolusExtendedDurationInMinutes = ev.statusBolusExtendedInProgress ? statusBolusExtendedDurationInMinutes : 0;
        ev.statusBolusExtendedDurationSoFarInMinutes = ev.statusBolusExtendedInProgress ? statusBolusExtendedDurationSoFarInMinutes : 0;

        MainApp.bus().post(ev);

        log.debug("statusBolusExtendedInProgress:"+statusBolusExtendedInProgress
                        + " statusBolusExtendedDurationInMinutes:"+statusBolusExtendedDurationInMinutes
                        + " statusBolusExtendedPlannedAmount:"+statusBolusExtendedPlannedAmount
                        + " statusBolusExtendedDurationSoFarInMinutes:"+statusBolusExtendedDurationSoFarInMinutes
        );
    }

}
