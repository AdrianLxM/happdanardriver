package info.nightscout.danar.comm;

/**
 * Created by mike on 11.01.2016.
 */
public class MsgHistoryAllDone extends DanaRMessage {
    public boolean done = false;

    public MsgHistoryAllDone() {
        super("CMD_HISTORY_ALL_DONE");
        SetCommand((byte)SerialParam.CMD_HISTORY_ALL);
        SetSubCommand((byte) SerialParam.CMD_SUB_HISTORY_DONE);
    }
    public MsgHistoryAllDone(String cmdName) {
        super(cmdName);
    }
    public void handleMessage(byte[] bytes) {
        done = true;
    }

}
