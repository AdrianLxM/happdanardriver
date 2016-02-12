package info.nightscout.danar.comm;

/**
 * Created by mike on 11.01.2016.
 */
public class MsgPCCommStart extends DanaRMessage {
    public MsgPCCommStart() {
        super("CMD_CONNECT");
        SetCommand(SerialParam.CTRL_CMD_COMM);
        SetSubCommand(SerialParam.CTRL_SUB_COMM_CONNECT);
    }
    public MsgPCCommStart(String cmdName) {
        super(cmdName);
    }
}
