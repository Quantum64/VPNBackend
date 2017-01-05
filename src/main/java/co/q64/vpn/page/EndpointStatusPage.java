package co.q64.vpn.page;

import org.watertemplate.Template;

import co.q64.vpn.objects.UserData;

public class EndpointStatusPage extends Template {

	public EndpointStatusPage(UserData ud, String name, long endTime) {
		boolean premium = ud.getEndTime() > System.currentTimeMillis();
		add("premium", premium);
		add("status", premium ? "Premium" : "Free");
		add("time", String.valueOf(premium ? ud.getEndTime() : endTime));
		add("name", name);
		add("address", "net.q64.co");
		add("username", ud.getUsername());
		add("password", ud.getPass());
	}

	@Override
	protected String getFilePath() {
		return "status.water";
	}
}
