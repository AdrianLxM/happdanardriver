package info.nightscout.danar.comm;

/**
 * Created by mike on 11.01.2016.
 */
public class MsgPCCommStop extends DanaRMessage {
    public MsgPCCommStop() {
        super("CMD_CMD_DISCONNECT");
        SetCommand(SerialParam.CTRL_CMD_COMM);
        SetSubCommand(SerialParam.CTRL_SUB_COMM_DISCONNECT);
    }
    public MsgPCCommStop(String cmdName) {
        super(cmdName);
    }
}
