package co.q64.vpn.page;

import org.watertemplate.Template;

public class ErrorPage extends Template {

	public ErrorPage(String error) {
		add("head", "That's an error...");
		add("sub", error);
	}

	@Override
	protected String getFilePath() {
		return "info.water";
	}
}
