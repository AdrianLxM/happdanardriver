package info.nightscout.client;

import com.j256.ormlite.dao.Dao;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Calendar;

import info.nightscout.client.acks.NSAck;
import info.nightscout.client.utils.DateUtil;
import info.nightscout.danar.comm.SerialParam;
import info.nightscout.danar.db.HistoryRecord;

/**
 * Created by mike on 18.01.2016.
 */
public class NSHistorySync {
    private static Logger log = LoggerFactory.getLogger(NSHistorySync.class);
    private Dao<HistoryRecord, String> daoHistoryRecords;
    private NSClient nsClient;

    public NSHistorySync(Dao<HistoryRecord, String> daoHistoryRecords, NSClient nsClient) {
        this.daoHistoryRecords = daoHistoryRecords;
        this.nsClient = nsClient;
    }


    public void sync(){
        try {
            Calendar cal = Calendar.getInstance();
            log.debug("Database contains " + daoHistoryRecords.countOf() + " records");
            for (HistoryRecord record : daoHistoryRecords) {
                if (record.get_id() != null) continue;
                //log.debug(record.getBytes());
                JSONObject nsrec = new JSONObject();
                NSAck nsAck = new NSAck();
                switch (record.getRecordCode()) {
                    case SerialParam.RECORD_TYPE_BOLUS:
                        switch (record.getBolusType()) {
                            case "S":
                                log.debug("Syncing standard bolus record " + record.getRecordValue() + "U " + DateUtil.toISOString(record.getRecordDate()));
                                nsrec.put("eventType", "Meal Bolus");
                                nsrec.put("insulin", record.getRecordValue());
                                nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                                nsrec.put("enteredBy", "DanaR/Sync");
                                nsClient.sendAddTreatment(nsrec, nsAck);
                                record.set_id(nsAck._id);
                                daoHistoryRecords.update(record);

                                //HistoryRecord ver = daoHistoryRecords.queryForId(record.getBytes());
                                //log.debug(ver.get_id());
                                break;
                            case "E":
                                if (record.getRecordDuration() > 0) {
                                    log.debug("Syncing extended bolus record " + record.getRecordValue() + "U " + DateUtil.toISOString(record.getRecordDate()));
                                    nsrec.put("eventType", "Combo Bolus");
                                    nsrec.put("insulin", 0);
                                    nsrec.put("duration", record.getRecordDuration());
                                    nsrec.put("relative", record.getRecordValue() / record.getRecordDuration() * 60);
                                    nsrec.put("splitNow", 0);
                                    nsrec.put("splitExt", 100);
                                    cal.setTime(record.getRecordDate());
                                    cal.add(Calendar.MINUTE, -1 * record.getRecordDuration());
                                    nsrec.put("created_at", DateUtil.toISOString(cal.getTime()));
                                    nsrec.put("enteredBy", "DanaR/Sync");
                                    nsClient.sendAddTreatment(nsrec, nsAck);
                                    record.set_id(nsAck._id);
                                    daoHistoryRecords.update(record);
                                } else {
                                    log.debug("NOT Syncing extended bolus record " + record.getRecordValue() + "U " + DateUtil.toISOString(record.getRecordDate()) + " zero duration");
                                }
                                break;
                            case "DS":
                                log.debug("Syncing dual(S) bolus record " + record.getRecordValue() + "U " + DateUtil.toISOString(record.getRecordDate()));
                                nsrec.put("eventType", "Combo Bolus");
                                nsrec.put("insulin", record.getRecordValue());
                                nsrec.put("splitNow", 100);
                                nsrec.put("splitExt", 0);
                                nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                                nsrec.put("enteredBy", "DanaR/Sync");
                                nsClient.sendAddTreatment(nsrec, nsAck);
                                record.set_id(nsAck._id);
                                daoHistoryRecords.update(record);
                                break;
                            case "DE":
                                log.debug("Syncing dual(E) bolus record " + record.getRecordValue() + "U " + DateUtil.toISOString(record.getRecordDate()));
                                nsrec.put("eventType", "Combo Bolus");
                                nsrec.put("duration", record.getRecordDuration());
                                nsrec.put("relative", record.getRecordValue() / record.getRecordDuration() * 60);
                                nsrec.put("splitNow", 0);
                                nsrec.put("splitExt", 100);
                                cal.setTime(record.getRecordDate());
                                cal.add(Calendar.MINUTE, -1 * record.getRecordDuration());
                                nsrec.put("created_at", DateUtil.toISOString(cal.getTime()));
                                nsrec.put("enteredBy", "DanaR/Sync");
                                nsClient.sendAddTreatment(nsrec, nsAck);
                                record.set_id(nsAck._id);
                                daoHistoryRecords.update(record);

                                //HistoryRecord ver = daoHistoryRecords.queryForId(record.getBytes());
                                //log.debug(ver.get_id());
                                break;
                            default:
                                log.debug("Unknown bolus record");
                                break;
                        }
                        break;
                    case SerialParam.RECORD_TYPE_ERROR:
                        log.debug("Syncing error record " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "Note");
                        nsrec.put("notes", "Error");
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_REFILL:
                        log.debug("Syncing refill record " + record.getRecordValue() + " " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "Insulin Change");
                        nsrec.put("notes", "Refill " + record.getRecordValue() + "U");
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_BASALHOUR:
                        log.debug("Syncing basal hour record " + record.getRecordValue() + " " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "Temp Basal");
                        nsrec.put("absolute", record.getRecordValue());
                        nsrec.put("duration", 60);
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_TB:
                        //log.debug("Ignoring TB record " + record.getBytes() + " " + DateUtil.toISOString(record.getRecordDate()));
                        break;
                    case SerialParam.RECORD_TYPE_GLUCOSE:
                        log.debug("Syncing glucose record " + record.getRecordValue() + " " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "BG Check");
                        nsrec.put("glucose", record.getRecordValue());
                        nsrec.put("glucoseType", "Finger");
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_CARBO:
                        log.debug("Syncing carbo record " + record.getRecordValue() + "g " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "Meal Bolus");
                        nsrec.put("carbs", record.getRecordValue());
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_ALARM:
                        log.debug("Syncing alarm record " + record.getRecordAlarm() + " " + DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("eventType", "Note");
                        nsrec.put("notes", "Alarm: " + record.getRecordAlarm());
                        nsrec.put("created_at", DateUtil.toISOString(record.getRecordDate()));
                        nsrec.put("enteredBy", "DanaR/Sync");
                        nsClient.sendAddTreatment(nsrec, nsAck);
                        record.set_id(nsAck._id);
                        daoHistoryRecords.update(record);
                        break;
                    case SerialParam.RECORD_TYPE_DAILY:
                    case SerialParam.RECORD_TYPE_PRIME:
                    case SerialParam.RECORD_TYPE_SUSPEND:
                        // Ignore
                        break;
                    default:
                        log.error("Unknown record type");
                        break;
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
}
