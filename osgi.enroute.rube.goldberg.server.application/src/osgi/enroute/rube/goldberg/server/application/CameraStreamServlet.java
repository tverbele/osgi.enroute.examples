package osgi.enroute.rube.goldberg.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

import osgi.enroute.rube.goldberg.api.camera.listener.CameraListener;

@Component(
	property = { 
		RemoteConstants.SERVICE_EXPORTED_INTERFACES +"=osgi.enroute.rube.goldberg.api.camera.listener.CameraListener"
	})
public class CameraStreamServlet extends HttpServlet implements CameraListener {

	private List<AsyncContext> streams = new ArrayList<AsyncContext>();
	
	private class Frame {
		public final byte[] data;
		public final long timestamp;
		
		public Frame(long timestamp, byte[] data){
			this.data = data;
			this.timestamp = timestamp;
		}
	}
	
	private BlockingQueue<Frame> buffer = new LinkedBlockingQueue<>();
	
	private Thread playingThread;
	private volatile boolean playing = false; // wait until buffer is filled
	private volatile boolean listening = false; //check whether someone is listening

	
	@Reference
	public void setHttpService(HttpService http){
		try {
			// TODO use whiteboard instead?
			http.registerServlet("/osgi.enroute.rube.goldberg.server/camera.mjpeg", this, null, null);
		} catch (ServletException e) {
		} catch (NamespaceException e) {
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setCharacterEncoding("UTF-8");
		response.addHeader("Connection", "keep-alive");
		response.setContentType("multipart/x-mixed-replace;boundary=next");

		synchronized(streams){
			streams.add(request.startAsync());
			if(streams.size()==1){
				// start new playing thread
				listening = true;
				playingThread = new Thread(new Runnable(){
					public void run(){
						long timestamp = -1;
						while(listening){
							if(playing){
								try {
									Frame f = buffer.take();
									long sleep = f.timestamp - timestamp;
									if(sleep < 5000){ // wait utmost 5 seconds
										Thread.sleep(f.timestamp-timestamp);
									}
									timestamp = f.timestamp;
									sendFrame(f.data);
									
									if(buffer.isEmpty())
										playing = false;
								} catch(InterruptedException e){}
							}
						}
					}
				});
				playingThread.start();
				
			}
		}
	}

	@Override
	public void nextFrame(long timestamp, byte[] data) {
		Frame f = new Frame(timestamp, data);
		try {
			buffer.put(f);
		} catch (InterruptedException e) {
		}
		if(buffer.size()>20){ // TODO which buffer size?
			playing = true;
		}
	}
	
	private void sendFrame(byte[] data){
		synchronized(streams){
			Iterator<AsyncContext> it = streams.iterator();
			while(it.hasNext()){
				try {
					AsyncContext stream = it.next();
					sendFrame(stream, data);
				} catch (IOException e) {
					// Remove notifier because connection failed. (ex: browser is closed)
					it.remove();
					if(streams.size()==0){
						listening = false;
						playingThread.interrupt();
					}
				}
			}
		}
	}
	
	private void sendFrame(AsyncContext stream, byte[] data) throws IOException {
		stream.getResponse().getOutputStream().println("--next");
		stream.getResponse().getOutputStream().println("Content-Type: image/jpeg");
		stream.getResponse().getOutputStream().println("Content-Length: "+data.length);
		stream.getResponse().getOutputStream().println("");
		stream.getResponse().getOutputStream().write(data, 0, data.length);
		stream.getResponse().getOutputStream().println("");
		stream.getResponse().flushBuffer();
	}
}
