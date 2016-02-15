package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 07.02.2016.
 */
public class MsgExtendedBolusStop extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusStart.class);
    private String _id;

    public MsgExtendedBolusStop() {
        super("CMD_PUMPSET_EXPANS_INS_STOP");
        SetCommand(SerialParam.CTRL_CMD_EB);
        SetSubCommand(SerialParam.CTRL_SUB_EB_STOP);
    }

    public MsgExtendedBolusStop(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
        received = true;
        int result = DanaRMessages.byteArrayToInt(bytes, 0, 1);
        log.info("MsgExtendedBolusStop result " + result);
        if(result!=1) {
            failed = true;
            log.error("Command response is not OK " + getMessageName());
        }
    }


}
