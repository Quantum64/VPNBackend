package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.*;

import javax.inject.Singleton;

@Singleton
public class LoginPageRenderer {
	public String render(String github, String google) {
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("Please Login");
		result.append(BR);
		result.append(BR);
		result.append(GH_BUTTON_BEGIN);
		result.append(github);
		result.append(GH_BUTTON_FINAL);
		result.append(GOOGLE_BUTTON_BEGIN);
		result.append(google);
		result.append(GOOGLE_BUTTON_FINAL);
		result.append("Click to sign in with GitHub");
		result.append(LINK_FINAL);
		result.append(END);
		return result.toString();
	}
}
