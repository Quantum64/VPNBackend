package co.q64.vpn.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;

import spark.Service;
import spark.Session;
import spark.Spark;
import spark.template.water.WaterTemplateEngine;
import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.api.net.Server;
import co.q64.vpn.objects.CodeData;
import co.q64.vpn.objects.CodeData.CodeUsage;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.page.AccountPage;
import co.q64.vpn.page.DemoPage;
import co.q64.vpn.page.EndpointNoauthPage;
import co.q64.vpn.page.EndpointStatusPage;
import co.q64.vpn.page.ErrorPage;
import co.q64.vpn.page.ForceTosPage;
import co.q64.vpn.page.InfoPage;
import co.q64.vpn.page.InvitePage;
import co.q64.vpn.page.LoginPage;
import co.q64.vpn.page.LogoutPage;
import co.q64.vpn.page.TosPage;
import co.q64.vpn.page.VerifyPage;
import co.q64.vpn.util.IPSECUpdater;
import co.q64.vpn.util.PayPalAPI;
import co.q64.vpn.util.TimeUtil;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.tasks.OnSuccessListener;

@Singleton
public class SparkServer implements Server {
	private @Inject Config config;
	private @Inject Database database;
	private @Inject TimeUtil time;
	private @Inject IPSECUpdater ipsec;
	private @Inject Logger logger;
	private @Inject PayPalAPI paypal;

