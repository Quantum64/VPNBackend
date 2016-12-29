package co.q64.vpn.util;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import co.q64.vpn.objects.UserData;

@Singleton
public class TimeUtil {
	public long getAccountTerminationTime(UserData data) {
		return data.getEndTime() + TimeUnit.DAYS.toMillis(7);
	}

	public long getTargetTerminationTime() {
		return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
	}
}
