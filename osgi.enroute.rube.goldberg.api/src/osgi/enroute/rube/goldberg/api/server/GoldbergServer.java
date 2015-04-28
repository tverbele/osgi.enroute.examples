package osgi.enroute.rube.goldberg.api.server;

/**
 * Interface for the Goldberg server, each Rube Goldberg contraption will poll the server
 * to check when it can start. Once started the contraption calls done() when finished.
 * 
 * @author Tim Verbelen
 */
public interface GoldbergServer {
	
	public static final String TOPIC = "osgi/enroute/rube/goldberg";

	/**
	 * Poll the server whether the contraption can start.
	 */
	public boolean poll(String id);
	
	/**
	 * The contraption should call done when finished. 
	 */
	public void done(String id);
}
