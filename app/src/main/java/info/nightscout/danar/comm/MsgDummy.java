package info.nightscout.danar.comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsgDummy extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgDummy.class);

    public MsgDummy() {
        super("CMD_DUMMY");
    }

    public MsgDummy(String cmdName) {
        super(cmdName);
    }

    public void handleMessage(byte[] bytes) {
    }
}
