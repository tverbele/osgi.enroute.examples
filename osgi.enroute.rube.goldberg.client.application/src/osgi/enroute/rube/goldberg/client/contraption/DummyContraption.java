package osgi.enroute.rube.goldberg.client.contraption;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.rube.goldberg.api.GoldbergContraption;

@ConfigurerExtender
@Component(name="osgi.enroute.rube.goldberg.contraption.dummy", 
	configurationPolicy=ConfigurationPolicy.REQUIRE)
public class DummyContraption implements GoldbergContraption {

	private String id;
	
	@Activate
	public void activate(Map<String, String> config){
		id = config.get("id");
	}
	
	@Override
	public String getId() {
		return id;
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
