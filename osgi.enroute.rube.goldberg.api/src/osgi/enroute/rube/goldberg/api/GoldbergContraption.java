package osgi.enroute.rube.goldberg.api;

/**
 * An actual Goldberg Contraption
 * 
 * @author tverbele
 *
 */
public interface GoldbergContraption {

	/**
	 * For now a simple String as id to identify the contraption.
	 */
	public String getId();
	
	/**
	 * Actually run the contraption, this method returns when the contraption is finished.
	 */
	public void run();
	
}
