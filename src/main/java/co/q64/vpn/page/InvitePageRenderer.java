package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.*;

import javax.inject.Singleton;

import co.q64.vpn.api.net.page.PageRenderer;
import co.q64.vpn.objects.UserData;

@Singleton
public class InvitePageRenderer implements PageRenderer {
	@Override
	public String render(UserData data) {
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("This website is currently invite only");
		result.append(BR);
		result.append("Please get an access token from the site owner");
		result.append(BR);
		result.append(BR);
		result.append("Type access token below");
		result.append(FORM_BEGIN_ACTION);
		result.append("invite");
		result.append(FORM_END_ACTION);
		result.append(FORM_TEXT_BEGIN);
		result.append("code");
		result.append(FORM_TEXT_END);
		result.append(FORM_SUBMIT);
		result.append(FORM_FINAL);
		result.append(END);
		return result.toString();
	}
}
