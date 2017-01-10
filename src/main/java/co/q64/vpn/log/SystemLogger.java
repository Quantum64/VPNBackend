package co.q64.vpn.log;

import javax.inject.Singleton;

import co.q64.vpn.api.log.Logger;

@Singleton
public class SystemLogger implements Logger {

	@Override
	public void info(String message) {
		System.out.println("[INFO] " + message);
	}

	@Override
	public void warn(String message) {
		System.out.println("[WARN] " + message);
	}

	@Override
	public void error(String message) {
		System.err.println("[ERROR] " + message);
	}

	@Override
	public void warn(Throwable t) {
		t.printStackTrace();
	}

	@Override
	public void error(Throwable t) {
		t.printStackTrace(System.err);
	}
}
