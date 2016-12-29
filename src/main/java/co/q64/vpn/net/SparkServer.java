package co.q64.vpn.net;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;

import spark.Session;
import spark.Spark;
import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.net.Server;
import co.q64.vpn.objects.CodeData;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.page.LoginPageRenderer;

import com.github.scribejava.apis.GitHubApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuth20Service;

@Singleton
public class SparkServer implements Server {
	private @Inject Config config;
	private @Inject LoginPageRenderer loginPage;
	private @Inject Database database;

	private OAuth20Service service;

	@Inject
	public void init() {
		service = new ServiceBuilder().apiKey(config.getOAuthID()).apiSecret(config.getOAuthSecret()).callback(config.getServerURL() + "/callback").scope("user:email,repo").build(GitHubApi.instance());
	}

	@Override
	public void startServer() {
		//Spark.secure(config.getJKSPath(), config.getJKSPassword(), null, null);
		Spark.port(config.getServerPort());

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
			if (Boolean.valueOf(data.getIsNew())) {

			}
			return "Hello";
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
				response.redirect("/");
				return null;
			}
			database.disconnect(code);
			CodeData data = database.getData(CodeData.class, code);
			response.redirect("/");
			return null;
		});

		Spark.get("/logout", (request, response) -> {
			Session s = request.session();
			s.removeAttribute("token");
			s.removeAttribute("data");
			response.redirect("/");
			return null;
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
	}
}
