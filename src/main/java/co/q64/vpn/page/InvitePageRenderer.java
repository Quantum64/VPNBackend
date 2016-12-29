package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.*;

import javax.inject.Singleton;

@Singleton
public class InvitePageRenderer {
	public String render(String url) {
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("This website is currently invite only");
		result.append(BR);
		result.append("Please get an access token from the site owner");
		result.append(BR);
		result.append(BR);
		result.append("Type access token below");
		result.append(END);
		return result.toString();
	}
}
