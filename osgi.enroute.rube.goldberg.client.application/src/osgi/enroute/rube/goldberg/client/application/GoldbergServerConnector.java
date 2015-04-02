package osgi.enroute.rube.goldberg.client.application;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import osgi.enroute.rube.goldberg.api.camera.listener.CameraListener;
import osgi.enroute.rube.goldberg.api.server.GoldbergServer;

@Component
public class GoldbergServerConnector {

	private RemoteServiceAdmin rsa;
	
	private ImportRegistration serverImport;
	private ImportRegistration listenerImport;

	
	private String uri = "r-osgi://127.0.0.1:9278";
	
	// TODO check for r-osgi exported config? kinda hard coded for now
	@Reference
	public void setRemoteServiceAdmin(RemoteServiceAdmin rsa){
		this.rsa = rsa;
	}
	
	@Activate
	public void activate(){
		Map<String, Object> serverEndpointProps = new HashMap<String, Object>();
		serverEndpointProps.put("endpoint.id", uri);
		serverEndpointProps.put("service.imported.configs", "be.iminds.aiolos.r-osgi");
		serverEndpointProps.put("objectClass", new String[]{GoldbergServer.class.getName()});
		EndpointDescription serverEndpoint = new EndpointDescription(serverEndpointProps);
		
		serverImport = rsa.importService(serverEndpoint);
		
		if(serverImport.getException()!=null){
			System.err.println("Error connecting to Goldberg server");
			serverImport.getException().printStackTrace();
		}
		
		// already import camera listener as well?
		Map<String, Object> listenerEndpointProps = new HashMap<String, Object>();
		listenerEndpointProps.put("endpoint.id", uri);
		listenerEndpointProps.put("service.imported.configs", "be.iminds.aiolos.r-osgi");
		listenerEndpointProps.put("objectClass", new String[]{CameraListener.class.getName()});
		EndpointDescription listenerEndpoint = new EndpointDescription(listenerEndpointProps);
		
		listenerImport = rsa.importService(listenerEndpoint);
		
		if(listenerImport.getException()!=null){
			System.err.println("Error connecting to Goldberg server Camera listener");
			listenerImport.getException().printStackTrace();
		}
	}
	
	@Deactivate
	public void deactivate(){
		serverImport.close();
		listenerImport.close();
	}
}
