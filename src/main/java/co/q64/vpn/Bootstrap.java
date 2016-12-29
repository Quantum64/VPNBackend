package co.q64.vpn;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.net.Server;
import co.q64.vpn.inject.DefaultModule;
import co.q64.vpn.inject.RuntimeModule;
import co.q64.vpn.objects.CodeData;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.task.AccountSweeperTask;
import co.q64.vpn.util.VersionPrinter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Bootstrap {
	private @Inject VersionPrinter versionPrinter;
	private @Inject Config config;
	private @Inject Server server;
	private @Inject Database database;
	private @Inject AccountSweeperTask cleanerTask;

	private ScheduledThreadPoolExecutor userCleaner;

	public void init() {
		Injector injector = Guice.createInjector(Modules.override(new DefaultModule()).with(new RuntimeModule()));
		injector.injectMembers(this);

		versionPrinter.printProgramInfo();
		database.addTable(UserData.class);
		database.addTable(CodeData.class);
		database.initDatabase();
		if (!database.isValid()) {
			return;
		}
		server.startServer();

		userCleaner = new ScheduledThreadPoolExecutor(1);
		userCleaner.scheduleAtFixedRate(cleanerTask, 0, 5, TimeUnit.MINUTES);
	}
}
