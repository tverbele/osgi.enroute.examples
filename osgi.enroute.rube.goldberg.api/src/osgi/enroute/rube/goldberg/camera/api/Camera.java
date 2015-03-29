package osgi.enroute.rube.goldberg.camera.api;

public interface Camera {

	public final static int FORMAT_GRAYSCALE = 0;
	public final static int FORMAT_RGB = 1;
	public final static int FORMAT_MJPEG = 2;
	
	public int width();
	
	public int height();
	
	public int format();
	
	public void start();
	
	public void stop();
}
