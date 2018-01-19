package net.raepple.homezone;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

public class FoscamFtplet extends DefaultFtplet {
	
	private static Logger log = LogManager.getLogger(FoscamFtplet.class);
	
	public FoscamFtplet() {
		super();		
	}
	
	@Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request) throws FtpException, IOException {
        String cameraName = session.getUser().getName();
		
		log.debug(cameraName + " logged in");
        
		// provide basic auth to authenticate at openHAB
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
			     .credentials("<username>", "<password>").build();
		
		Client client = ClientBuilder.newClient();
		client.register(feature);
		String targetUrl = "http://localhost:8080/rest/items/MotionDetector_" + cameraName + "_Camera/state";
		WebTarget motionDetectItemTarget = client.target(targetUrl);
        
		// update motion contact item to OPEN
		String command = "OPEN";
		Invocation.Builder invocationBuilder = motionDetectItemTarget.request();
		Response openHabResponse = invocationBuilder.put(Entity.entity(command, MediaType.TEXT_PLAIN));
		
		if (openHabResponse.getStatus() != HttpURLConnection.HTTP_OK) {
			log.error("Received error from REST API response: " + openHabResponse.getStatus() + " (" + openHabResponse.getStatusInfo().getReasonPhrase() + ")");
		} else {
			log.debug("Received REST API response status: " + openHabResponse.getStatus());
			log.debug("Set MotionDetector_" + cameraName + "_Camera state to " + command);
		}
			
		// continue storing the video if vacation is ON
		WebTarget vacationItemTarget = client.target("http://localhost:8080/rest/items/Vacation/state");
      
		invocationBuilder = vacationItemTarget.request();
				
		openHabResponse = invocationBuilder.get();
		String vacationState = openHabResponse.readEntity(String.class);
		log.debug("Vacation state: " + vacationState);
		
		if (vacationState.equals("ON")) {
			log.debug("Continue session");
			return super.onLogin(session, request);
		} else {
			log.debug("Disconnect session");
			return FtpletResult.DISCONNECT;
		}   
    }
}