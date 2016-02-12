package info.nightscout.danar.comm;

import com.j256.ormlite.dao.Dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;

import info.nightscout.happdanardriver.MainApp;
import info.nightscout.danar.db.HistoryRecord;

/**
 * Created by mike on 11.01.2016.
 */
public class MsgHistoryAll extends DanaRMessage {
    private static Logger log = LoggerFactory.getLogger(MsgHistoryAll.class);
    public MsgHistoryAll() {
        super("CMD_HISTORY_ALL");
        SetCommand((byte)SerialParam.CMD_HISTORY_ALL);
        SetSubCommand((byte)SerialParam.CMD_SUB_HISTORY_START);
    }
    public MsgHistoryAll(String cmdName) {
        super(cmdName);
    }
    public void handleMessage(byte[] bytes) {
        byte recordCode = (byte) DanaRMessages.byteArrayToInt(bytes,0,1);
        Date date = DanaRMessages.byteArrayToDate(bytes, 1);                     // 3 bytes
        Date datetime = DanaRMessages.byteArrayToDateTime(bytes, 1);             // 5 bytes
        Date datetimewihtsec = DanaRMessages.byteArrayToDateTimeSec(bytes, 1);   // 6 bytes

        byte paramByte5 = (byte) DanaRMessages.byteArrayToInt(bytes,4,1);
        byte paramByte6 = (byte) DanaRMessages.byteArrayToInt(bytes,5,1);
        byte paramByte7 = (byte) DanaRMessages.byteArrayToInt(bytes,6,1);
        byte paramByte8 = (byte) DanaRMessages.byteArrayToInt(bytes,7,1);
        double value = (double) DanaRMessages.byteArrayToInt(bytes,8,2);

        HistoryRecord historyRecord = new HistoryRecord();

        historyRecord.setRecordCode(recordCode);
        historyRecord.setBytes(bytes);

        switch (recordCode){
            case SerialParam.RECORD_TYPE_BOLUS:
                historyRecord.setRecordDate(datetime);
                switch (0xF0 & paramByte8)
                {
                    case 0xA0:
                        historyRecord.setBolusType("DS");
                        break;
                    case 0xC0:
                        historyRecord.setBolusType("E");
                        break;
                    case 0x80:
                        historyRecord.setBolusType("S");
                        break;
                    case 0x90:
                        historyRecord.setBolusType("DE");
                        break;
                    default:
                        historyRecord.setBolusType("None");
                        break;
                }
                historyRecord.setRecordDuration(((int) paramByte8 & 0x0F) * 60 + (int) paramByte7);
                historyRecord.setRecordValue(value * 0.01);
                break;
            case SerialParam.RECORD_TYPE_DAILY:
                historyRecord.setRecordDate(date);
                historyRecord.setRecordDailyBasal((double) ((int) paramByte5 * 0xFF + (int) paramByte6) * 0.01);
                historyRecord.setRecordDailyBolus((double) ((int) paramByte7 * 0xFF + (int) paramByte8) / 0.01);
                break;
            case SerialParam.RECORD_TYPE_PRIME:
            case SerialParam.RECORD_TYPE_ERROR:
            case SerialParam.RECORD_TYPE_REFILL:
            case SerialParam.RECORD_TYPE_BASALHOUR:
            case SerialParam.RECORD_TYPE_TB:
                historyRecord.setRecordDate(datetimewihtsec);
                historyRecord.setRecordValue(value * 0.01);
                break;
            case SerialParam.RECORD_TYPE_GLUCOSE:
            case SerialParam.RECORD_TYPE_CARBO:
                historyRecord.setRecordDate(datetimewihtsec);
                historyRecord.setRecordValue(value);
                break;
            case SerialParam.RECORD_TYPE_ALARM:
                historyRecord.setRecordDate(datetimewihtsec);
                String strAlarm = "None";
                switch ((int) paramByte8) {
                    case 67:
                        strAlarm = "Check";
                        break;
                    case 79:
                        strAlarm = "Occlusion";
                        break;
                    case 66:
                        strAlarm = "Low Battery";
                        break;
                    case 83:
                        strAlarm = "Shutdown";
                        break;
                }
                historyRecord.setRecordAlarm(strAlarm);
                historyRecord.setRecordValue(value * 0.01);
                break;
            case SerialParam.RECORD_TYPE_SUSPEND:
                historyRecord.setRecordDate(datetimewihtsec);
                String strRecordValue = "Off";
                if ((int) paramByte8 == 79)
                    strRecordValue = "On";
                historyRecord.setStringRecordValue(strRecordValue);
                break;
        }

        try {
            Dao<HistoryRecord, String> daoHistoryRecords = MainApp.getDbHelper().getDaoHistoryRecords();
            daoHistoryRecords.createIfNotExists(historyRecord);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return;

    }


}
