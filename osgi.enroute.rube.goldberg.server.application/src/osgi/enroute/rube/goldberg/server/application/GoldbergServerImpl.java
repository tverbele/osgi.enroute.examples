package osgi.enroute.rube.goldberg.server.application;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.rube.goldberg.api.server.GoldbergServer;

@Component(immediate=true,
	property = { 
		Debug.COMMAND_SCOPE + "=goldberg",
		Debug.COMMAND_FUNCTION + "=start",
		Debug.COMMAND_FUNCTION + "=list",
		RemoteConstants.SERVICE_EXPORTED_INTERFACES +"=osgi.enroute.rube.goldberg.api.server.GoldbergServer"
	})
public class GoldbergServerImpl implements GoldbergServer {

	private static final int POLL_TIMEOUT = 60000;
	
	private Map<String, Thread> pollers = Collections.synchronizedMap(new HashMap<String, Thread>());
	
	@Override
	public boolean poll(String id) {
		pollers.put(id, Thread.currentThread());
		try {
			Thread.sleep(POLL_TIMEOUT);
		} catch (InterruptedException e) {
			return true;
		}
		return false;
	}

	@Override
	public void done(String id) {
		System.out.println(id+" done");
	}
	
	public void start(String id){
		System.out.println("Start "+id);
		Thread poller = pollers.get(id);
		if(poller!=null){
			poller.interrupt();
		}
	}

	public void list(){
		synchronized(pollers){
			for(String id : pollers.keySet()){
				System.out.println("* "+id);
			}
		}
	}
}
