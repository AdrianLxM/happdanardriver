package info.nightscout.danar.event;

/**
 * Created by mike on 27.12.2015.
 */
public class CommandEvent {
    public String sCommand = "";

    public CommandEvent(String command) {
        sCommand = command;
    }

    public CommandEvent() {

    }
}
