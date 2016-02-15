package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 07.02.2016.
 */
public class MsgExtendedBolusStart  extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusStart.class);
    private String _id;

    public MsgExtendedBolusStart(double amount, byte halfhours) {
        super("CMD_PUMPSET_EXPANS_INS_S");
        SetCommand(SerialParam.CTRL_CMD_EB);
        SetSubCommand(SerialParam.CTRL_SUB_EB_START);
        SetParamInt((int) (amount * 100));
        SetParamByte(halfhours);
    }

    public MsgExtendedBolusStart(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
        received = true;
        int result = DanaRMessages.byteArrayToInt(bytes, 0, 1);
        log.info("MsgExtendedBolusStart result " + result);
        if(result!=1) {
            failed = true;
            log.error("Command response is not OK " + getMessageName());
        }
    }


}
