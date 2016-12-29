package co.q64.vpn.task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.dbutils.handlers.BeanListHandler;

import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.util.IPSECUpdater;
import co.q64.vpn.util.TimeUtil;

@Singleton
public class AccountSweeperTask implements Runnable {
	private @Inject TimeUtil time;
	private @Inject Database database;
	private @Inject Logger logger;
	private @Inject IPSECUpdater ipsec;

	@Override
	public void run() {
		try {
			String name = database.getTableName(UserData.class);
			long targetTime = time.getTargetTerminationTime();
			int removed = 0;
			StringBuilder terminateStatement = new StringBuilder();
			terminateStatement.append("SELECT * FROM ");
			terminateStatement.append(name);
			terminateStatement.append(" WHERE `endTime` < ?");
			List<UserData> lst = database.getQueryRunner().query(terminateStatement.toString(), new BeanListHandler<UserData>(UserData.class), targetTime);
			for (UserData ud : lst) {
				StringBuilder deleteStatement = new StringBuilder();
				deleteStatement.append("DELETE FROM ");
				deleteStatement.append(name);
				deleteStatement.append(" WHERE `id` = ?");
				removed += database.getQueryRunner().update(deleteStatement.toString(), ud.getId());
			}
			if (removed > 0) {
				logger.info("Terminated " + removed + " expired accounts");
			}
			removed = 0;
			StringBuilder accessStatement = new StringBuilder();
			accessStatement.append("SELECT * FROM ");
			accessStatement.append(name);
			accessStatement.append(" WHERE `endTime` < ?");
			List<UserData> l = database.getQueryRunner().query(accessStatement.toString(), new BeanListHandler<UserData>(UserData.class), System.currentTimeMillis());
			for (UserData ud : l) {
				if (ipsec.get(ud.getId()) != null) {
					ipsec.remove(ud.getId());
					removed++;
				}
			}
			if (removed > 0) {
				ipsec.updateNow();
				logger.info("Removed access for " + removed + " unpaid accounts");
			}
		} catch (Exception e) {
			logger.error("Error checking database for expired accounts");
			logger.error(e);
		}
	}
}
