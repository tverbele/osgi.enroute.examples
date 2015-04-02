package osgi.enroute.rube.goldberg.api.server;

import java.util.List;

public interface GoldbergServerControl {

	public void start(String id);
	
	public List<String> list();
	
}
