package galileo.resource;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import galileo.adapter.GalileoConnector;
import galileo.event.Event;
import galileo.service.ColumbusServerApplication;

public class ColumbusServerResource extends ServerResource {

	private static final Logger LOGGER = Logger.getLogger(ColumbusServerResource.class.getName());
	private GalileoConnector connector;

	public ColumbusServerResource() {
		setNegotiated(false);
	}

	/**
	 * Implements the functionality that needs to be executed for every incoming
	 * request
	 */
	@Override
	protected void doInit() throws ResourceException {
		try {
			super.doInit();
			Entry<String, Integer> server = ((ColumbusServerApplication) getApplication()).getServerAddress();
			LOGGER.info("Galileo Host: " + server.getKey() + ":" + server.getValue());
			this.connector = new GalileoConnector(server.getKey(), server.getValue());
		} catch (IOException ioe) {
			throw new ResourceException(ioe.getCause());
		}
	}

	@Override
	protected void doRelease() throws ResourceException {
		LOGGER.fine("Closing the connection to Galileo");
		this.connector.close();
		super.doRelease();
	}

	public Event sendMessage(Event request) throws IOException {
		return this.connector.sendMessage(request);
	}
}