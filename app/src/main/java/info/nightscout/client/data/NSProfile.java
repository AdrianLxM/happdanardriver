package info.nightscout.client.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by mike on 25.01.2016.
 */
public class NSProfile {
    private JSONObject json = null;
    private String activeProfile = null;

    public NSProfile(JSONObject json, String activeProfile) {
        this.json = json;
        this.activeProfile = activeProfile;
    }

    JSONObject getDefaultProfile() {
        String defaultProfileName = null;
        JSONObject store;
        JSONObject profile = null;
        try {
            defaultProfileName = (String) json.get("defaultProfile");
            store = json.getJSONObject("store");
            if (activeProfile != null && store.has(activeProfile)) {
            defaultProfileName = activeProfile;
            }
            profile = store.getJSONObject(defaultProfileName);
        } catch (JSONException e) {
        }

        return profile;
    }

    public String log() {
        String ret = "\n";
        for (Integer hour = 0; hour < 24; hour ++) {
            double value = getBasal(hour * 60 * 60);
            ret += "NS basal value for " + hour + ":00 is " + value + "\n";
        }
        ret += "NS units: " + getUnits();
        return ret;
    }

    public Double getDia() {
        Double dia;
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                dia = profile.getDouble("dia");
                return dia;
            } catch (JSONException e) {
            }
        }
        return 3D;
    }

   public Double getCarbAbsorbtionRate() {
        Double carbAbsorptionRate;
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                carbAbsorptionRate = profile.getDouble("carbs_hr");
                return carbAbsorptionRate;
            } catch (JSONException e) {
            }
        }
       return 0D;
    }

    // mmol or mg/dl
   public String getUnits() {
       String units;
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                units = profile.getString("units");
                return units;
            } catch (JSONException e) {
            }
        }
       return "mg/dl";
    }

   public TimeZone getTimeZone() {
       TimeZone timeZone;
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return TimeZone.getTimeZone(profile.getString("timezone"));
            } catch (JSONException e) {
            }
        }
       return TimeZone.getDefault();
    }

    public Double getValueToTime(JSONArray array, Integer timeAsSeconds) {
        Double lastValue = null;
        for(Integer index = 0; index < array.length(); index++) {
            try {
                JSONObject o = array.getJSONObject(index);
                Integer tas = o.getInt("timeAsSeconds");
                Double value = o.getDouble("value");
                if (lastValue == null) lastValue = value;
                if (timeAsSeconds < tas) {
                    break;
                }
                lastValue = value;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return lastValue;
    }

    public Double getIsf(Integer timeAsSeconds) {
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return getValueToTime(profile.getJSONArray("sens"),timeAsSeconds);
            } catch (JSONException e) {
            }
        }
        return 0D;
    }

    public Double getIc(Integer timeAsSeconds) {
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return getValueToTime(profile.getJSONArray("carbratio"),timeAsSeconds);
            } catch (JSONException e) {
            }
        }
        return 0D;
    }

    public Double getBasal(Integer timeAsSeconds) {
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return getValueToTime(profile.getJSONArray("basal"),timeAsSeconds);
            } catch (JSONException e) {
            }
        }
        return 0D;
    }

    public Double getTargetLow(Integer timeAsSeconds) {
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return getValueToTime(profile.getJSONArray("target_low"),timeAsSeconds);
            } catch (JSONException e) {
            }
        }
        return 0D;
    }

    public Double getTargetHigh(Integer timeAsSeconds) {
        JSONObject profile = getDefaultProfile();
        if (profile != null) {
            try {
                return getValueToTime(profile.getJSONArray("target_high"), timeAsSeconds);
            } catch (JSONException e) {
            }
        }
        return 0D;
    }
}
