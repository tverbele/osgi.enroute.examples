package osgi.enroute.rube.goldberg.camera.api;

public interface CameraListener {

	public void nextFrame(byte[] data);
}
