package osgi.enroute.rube.goldberg.server.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import osgi.enroute.rube.goldberg.camera.api.CameraListener;

@Component
public class CameraStreamServlet extends HttpServlet implements CameraListener {

	private List<AsyncContext> streams = new ArrayList<AsyncContext>();
	
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
		}
	}

	@Override
	public void nextFrame(byte[] data) {
		synchronized(streams){
			Iterator<AsyncContext> it = streams.iterator();
			while(it.hasNext()){
				try {
					AsyncContext stream = it.next();
					sendFrame(stream, data);
				} catch (IOException e) {
					// Remove notifier because connection failed. (ex: browser is closed)
					it.remove();
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
		stream.getResponse().getOutputStream().flush();
	}
}
