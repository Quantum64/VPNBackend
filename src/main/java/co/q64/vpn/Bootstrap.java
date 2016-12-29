package co.q64.vpn;

import javax.inject.Inject;

import co.q64.vpn.inject.DefaultModule;
import co.q64.vpn.inject.RuntimeModule;
import co.q64.vpn.util.VersionPrinter;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Bootstrap {
	private @Inject VersionPrinter versionPrinter;

	public void init() {
		Injector injector = Guice.createInjector(Modules.override(new DefaultModule()).with(new RuntimeModule()));
		injector.injectMembers(this);

		versionPrinter.printProgramInfo();
	}
}
