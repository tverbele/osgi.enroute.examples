package osgi.enroute.rube.goldberg.client.contraption;

import org.osgi.service.component.annotations.Component;

import osgi.enroute.rube.goldberg.api.GoldbergContraption;

@Component
public class DummyContraption implements GoldbergContraption {

	@Override
	public String getId() {
		return "dummy";
	}

	@Override
	public void run() {
		for(int i=0;i<10;i++){
			System.out.println("Running step "+i);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
