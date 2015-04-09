package osgi.enroute.rube.goldberg.api.camera.listener;

public interface CameraListener {

	public void nextFrame(long timestamp, byte[] data);
}