	private FirebaseAuth auth;
	private Map<String, Long> lastRequest = new HashMap<String, Long>();
	private ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);

	@Inject
	public void init() {
		File firebase = new File(Paths.get("firebase.json").toUri());
		if (!firebase.exists()) {
			logger.error("Could not find firebase config at " + firebase.getAbsolutePath());
			return;
		}
		//formatter:off
		FirebaseOptions options = null;
		try {
			options = new FirebaseOptions.Builder()
			  .setServiceAccount(new FileInputStream(firebase))
			  .build();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		//formatter:on
		FirebaseApp.initializeApp(options);
		auth = FirebaseAuth.getInstance();
		pool.scheduleAtFixedRate(() -> {
			Set<String> remove = new HashSet<String>();
			for (Entry<String, Long> e : lastRequest.entrySet()) {
				if (System.currentTimeMillis() > e.getValue() + TimeUnit.HOURS.toMillis(12)) {
					remove.add(e.getKey());
				}
			}
			lastRequest.keySet().removeAll(remove);
			for (String s : remove) {
				database.disconnect(s);
			}
		}, 10, 10, TimeUnit.MINUTES);

	}

	@Override
	public void startServer() {
		Service http = Service.ignite();
		http.port(config.getServerPort());
		http.before("/*", (request, response) -> {
			if (request.url().startsWith("http://")) {
				response.redirect(request.url().replace("http://", "https://"), 301);
			}
		});

		Spark.secure(config.getJKSPath(), config.getJKSPassword(), null, null);
		Spark.port(443);

		Spark.get("/endpoint/win", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null) {
				response.redirect("/verify");
				return null;
			}
			if (data == null) {
				String id = token.getUid();
				database.disconnect(id);
				database.queryData(id);
				UserData userData = database.getData(UserData.class, id);
				s.attribute("data", userData);
				data = userData;
			}
			if (time.getAccountTerminationTime(data) < System.currentTimeMillis()) {
				database.deleteData(data);
				response.redirect("/terminated");
				return null;
			}
			if (Boolean.valueOf(data.getIsNew()) || !Boolean.valueOf(data.getIsTos())) {
				response.redirect("/endpoint/noauth");
				return null;
			}
			response.redirect("/status");
			return null;
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/endpoint/win/login", (request, response) -> {
			return WaterTemplateEngine.render(new LoginPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/endpoint/win/status", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null || data == null || Boolean.valueOf(data.getIsNew()) || !Boolean.valueOf(data.getIsTos())) {
				response.redirect("/");
				return null;
			}
			return WaterTemplateEngine.render(new EndpointStatusPage(data, token.getName(), time.getAccountTerminationTime(data)), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/endpoint/win/noauth", (request, response) -> {
			return WaterTemplateEngine.render(new EndpointNoauthPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/login", (request, response) -> {
			return WaterTemplateEngine.render(new LoginPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/verify", (request, response) -> {
			return WaterTemplateEngine.render(new VerifyPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.post("/token/*", (request, response) -> {
			Session session = request.session();
			String token = request.queryParams("user");
			auth.verifyIdToken(token).addOnSuccessListener(new OnSuccessListener<FirebaseToken>() {

				@Override
				public void onSuccess(FirebaseToken token) {
					session.attribute("token", token);
					logger.info("Auth accepted for " + token.getUid());
				}
			});
			return "";
		});

		Spark.get("/access", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null || data == null) {
				response.redirect("/");
				return null;
			}
			return WaterTemplateEngine.render(new InvitePage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/account", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null || data == null || Boolean.valueOf(data.getIsNew()) || !Boolean.valueOf(data.getIsTos())) {
				response.redirect("/");
				return null;
			}
			return WaterTemplateEngine.render(new AccountPage(data, token.getName(), time.getAccountTerminationTime(data)), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/forcetos", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null || data == null || Boolean.valueOf(data.getIsTos())) {
				response.redirect("/");
				return null;
			}
			return WaterTemplateEngine.render(new ForceTosPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/conftos", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null || data == null || Boolean.valueOf(data.getIsTos())) {
				response.redirect("/");
				return null;
			}
			data.setIsTos(String.valueOf(true));
			response.redirect("/");
			return null;
		});

		Spark.get("robots.txt", (request, response) -> {
			return "User-agent: *\nDisallow: /";
		});

		Spark.get("/tos", (request, response) -> {
			return WaterTemplateEngine.render(new TosPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/", (request, response) -> {
			Session s = request.session();
			FirebaseToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null) {
				response.redirect("/verify");
				return null;
			}
			if (data == null) {
				String id = token.getUid();
				database.disconnect(id);
				database.queryData(id);
				UserData userData = database.getData(UserData.class, id);
				s.attribute("data", userData);
				data = userData;
			}
			if (time.getAccountTerminationTime(data) < System.currentTimeMillis()) {
				database.deleteData(data);
				response.redirect("/terminated");
				return null;
			}
			if (Boolean.valueOf(data.getIsNew())) {
				if (s.attribute("preauth") != null && Boolean.valueOf(s.attribute("preauth"))) {
					data.setIsNew(String.valueOf(false));
				} else {
					response.redirect("/access");
					return null;
				}
			}
			if (!Boolean.valueOf(data.getIsTos())) {
				response.redirect("/forcetos");
				return null;
			}
			response.redirect("/account");
			return null;
		});

		Spark.exception(Exception.class, (exception, request, response) -> {
			exception.printStackTrace();
		});

		Spark.post("/invite", (request, response) -> {
			Session s = request.session();
			UserData ud = s.attribute("data");
			if (ud == null || !Boolean.valueOf(ud.getIsNew())) {
				response.redirect("/");
				return null;
			}
			String code = request.queryParams("code");
			if (code == null || code.isEmpty()) {
				response.redirect("/invalid");
				return null;
			}
			database.disconnect(code);
			database.queryData(code);
			CodeData data = database.getData(CodeData.class, code);
			if (Boolean.valueOf(data.getIsValid()) && data.getUsage().equals(CodeUsage.ACCESS.name())) {
				ud.setIsNew(String.valueOf(false));
				if (!Boolean.valueOf(data.getIsPerm())) {
					database.deleteData(data);
				}
				database.disconnect(code);
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			response.redirect("/invalid");
			return null;
		});

		Spark.get("/preauth/*", (request, response) -> {
			Session s = request.session();
			String token = request.queryParams("token");
			if (token == null || token.isEmpty()) {
				return WaterTemplateEngine.render(new ErrorPage("That token is invalid"), request);
			}
			database.disconnect(token);
			database.queryData(token);
			CodeData data = database.getData(CodeData.class, token);
			if (Boolean.valueOf(data.getIsValid()) && data.getUsage().equals(CodeUsage.ACCESS.name())) {
				if (!Boolean.valueOf(data.getIsPerm())) {
					database.deleteData(data);
				}
				database.disconnect(token);
				s.attribute("preauth", String.valueOf(true));
				return WaterTemplateEngine.render(new DemoPage(), request);
			}
			database.disconnect(token);
			return WaterTemplateEngine.render(new ErrorPage("That token is invalid"), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.post("/redeem", (request, response) -> {
			Session s = request.session();
			UserData ud = s.attribute("data");
			if (ud == null || Boolean.valueOf(ud.getIsNew())) {
				response.redirect("/");
				return null;
			}
			String code = request.queryParams("code");
			if (code == null || code.isEmpty()) {
				response.redirect("/invalid");
				return null;
			}
			database.disconnect(code);
			database.queryData(code);
			CodeData data = database.getData(CodeData.class, code);
			if (Boolean.valueOf(data.getIsValid()) && data.getUsage().equals(CodeUsage.TIME.name())) {
				long time = ud.getEndTime();
				if (time < System.currentTimeMillis()) {
					time = System.currentTimeMillis();
				}
				time += TimeUnit.DAYS.toMillis(data.getAmount());
				ud.setEndTime(time);
				if (ipsec.get(ud.getUsername()) == null) {
					if (ud.getUsername().equals("default")) {
						StringBuilder user = new StringBuilder();
						for (int i = 0; i < 8; i++) {
							char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
							user.append(c);
						}
						ud.setUsername(user.toString());
					}
					StringBuilder pass = new StringBuilder();
					for (int i = 0; i < 8; i++) {
						char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
						pass.append(c);
					}
					ipsec.update(ud.getUsername(), pass.toString());
					ud.setPass(pass.toString());
				}
				ipsec.updateNow();
				database.deleteData(data);
				database.disconnect(code);
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			response.redirect("/invalid");
			return null;
		});

		Spark.post("/genaccess", (request, response) -> {
			Session s = request.session();
			UserData ud = s.attribute("data");
			if (ud == null || !Boolean.valueOf(ud.getIsAdmin())) {
				response.redirect("/");
				return null;
			}
			String code = request.queryParams("code");
			if (code == null || code.isEmpty()) {
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			database.queryData(code);
			CodeData data = database.getData(CodeData.class, code);
			data.setIsValid(String.valueOf(true));
			data.setUsage(CodeUsage.ACCESS.name());
			response.redirect("/code/gen?name=" + data.getId());
			return null;
		});

		Spark.post("/gentime", (request, response) -> {
			Session s = request.session();
			UserData ud = s.attribute("data");
			if (ud == null || !Boolean.valueOf(ud.getIsAdmin())) {
				response.redirect("/");
				return null;
			}
			String code = request.queryParams("code");
			if (code == null || code.isEmpty()) {
				response.redirect("/");
				return null;
			}
			String time = request.queryParams("time");
			if (time == null || time.isEmpty()) {
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			database.queryData(code);
			CodeData data = database.getData(CodeData.class, code);
			data.setIsValid(String.valueOf(true));
			data.setAmount(Integer.parseInt(time));
			data.setUsage(CodeUsage.TIME.name());
			response.redirect("/code/gen?name=" + data.getId() + "&time=" + data.getAmount());
			return null;
		});

		Spark.get("/cert", (request, response) -> {
			try {
				File file = new File(Paths.get("cert.p12").toUri());
				if (!file.exists()) {
					logger.warn("Did not find expected cert file at " + file.getAbsolutePath());
					return null;
				}
				InputStream inputStream = new FileInputStream(file);
				response.type("application/x-pkcs12");
				response.status(200);

				byte[] buf = new byte[1024];
				OutputStream os = response.raw().getOutputStream();
				OutputStreamWriter outWriter = new OutputStreamWriter(os);
				int count = 0;
				while ((count = inputStream.read(buf)) >= 0) {
					os.write(buf, 0, count);
				}
				inputStream.close();
				outWriter.close();

				return new String();
			} catch (Exception e) {
				logger.error(e);
				return null;
			}
		});

		Spark.get("/osxcert", (request, response) -> {
			try {
				File file = new File(Paths.get("osxcert.p12").toUri());
				if (!file.exists()) {
					logger.warn("Did not find expected cert file at " + file.getAbsolutePath());
					return null;
				}
				InputStream inputStream = new FileInputStream(file);
				response.type("application/x-pkcs12");
				response.status(200);

				byte[] buf = new byte[1024];
				OutputStream os = response.raw().getOutputStream();
				OutputStreamWriter outWriter = new OutputStreamWriter(os);
				int count = 0;
				while ((count = inputStream.read(buf)) >= 0) {
					os.write(buf, 0, count);
				}
				inputStream.close();
				outWriter.close();

				return new String();
			} catch (Exception e) {
				logger.error(e);
				return null;
			}
		});

		Spark.get("/invalid", (request, response) -> {
			return WaterTemplateEngine.render(new ErrorPage("That was an invalid code!"), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/invalid", (request, response) -> {
			return WaterTemplateEngine.render(new ErrorPage("Your session was ended due to inactivity"), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/logout", (request, response) -> {
			Session s = request.session();
			UserData data = s.attribute("data");
			if (data != null) {
				database.disconnect(data.getId());
			}
			s.removeAttribute("token");
			s.removeAttribute("data");
			return WaterTemplateEngine.render(new LogoutPage(), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/terminated", (request, response) -> {
			Session s = request.session();
			s.removeAttribute("token");
			s.removeAttribute("data");
			return WaterTemplateEngine.render(new ErrorPage("Your account has been terminated"), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/code/*", (request, response) -> {
			String name = request.queryParams("name");
			String time = request.queryParams("time");
			if (time == null) {
				return WaterTemplateEngine.render(new InfoPage("New access code", name), request);
			} else {
				return WaterTemplateEngine.render(new InfoPage("New time code for " + time + " days", name), request);
			}
		}, WaterTemplateEngine.waterEngine());

		// Paypal

		Spark.post("/paypalnew/*", (request, response) -> {
			int time = Integer.parseInt(request.queryParams("time"));
			JSONObject object = new JSONObject();
			String id = paypal.createPayment(time, time);
			object.put("paymentID", id);
			request.session().attribute("time", time);
			return object.toString();
		});

		Spark.post("/paypalexec/*", (request, response) -> {
			logger.info(request.url());
			String id = request.queryParams("id");
			String payer = request.queryParams("payer");
			int time = request.session().attribute("time");
			if (time == 0) {
				logger.warn("Failed to get sub time");
				return new JSONObject().toString();
			}
			if (paypal.executePayment(id, payer)) {
				UserData ud = request.session().attribute("data");
				long newtime = ud.getEndTime();
				if (newtime < System.currentTimeMillis()) {
					newtime = System.currentTimeMillis();
				}
				newtime += TimeUnit.DAYS.toMillis(time * 7);
				ud.setEndTime(newtime);
				if (ipsec.get(ud.getUsername()) == null) {
					if (ud.getUsername().equals("default")) {
						StringBuilder user = new StringBuilder();
						for (int i = 0; i < 8; i++) {
							char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
							user.append(c);
						}
						ud.setUsername(user.toString());
					}
					StringBuilder pass = new StringBuilder();
					for (int i = 0; i < 8; i++) {
						char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
						pass.append(c);
					}
					ipsec.update(ud.getUsername(), pass.toString());
					ud.setPass(pass.toString());
				}
				ipsec.updateNow();
				logger.info("Added time");
				return new JSONObject().toString();
			}
			logger.info("Fail payment exec");
			return new JSONObject().toString();
		});

		Spark.get("/paypaldone", (request, response) -> {
			return WaterTemplateEngine.render(new InfoPage("Payment Successful", "Time has been added to your account"), request);
		}, WaterTemplateEngine.waterEngine());

		Spark.get("/paypalfail", (request, response) -> {
			return WaterTemplateEngine.render(new InfoPage("Payment Failure", "Something went wrong during the transaction and you account has not been charged"), request);
		}, WaterTemplateEngine.waterEngine());
	}
}
