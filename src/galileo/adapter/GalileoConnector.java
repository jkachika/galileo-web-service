package galileo.adapter;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.resource.ResourceException;

import galileo.client.EventPublisher;
import galileo.comm.GalileoEventMap;
import galileo.event.BasicEventWrapper;
import galileo.event.Event;
import galileo.event.EventWrapper;
import galileo.net.ClientMessageRouter;
import galileo.net.GalileoMessage;
import galileo.net.MessageListener;
import galileo.net.NetworkDestination;

public class GalileoConnector implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(GalileoConnector.class.getName());
	private static GalileoEventMap eventMap = new GalileoEventMap();
	private static EventWrapper wrapper = new BasicEventWrapper(eventMap);
	private String galileoIP;
	private int galileoPort;
	private ClientMessageRouter messageRouter;
	private NetworkDestination server;
	private Event response;
	private CountDownLatch latch;

	public GalileoConnector(String ip, int port) throws IOException {
		this.galileoIP = ip;
		this.galileoPort = port;
		this.messageRouter = new ClientMessageRouter();
		this.messageRouter.addListener(this);
		this.server = new NetworkDestination(galileoIP, galileoPort);
		this.latch = new CountDownLatch(1);
	}

	public Event sendMessage(Event request) throws IOException {
		messageRouter.sendMessage(server, EventPublisher.wrapEvent(request));
		LOGGER.fine("Request sent. Waiting for response");
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return response;
	}

	@Override
	public void onMessage(GalileoMessage message) {
		try {
			LOGGER.fine("Obtained response from Galileo");
			this.response = wrapper.unwrap(message);
			this.latch.countDown();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceException(e.getCause());
		}
	}

	public void close() {
		try {
			this.messageRouter.shutdown();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to shutdown the router", e);
		}
	}

	@Override
	public void onConnect(NetworkDestination arg0) {
		// TODO Auto-generated method stub
		LOGGER.fine("Successfully connected to Galileo");
	}

	@Override
	public void onDisconnect(NetworkDestination arg0) {
		// TODO Auto-generated method stub
		LOGGER.fine("Disconnected from galileo");
	}

}
