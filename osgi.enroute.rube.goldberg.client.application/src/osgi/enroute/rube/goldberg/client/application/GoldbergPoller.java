package osgi.enroute.rube.goldberg.client.application;

import org.osgi.framework.ServiceException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.rube.goldberg.api.GoldbergContraption;
import osgi.enroute.rube.goldberg.api.camera.Camera;
import osgi.enroute.rube.goldberg.api.server.GoldbergServer;

/**
 * This does the housekeeping for one Rube Goldberg contraption. It will poll the server
 * to check whether it can start. Once started it will run the actual contraption, until
 * this one is finished and then the server is notified again.
 * 
 * @author Tim Verbelen
 *
 */
@Component
public class GoldbergPoller {

	private GoldbergServer server;
	private GoldbergContraption contraption;
	private Camera camera;
	
	private volatile boolean polling = false;
	
	@Reference
	public void setGoldbergServer(GoldbergServer g){
		this.server = g;
	}
	
	@Reference
	public void setGoldbergContraption(GoldbergContraption c){
		this.contraption = c;
	}
	
	@Reference
	public void setCamera(Camera c){
		this.camera = c;
	}
	
	@Activate
	public void activate(){
		polling = true;
		Thread pollThread = new Thread(new Runnable(){
			public void run(){
				while(polling){
					// poll server ...
					try {
						boolean start = server.poll(contraption.getId());
						if(start){
							// start camera stream
							camera.start();
							
							// run contraption
							contraption.run();
							server.done(contraption.getId());
							
							// stop camera stream
							camera.stop();
						}
					} catch(ServiceException e){
						// polling server failed
						polling = false;
						
					}
				}
			}
		});
		pollThread.start();
	}
	
	@Deactivate
	public void deactivate(){
		polling = false;
	}
}
