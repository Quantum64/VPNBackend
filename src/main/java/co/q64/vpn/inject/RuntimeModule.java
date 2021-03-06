package co.q64.vpn.inject;

import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.api.net.Server;
import co.q64.vpn.bind.ConstantBinders.ModuleName;
import co.q64.vpn.database.MySQLDatabase;
import co.q64.vpn.config.ApacheConfig;
import co.q64.vpn.log.SystemLogger;
import co.q64.vpn.net.SparkServer;

import com.google.inject.AbstractModule;

public class RuntimeModule extends AbstractModule {
	private static String MODULE_NAME = "Default Runtime";

	@Override
	protected void configure() {
		bindConstant().annotatedWith(ModuleName.class).to(MODULE_NAME);

		bind(Logger.class).to(SystemLogger.class);
		bind(Config.class).to(ApacheConfig.class);
		bind(Server.class).to(SparkServer.class);
		bind(Database.class).to(MySQLDatabase.class);
	}
}
