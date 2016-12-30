package co.q64.vpn.net;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import spark.Service;
import spark.Session;
import spark.Spark;
import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.api.net.Server;
import co.q64.vpn.objects.CodeData;
import co.q64.vpn.objects.CodeData.CodeUsage;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.page.AccountPageRenderer;
import co.q64.vpn.page.BasicHTMLComponents;
import co.q64.vpn.page.InvalidPageRenderer;
import co.q64.vpn.page.InvitePageRenderer;
import co.q64.vpn.page.LoginPageRenderer;
import co.q64.vpn.util.IPSECUpdater;
import co.q64.vpn.util.TimeUtil;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuth20Service;

@Singleton
public class SparkServer implements Server {
	private @Inject Config config;
	private @Inject LoginPageRenderer loginPage;
	private @Inject InvitePageRenderer invitePage;
	private @Inject AccountPageRenderer accountPage;
	private @Inject InvalidPageRenderer invalidPage;
	private @Inject Database database;
	private @Inject TimeUtil time;
	private @Inject IPSECUpdater ipsec;
	private @Inject Logger logger;

	private OAuth20Service service;
	private Map<String, Long> lastRequest = new HashMap<String, Long>();
	private ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);

	@Inject
	public void init() {
		service = new ServiceBuilder().apiKey(config.getOAuthID()).apiSecret(config.getOAuthSecret()).callback(config.getServerURL() + "/callback/gh").scope("user:email,repo").build(GitHubApi.instance());
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
		Spark.get("/", (request, response) -> {
			Session s = request.session();
			OAuth2AccessToken token = s.attribute("token");
			UserData data = s.attribute("data");
			if (token == null) {
				return loginPage.render(service.getAuthorizationUrl());
			}

			if (data == null) {
				GitHubClient client = new GitHubClient();
				client.setOAuth2Token(token.getAccessToken());
				UserService userService = new UserService(client);
				String id = userService.getUser().getLogin();
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
				return invitePage.render(data);
			}
			return accountPage.render(data);
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
				database.deleteData(data);
				database.disconnect(code);
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			response.redirect("/invalid");
			return null;
		});

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
				if (ipsec.get(ud.getId()) == null) {
					StringBuilder pass = new StringBuilder();
					for (int i = 0; i < 8; i++) {
						char c = (char) (ThreadLocalRandom.current().nextInt(26) + 'a');
						pass.append(c);
					}
					ipsec.update(ud.getId(), pass.toString());
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
			return invalidPage.render("That was an invalid code!");
		});

		Spark.get("/session", (request, response) -> {
			return invalidPage.render("Your session was ended due to inactivity");
		});

		Spark.get("/logout", (request, response) -> {
			Session s = request.session();
			UserData data = s.attribute("data");
			if (data != null) {
				database.disconnect(data.getId());
			}
			s.removeAttribute("token");
			s.removeAttribute("data");
			response.redirect("/");
			return null;
		});

		Spark.get("/terminated", (request, response) -> {
			Session s = request.session();
			s.removeAttribute("token");
			s.removeAttribute("data");
			return BasicHTMLComponents.BEGIN + "Account terminated" + BasicHTMLComponents.END;
		});

		Spark.get("/callback/*", (request, response) -> {
			Session s = request.session();
			Object token = s.attribute("token");
			if (token == null) {
				if (request.queryParams("code") == null || request.queryParams("code").isEmpty()) {
					response.redirect("/");
					return null;
				}
				Token auth = service.getAccessToken(request.queryParams("code"));
				s.attribute("token", auth);
			}
			response.redirect("/");
			return null;
		});

		Spark.get("/code/*", (request, response) -> {
			String name = request.queryParams("name");
			String time = request.queryParams("time");
			if (time == null) {
				return BasicHTMLComponents.BEGIN + "Your new access code is: " + name + BasicHTMLComponents.END;
			} else {
				return BasicHTMLComponents.BEGIN + "Your new time code for " + time + " days is: " + name + BasicHTMLComponents.END;
			}
		});
	}
}
