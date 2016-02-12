package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 07.02.2016.
 */
public class MsgSetBasalProfile extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgBolusStart.class);
    private String _id;

    // index 0-3
    public MsgSetBasalProfile(byte index, double[] values) {
        super("CMD_SETTING_BASAL_PROFILE_S");
        SetCommand(SerialParam.CMD_SET);
        SetSubCommand(SerialParam.CMD_SUB_SET_BASAL_PROFILE);
        SetParamByte(index);
        for( Integer i=0; i < 24; i++) {
            SetParamInt((int)(values[i] * 100));
        }

    }

    public MsgSetBasalProfile(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
        int result = DanaRMessages.byteArrayToInt(bytes, 0, 1);
        log.info("Set basal profile result " + result);
        if(result !=1 ) {
            failed = true;
            log.error("Command response is not OK " + getMessageName());
        }
    }


}
