package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.*;

import javax.inject.Singleton;

@Singleton
public class InvalidPageRenderer {
	public String render() {
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("That was an invalid code!");
		result.append(BR);
		result.append(BR);
		result.append(LINK_START);
		result.append("/");
		result.append(LINK_END);
		result.append("Return");
		result.append(LINK_FINAL);
		result.append(END);
		return result.toString();
	}
}
