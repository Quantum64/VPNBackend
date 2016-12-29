package co.q64.vpn.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.config.ApacheDefaultConfig.ConfigurationKeys;

@Singleton
public class ApacheConfig implements Config {
	private @Inject Logger logger;

	@Override
	@Inject
	public void load() {
		for (ApacheDefaultConfig cfg : ApacheDefaultConfig.values()) {
			logger.info("Config file location: " + cfg.loadFromFile());
		}
		logger.info("Config loaded from files");
	}

	@Override
	public int getServerPort() {
		return ApacheDefaultConfig.SERVER.getInt(ConfigurationKeys.SERVER_PORT);
	}

	@Override
	public String getJKSPath() {
		return ApacheDefaultConfig.SERVER.getString(ConfigurationKeys.SERVER_JKS_PATH);
	}

	@Override
	public String getJKSPassword() {
		return ApacheDefaultConfig.SERVER.getString(ConfigurationKeys.SERVER_JKS_PASSWORD);
	}

	@Override
	public String getServerURL() {
		return ApacheDefaultConfig.SERVER.getString(ConfigurationKeys.SERVER_URL);
	}

	@Override
	public String getSQLHost() {
		return ApacheDefaultConfig.SQL.getString(ConfigurationKeys.SQL_HOST);
	}

	@Override
	public int getSQLPort() {
		return ApacheDefaultConfig.SQL.getInt(ConfigurationKeys.SQL_PORT);
	}

	@Override
	public String getSQLDatabaseName() {
		return ApacheDefaultConfig.SQL.getString(ConfigurationKeys.SQL_DB);
	}

	@Override
	public String getSQLUsername() {
		return ApacheDefaultConfig.SQL.getString(ConfigurationKeys.SQL_USER);
	}

	@Override
	public String getSQLPassword() {
		return ApacheDefaultConfig.SQL.getString(ConfigurationKeys.SQL_PASS);
	}

	@Override
	public String getOAuthID() {
		return ApacheDefaultConfig.OAUTH.getString(ConfigurationKeys.OAUTH_ID);
	}

	@Override
	public String getOAuthSecret() {
		return ApacheDefaultConfig.OAUTH.getString(ConfigurationKeys.OAUTH_SECRET);
	}
}
