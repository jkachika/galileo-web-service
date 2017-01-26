package galileo.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Header;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.engine.application.CorsFilter;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.FileRepresentation;
import org.restlet.routing.Router;
import org.restlet.util.Series;

import galileo.resource.BlocksServerResource;
import galileo.resource.FeaturesServerResource;
import galileo.resource.FeaturesetServerResource;
import galileo.resource.FilesystemServerResource;
import galileo.resource.StatusServerResource;

/**
 * The server application for Galileo - exposes the functionality of Galileo as
 * a web service.
 * 
 * @author jkachika
 *
 */
public class ColumbusServerApplication extends Application {
	private static final Logger LOGGER = Logger.getLogger(ColumbusServerApplication.class.getName());
	private List<Entry<String, Integer>> hostnames;
	private Map<String, String> hostAddresses;
	private int currentServer;

	public ColumbusServerApplication() {
		setName("Galileo REST Service for Columbus");
		setDescription("A web service for Galileo");
		setOwner("Johnson Kachikaran, Colorado State University");
		setAuthor("Johnson Kachikaran, johnsoncharles26@gmail.com");
	}

	public void setup() {
		try {
			ServletContext servlet = (ServletContext) getContext().getAttributes()
					.get("org.restlet.ext.servlet.ServletContext");
			// Get the path of the config file relative to the WAR
			String rootPath = servlet.getRealPath("/WEB-INF/hostnames");
			Path path = Paths.get(rootPath);
			File configFile = new File(path.toString());
			FileRepresentation hostsfile = new FileRepresentation(configFile, MediaType.TEXT_PLAIN);
			String hosts = hostsfile.getText();
			String[] hostnames = hosts.split("\\r?\\n");
			this.hostnames = new ArrayList<>();
			Map<String, Integer> hostMap = new HashMap<>();
			for (String hostname : hostnames) {
				String[] host = hostname.split(":");
				hostMap.put(host[0], Integer.parseInt(host[1]));
			}
			this.hostnames.addAll(hostMap.entrySet());
			this.hostAddresses = new HashMap<String, String>();
			for(Entry<String, Integer> host: this.hostnames){
				try{
					this.hostAddresses.put(host.getKey(), InetAddress.getByName(host.getKey()).getHostAddress());
				} catch(Exception uhe){
					this.hostAddresses.put(host.getKey(), host.getKey());
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to read the hostnames", e);
		}
	}
	
	public static String getFailureResponse(String request, String message){
		JSONObject failResponse = new JSONObject();
		failResponse.put("request", request);
		failResponse.put("status", "error");
		failResponse.put("reason", message);
		return failResponse.toString();
	}

	public Entry<String, Integer> getServerAddress() {
		if (this.currentServer >= this.hostnames.size())
			this.currentServer = 0;
		return this.hostnames.get(this.currentServer++);
	}

	public List<Entry<String, Integer>> getAllHosts() {
		return Collections.unmodifiableList(this.hostnames);
	}
	
	public Map<String, String> getAllHostAddresses(){
		return Collections.unmodifiableMap(this.hostAddresses);
	}

	/**
	 * Helps to trace, block or route the incoming requests.
	 */
	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		CorsFilter corsFilter = new CorsFilter(getContext(), router) {
			@SuppressWarnings("unchecked")
			@Override
			protected int beforeHandle(Request request, Response response) {

				Series<Header> responseHeaders = (Series<Header>) response.getAttributes()
						.get(HeaderConstants.ATTRIBUTE_HEADERS);
				if (responseHeaders == null) {
					responseHeaders = new Series<Header>(Header.class);
				}

				// Request headers

				Series<Header> reqHeaders = (Series<Header>) request.getAttributes()
						.get(HeaderConstants.ATTRIBUTE_HEADERS);
				String requestOrigin = reqHeaders.getFirstValue("Origin", false, "*");
				Set<Method> methods = new HashSet<Method>();
				methods.add(Method.ALL);
				response.setAccessControlAllowOrigin(requestOrigin);
				response.setAccessControlAllowCredentials(true);
				response.setAccessControlAllowMethods(methods);

				return super.beforeHandle(request, response);
			}
		};

		router.attach("/status", StatusServerResource.class);
		router.attach("/features", FeaturesServerResource.class);
		router.attach("/blocks", BlocksServerResource.class);
		router.attach("/featureset", FeaturesetServerResource.class);
		router.attach("/filesystem", FilesystemServerResource.class);
		setup();
		return corsFilter;
	}

}
