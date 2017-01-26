package galileo.resource;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import galileo.service.ColumbusServerApplication;

public class StatusServerResource extends ServerResource {
	@Get
	public String sayHello() {
		JSONObject response = new JSONObject();
		response.put("greeting", "Hi There!");
		List<Entry<String, Integer>> hosts = ((ColumbusServerApplication) getApplication()).getAllHosts();
		Map<String, String> hostAddresses = ((ColumbusServerApplication) getApplication()).getAllHostAddresses();
		JSONArray hostStatus = new JSONArray();
		response.put("status", hostStatus);
		for (Entry<String, Integer> host : hosts) {
			try(Socket s = new Socket(hostAddresses.get(host.getKey()), host.getValue())) {
				hostStatus.put(new JSONObject().put("hostname", host.getKey()).put("port", host.getValue())
						.put("availability", "Online").put("address", hostAddresses.get(host.getKey())));
			} catch (Exception e) {
				hostStatus.put(new JSONObject().put("hostname", host.getKey()).put("port", host.getValue())
						.put("availability", "Offline").put("address", hostAddresses.get(host.getKey())));
			}
		}
		return response.toString();
	}
}