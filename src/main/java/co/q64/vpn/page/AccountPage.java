package co.q64.vpn.page;

import org.watertemplate.Template;
import org.watertemplate.TemplateMap.SubTemplates;

import co.q64.vpn.objects.UserData;

public class AccountPage extends Template {

	public AccountPage(UserData ud, String name, long endTime) {
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
	protected void addSubTemplates(SubTemplates subTemplates) {
		subTemplates.add("why", new WhySection());
	}

	@Override
	protected String getFilePath() {
		return "account.water";
	}
}
