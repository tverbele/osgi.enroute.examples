package osgi.enroute.rube.goldberg.server.application;

import java.util.Iterator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import osgi.enroute.capabilities.AngularWebResource;
import osgi.enroute.capabilities.BootstrapWebResource;
import osgi.enroute.capabilities.ConfigurerExtender;
import osgi.enroute.capabilities.WebServerExtender;
import osgi.enroute.rest.api.REST;
import osgi.enroute.rest.api.RESTRequest;
import osgi.enroute.rube.goldberg.api.server.GoldbergServerControl;

@AngularWebResource(resource={"angular.js","angular-resource.js", "angular-route.js"}, priority=1000)
@BootstrapWebResource(resource="css/bootstrap.css")
@WebServerExtender
@ConfigurerExtender
@Component(name="osgi.enroute.rube.goldberg.server")
public class ServerApplication implements REST {

	private GoldbergServerControl control;
	
	@Reference
	public void setGoldbergServerControl(GoldbergServerControl c){
		control = c;
	}
	
	public String getContraptions(RESTRequest rq){
		// TODO use JSON lib for this..
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		Iterator<String> it = control.list().iterator();
		while(it.hasNext()){
			String s = it.next();
			sb.append("\""+s+"\"");
			if(it.hasNext()){
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	public String getStart(RESTRequest rq, String contraption){
		control.start(contraption);
		return "";
	}
}
