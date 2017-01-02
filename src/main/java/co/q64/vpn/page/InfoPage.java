package co.q64.vpn.page;

import org.watertemplate.Template;

public class InfoPage extends Template {

	public InfoPage(String main, String sub) {
		add("head", main);
		add("sub", sub);
	}

	@Override
	protected String getFilePath() {
		return "info.water";
	}
}
