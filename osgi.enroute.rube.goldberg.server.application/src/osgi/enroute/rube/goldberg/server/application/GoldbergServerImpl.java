package osgi.enroute.rube.goldberg.server.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import osgi.enroute.debug.api.Debug;
import osgi.enroute.rube.goldberg.api.server.GoldbergServer;
import osgi.enroute.rube.goldberg.api.server.GoldbergServerControl;

@Component(immediate=true,
	property = { 
		Debug.COMMAND_SCOPE + "=goldberg",
		Debug.COMMAND_FUNCTION + "=start",
		Debug.COMMAND_FUNCTION + "=list",
		RemoteConstants.SERVICE_EXPORTED_INTERFACES +"=osgi.enroute.rube.goldberg.api.server.GoldbergServer"
	})
public class GoldbergServerImpl implements GoldbergServer, GoldbergServerControl {

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
		// TODO remove here or start a timer before removing to allow for next poll call
		pollers.remove(id);
		return false;
	}

	@Override
	public void done(String id) {
		System.out.println(id+" done");
	}
	
	@Override
	public void start(String id){
		System.out.println("Start "+id);
		Thread poller = pollers.get(id);
		if(poller!=null){
			poller.interrupt();
		}
	}

	@Override
	public List<String> list(){
		ArrayList<String> contraptions = new ArrayList<String>();
		synchronized(pollers){
			for(String id : pollers.keySet()){
				contraptions.add(id);
			}
		}
		return contraptions;
	}
}
