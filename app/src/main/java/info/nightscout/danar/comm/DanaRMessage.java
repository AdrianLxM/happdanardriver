package info.nightscout.danar.comm;

import info.nightscout.danar.CRC;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/*
 *  00  01   02  03   04   05  06
 *
 *  7E  7E  len  F1  CMD  SUB data CRC CRC 2E  2E
 */

public class DanaRMessage {
    private String messageName;
    private byte[] bufferW = new byte[512];
    private byte cursorW = (byte) 6;

    public int responses = 1;
    public boolean received = false;

    public boolean failed = false;

    public void SetCommand(byte cmd) {
        this.bufferW[4] = cmd;
    }

    public void SetSubCommand(byte subcmd) {
        this.bufferW[5] = subcmd;
    }

    public void SetParamByte(byte data) {
        this.bufferW[(int) this.cursorW++] = data;
    }

    public void SetParamInt(int data) {
        this.bufferW[(int) this.cursorW++] = (byte) (data >> 8 & (int) 0xFF);
        this.bufferW[(int) this.cursorW++] = (byte) (data & (int) 0xFF);
    }

    public void SetParamDate(GregorianCalendar date) {
        SetParamByte((byte)(date.get(Calendar.YEAR) - 1900 - 100));
        SetParamByte((byte)(date.get(Calendar.MONTH) + 1));
        SetParamByte((byte)(date.get(Calendar.DAY_OF_MONTH)));
        SetParamByte((byte)(date.get(Calendar.HOUR)));
        SetParamByte((byte)(date.get(Calendar.MINUTE)));
    }

    public byte[] getMessageBytes() {
        this.bufferW[0] = (byte) 0x7E;
        this.bufferW[1] = (byte) 0x7E;

        byte length = (byte) (this.cursorW - 3);

        this.bufferW[2] = length;
        this.bufferW[3] = (byte) 0xf1;

        //Array.Copy((Array) this.bufferW, 4, (Array) new byte[(int) length], 0, (int) length);

        this.SetParamInt(CRC.getCrc16(this.bufferW, 3, length));

        this.bufferW[5 + (int) length] = (byte) 0x2E;
        this.bufferW[6 + (int) length] = (byte) 0x2E;

        return Arrays.copyOf(bufferW, length + 7);
    }

    public DanaRMessage(String cmdName) {
        this.messageName = cmdName;
    }

    public DanaRMessage() {
    }

    public String getMessageName() {
        return messageName;
    }

    public void handleMessage(byte[] bytes) {

    }

    public int getCommand() {
        int command = (bufferW[5] & 255) | ((bufferW[4] << 8) & 0xff00);
        return command;
    }

}
