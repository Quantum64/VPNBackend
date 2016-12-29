package co.q64.vpn.page;

import static co.q64.vpn.page.BasicHTMLComponents.BEGIN;
import static co.q64.vpn.page.BasicHTMLComponents.BR;
import static co.q64.vpn.page.BasicHTMLComponents.END;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_BEGIN_ACTION;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_END_ACTION;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_FINAL;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_SUBMIT;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_TEXT_BEGIN;
import static co.q64.vpn.page.BasicHTMLComponents.FORM_TEXT_END;
import static co.q64.vpn.page.BasicHTMLComponents.LINK_END;
import static co.q64.vpn.page.BasicHTMLComponents.LINK_FINAL;
import static co.q64.vpn.page.BasicHTMLComponents.LINK_START;
import static co.q64.vpn.page.BasicHTMLComponents.SCRIPT_BEGIN;
import static co.q64.vpn.page.BasicHTMLComponents.SCRIPT_END;

import javax.inject.Inject;
import javax.inject.Singleton;

import co.q64.vpn.api.net.page.PageRenderer;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.util.TimeUtil;

@Singleton
public class AccountPageRenderer implements PageRenderer {
	private @Inject TimeUtil time;

	@Override
	public String render(UserData data) {
		boolean free = System.currentTimeMillis() > data.getEndTime();
		StringBuilder result = new StringBuilder();
		result.append(BEGIN);
		result.append("Hello " + data.getId() + " - ");
		result.append(LINK_START);
		result.append("logout");
		result.append(LINK_END);
		result.append("Sign Out");
		result.append(LINK_FINAL);
		result.append(BR);
		result.append("Account status: " + (free ? "Free" : "Premium"));
		result.append(BR);
		if (free) {
			result.append("All unpaid accounts are terminated after one week unless additional payment is received");
			result.append(BR);
			result.append("Your account will be terminated in ");
			result.append(SCRIPT_BEGIN);
			result.append("window.onload = init;  function init(){countdown(function(ts) {document.getElementById('pageTimer').innerHTML = ts.toHTML(\"normal\"); }, " + time.getAccountTerminationTime(data) + ", countdown.ALL);}");
			result.append(SCRIPT_END);
			result.append("<span id = \"pageTimer\" style=\"color:#FF0000\">timer</span>");
		} else {
			result.append("Your subscription will end in ");
			result.append(SCRIPT_BEGIN);
			result.append("window.onload = init;  function init(){countdown(function(ts) {document.getElementById('pageTimer').innerHTML = ts.toHTML(\"normal\"); }, " + data.getEndTime() + ", countdown.ALL);}");
			result.append(SCRIPT_END);
			result.append("<span id = \"pageTimer\" style=\"color:#FF0000\">timer</span>");
		}
		result.append(BR);
		result.append(BR);
		result.append("Current pricing: $1/week of VPN access with guaranteed 100mb/s bandwidth, payable to site owner");
		result.append(BR);
		result.append(BR);
		result.append("Redeem time code (you get this when your purchase more time)");
		result.append(FORM_BEGIN_ACTION);
		result.append("redeem");
		result.append(FORM_END_ACTION);
		result.append(FORM_TEXT_BEGIN);
		result.append("code");
		result.append(FORM_TEXT_END);
		result.append(FORM_SUBMIT);
		result.append(FORM_FINAL);
		if (Boolean.valueOf(data.getIsAdmin())) {
			result.append(BR);
			result.append(BR);
			result.append("Generate access code (admin) [code name]");
			result.append(FORM_BEGIN_ACTION);
			result.append("genaccess");
			result.append(FORM_END_ACTION);
			result.append(FORM_TEXT_BEGIN);
			result.append("code");
			result.append(FORM_TEXT_END);
			result.append(FORM_SUBMIT);
			result.append(FORM_FINAL);
			result.append(BR);
			result.append(BR);
			result.append("Generate time code (admin) [code name] [time in days]");
			result.append(FORM_BEGIN_ACTION);
			result.append("gentime");
			result.append(FORM_END_ACTION);
			result.append(FORM_TEXT_BEGIN);
			result.append("code");
			result.append(FORM_TEXT_END);
			result.append(FORM_TEXT_BEGIN);
			result.append("time");
			result.append(FORM_TEXT_END);
			result.append(FORM_SUBMIT);
			result.append(FORM_FINAL);
		}
		result.append(END);
		return result.toString();
	}
}
