package osgi.enroute.rube.goldberg.camera;

import java.nio.ByteBuffer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.rube.goldberg.camera.api.Camera;
import osgi.enroute.rube.goldberg.camera.api.CameraListener;

@Component(immediate=true)
public class V4L2Camera implements Camera {
	
	private volatile boolean on;
	
	private String device = "/dev/video0";
	private int dev; // native device id
	private int format = FORMAT_MJPEG;
	private int fps = 15;
	
	private byte[] buffer;
	
	private Thread captureThread = null;
	
	// for now only one listener...
	private CameraListener listener = null;
	
	@Activate
	public void activate(){
		init();
		System.out.println("ACTIVATE!");
		
		start();
	}
	
	@Deactivate
	public void deactivate(){
		cleanup();
	}
	
	@Reference
	public void setCameraListener(CameraListener l){
		this.listener = l;
	}
	
	@Override
	public int width() {
		return get_width(dev);
	}

	@Override
	public int height() {
		return get_height(dev);
	}

	@Override
	public int format() {
		return format;
	}
	
	private void init(){
		dev = open_device(device);
	}	
	
	@Override
	public synchronized void start(){
		on = true;
		captureThread = new Thread(new Runnable(){
			public void run(){
				final ByteBuffer bb = start_capturing(dev, 640, 480, format);
				System.out.println("Started capturing "+get_width(dev)+"x"+get_height(dev));

				final int size = get_width(dev)*get_height(dev);
				
				switch(format){
				case FORMAT_GRAYSCALE:
					buffer = new byte[size];
					break;
				case FORMAT_RGB:
					buffer = new byte[size*3];
					break;
				case FORMAT_MJPEG:
					buffer = new byte[size*2];
					break;
				}
				
				while(on){
					next_frame(dev);
					
					
					int start = -1;
					int end = -1;
					// size of buffer constantly changes in case of MJPEG
					if(format==FORMAT_MJPEG){
						for(int i=0;i<bb.capacity()-1;i++){
							if((bb.get(i) & 0xff) == 255){
								if((bb.get(i+1) & 0xff) == 216){
									start = i;
								} else if((bb.get(i+1) & 0xff) == 217){
									end = i+2;
									break;
								}
							}
						}
					}
					
					if(start >= 0 && end-start > 2){
						buffer = new byte[end-start];
						
						bb.position(start);
					    bb.get(buffer);
					    
					    if(listener!=null){
					    	listener.nextFrame(buffer);
					    }
					}
					
					try {
						Thread.sleep(1000/fps);
					} catch (InterruptedException e) {
					}
				}
				
				stop_capturing(dev);

			}
		});
		captureThread.start();
	}
	
	@Override
	public synchronized void stop(){
		on = false;
	}
	
	private void cleanup(){
		if(on){
			stop();
			try {
				captureThread.join();
			} catch (InterruptedException e) {
			}
		}
		
		close_device(dev);
	}
	
	private native int open_device(String device);

	private native ByteBuffer start_capturing(int dev, int width, int height, int format);

	private native int next_frame(int dev);

	private native void stop_capturing(int dev);

	private native void close_device(int dev);

	private native int get_width(int dev);

	private native int get_height(int dev);

	static {
		try {
			System.loadLibrary("Camera");
		} catch (final UnsatisfiedLinkError e) {
			System.err.println("Native code library Camera failed to load.");
			throw e;
		}
	}
}
