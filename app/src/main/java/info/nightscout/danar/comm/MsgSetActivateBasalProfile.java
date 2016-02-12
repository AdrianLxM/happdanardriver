package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 07.02.2016.
 */
public class MsgSetActivateBasalProfile extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusStart.class);
    private String _id;

    // index 0-3
    public MsgSetActivateBasalProfile(byte index) {
        super("CMD_SETTING_PROFILE_NUMBER_S");
        SetCommand(SerialParam.CMD_SET);
        SetSubCommand(SerialParam.CMD_SUB_SET_ACTIVATE_BASAL_PROFILE);
        SetParamByte(index);
    }

    public MsgSetActivateBasalProfile(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
        int result = DanaRMessages.byteArrayToInt(bytes, 0, 1);
        log.info("Activate basal profile result " + result);
        if(result !=1 ) {
            failed = true;
            log.error("Command response is not OK " + getMessageName());
        }
    }
}
