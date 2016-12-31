package co.q64.vpn.util;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;

import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.log.Logger;

import com.mashape.unirest.http.Unirest;

@Singleton
public class PayPalAPI {
	private static String API = "https://api.paypal.com/v1/";

	private @Inject Config config;
	private @Inject Logger logger;

	private String token;
	private long tokenTime;

	public PayPalAPI() {
		this.token = null;
		this.tokenTime = System.currentTimeMillis();
	}

	private String getToken() {
		JSONObject object = null;
		try {
			if (tokenTime < System.currentTimeMillis()) {
				//formatter:off
				object = Unirest.post(API + "oauth2/token")
						.header("Accept", "application/json")
						.header("Accept-Language", "en_US")
						.header("Content-Type", "application/x-www-form-urlencoded")
						.basicAuth(config.getPaypalID(), config.getPaypalSecret())
						.body("grant_type=client_credentials")
						.asJson().getBody().getObject();
				//formatter:on
				token = object.getString("access_token");
				tokenTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(object.getInt("expires_in"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(object.toString());
		}
		return token;
	}

	public String createPayment(int price, int weeks) {
		try {

			/*//formatter:off
			JSONObject pr = new JSONObject();
			pr.put("name", "Q64 VPN");
			pr.put("temporary", false);
			JSONObject input = new JSONObject();
			input.put("allow_note", false);
			input.put("no_shipping", 1);
			pr.put("input_fields", input);
			JSONObject flow = new JSONObject();
			flow.put("user_action", "commit");
			flow.put("landing_page_type", "billing");
			pr.put("flow_config", flow);
			//formatter:off
			JSONObject profile = Unirest.post(API + "payment-experience/web-profiles")
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + getToken())
					.body(pr.toString())
					.asJson().getBody().getObject();
			//formatter:on
			logger.info(profile.toString());
			String profileId = profile.getString("id");
			logger.info(profileId);
			*///formatter:on

			JSONObject request = new JSONObject();
			request.put("intent", "sale");
			request.put("experience_profile_id", "XP-NJVB-UHDV-9Y2X-QB95");
			JSONObject redirect = new JSONObject();
			redirect.put("return_url", "http://q64.co/paypaldone");
			redirect.put("cancel_url", "http://q64.co/paypalfail");
			request.put("redirect_urls", redirect);
			JSONObject payer = new JSONObject();
			payer.put("payment_method", "paypal");
			request.put("payer", payer);
			JSONArray transactions = new JSONArray();
			JSONObject tranObj = new JSONObject();
			JSONObject amount = new JSONObject();
			amount.put("total", price);
			amount.put("currency", "USD");
			tranObj.put("amount", amount);
			JSONObject itemList = new JSONObject();
			JSONArray items = new JSONArray();
			JSONObject mainItem = new JSONObject();
			mainItem.put("quantity", "1");
			mainItem.put("name", weeks + " Week VPN Subscription");
			mainItem.put("price", price);
			mainItem.put("currency", "USD");
			mainItem.put("description", "Grants VPN access for " + weeks + "week" + (weeks != 1 ? "s" : new String()));
			mainItem.put("tax", "0");
			itemList.put("items", items);
			tranObj.put("item_list", itemList);
			transactions.put(tranObj);
			request.put("transactions", transactions);
			//formatter:off
			JSONObject object = Unirest.post(API + "payments/payment")
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + getToken())
					.body(request.toString())
					.asJson().getBody().getObject();
			//formatter:on
			logger.info(object.toString());
			return object.getString("id");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean executePayment(String paymentId, String payerId) {
		try {
			logger.info("Exec on payment " + paymentId + " for " + payerId);
			JSONObject request = new JSONObject();
			request.put("payer_id", payerId);
			//formatter:off
			JSONObject object = Unirest.post(API + "payments/payment/" + paymentId + "/execute/")
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + getToken())
				.body(request.toString())
				.asJson().getBody().getObject();
			//formatter:on
			logger.info(object.toString());
			return object.getString("state").equals("approved");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
