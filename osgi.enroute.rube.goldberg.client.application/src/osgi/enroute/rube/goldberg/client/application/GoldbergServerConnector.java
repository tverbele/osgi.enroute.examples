package osgi.enroute.rube.goldberg.client.application;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.rube.goldberg.api.camera.listener.CameraListener;
import osgi.enroute.rube.goldberg.api.server.GoldbergServer;

@ConfigurerExtender
@Component(name="osgi.enroute.rube.goldberg.server.connector", 
	configurationPolicy=ConfigurationPolicy.REQUIRE)
public class GoldbergServerConnector {

	private RemoteServiceAdmin rsa;
	
	private ImportRegistration serverImport;
	private ImportRegistration listenerImport;
	
	private String uri = "r-osgi://127.0.0.1:9278";
	
	private Thread connectorThread;
	private volatile boolean running = false;
	
	// TODO check for r-osgi exported config? kinda hard coded for now
	@Reference
	public void setRemoteServiceAdmin(RemoteServiceAdmin rsa){
		this.rsa = rsa;
	}
	
	@Activate
	public void activate(Map<String, String> config){
		uri = config.get("uri");
		// TODO check uri for correctness
		
		running = true;
		connectorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running){
					if(serverImport==null || serverImport.getImportReference()==null){
						Map<String, Object> serverEndpointProps = new HashMap<String, Object>();
						serverEndpointProps.put("endpoint.id", uri);
						serverEndpointProps.put("service.imported.configs", "be.iminds.aiolos.r-osgi");
						serverEndpointProps.put("objectClass", new String[]{GoldbergServer.class.getName()});
						EndpointDescription serverEndpoint = new EndpointDescription(serverEndpointProps);
						
						serverImport = rsa.importService(serverEndpoint);
						
						if(serverImport.getException()!=null){
							serverImport.close();
							serverImport = null;
						}
						
						// already import camera listener as well?
						Map<String, Object> listenerEndpointProps = new HashMap<String, Object>();
						listenerEndpointProps.put("endpoint.id", uri);
						listenerEndpointProps.put("service.imported.configs", "be.iminds.aiolos.r-osgi");
						listenerEndpointProps.put("objectClass", new String[]{CameraListener.class.getName()});
						EndpointDescription listenerEndpoint = new EndpointDescription(listenerEndpointProps);
						
						listenerImport = rsa.importService(listenerEndpoint);
						
						if(listenerImport.getException()!=null){
							listenerImport.close();
							listenerImport = null;
						}
					} 
					
					try {
						// check once every minute...
						Thread.sleep(60000);
					} catch (InterruptedException e) {
					}
				}
				
				if(serverImport!=null){
					serverImport.close();
					listenerImport.close();
				}
			}
		});
		connectorThread.start();
		
	}
	
	@Deactivate
	public void deactivate(){
		running = false;
		connectorThread.interrupt();
		try {
			connectorThread.join();
		} catch (InterruptedException e) {
		}
	}
}
