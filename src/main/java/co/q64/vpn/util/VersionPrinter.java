package co.q64.vpn.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import co.q64.vpn.api.log.Logger;
import co.q64.vpn.bind.ConstantBinders.Author;
import co.q64.vpn.bind.ConstantBinders.ModuleName;
import co.q64.vpn.bind.ConstantBinders.Name;
import co.q64.vpn.bind.ConstantBinders.Version;

@Singleton
public class VersionPrinter {
	private @Inject @Name String name;
	private @Inject @Version String version;
	private @Inject @Author String author;
	private @Inject @ModuleName String module;
	private @Inject Logger logger;

	public void printProgramInfo() {
		logger.info("This is " + name + " by " + author + " version " + version + " using configuration module " + module);
	}
}
