package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.*;

import javax.inject.Singleton;

@Singleton
public class LoginPageRenderer {
	public String render(String url) {
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("Please Login");
		result.append(BR);
		result.append(BR);
		result.append(LINK_START);
		result.append(url);
		result.append(LINK_END);
		result.append("Click to sign in with GitHub");
		result.append(LINK_FINAL);
		result.append(END);
		return result.toString();
	}
}
