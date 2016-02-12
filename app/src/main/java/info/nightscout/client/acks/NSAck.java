package info.nightscout.client.acks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Ack;

/**
 * Created by mike on 29.12.2015.
 */
public class NSAck implements Ack {
    public String _id = null;
    public void call(Object...args) {
        JSONArray responsearray = (JSONArray)(args[0]);
        JSONObject response = null;
        if (responsearray.length()>0) {
            try {
                response = responsearray.getJSONObject(0);
            } catch (JSONException e) {
            }
            _id = response.optString("_id");
        }
        synchronized(this) {
            this.notify();
        }
    }
}