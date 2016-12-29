package co.q64.vpn.task;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.dbutils.handlers.BeanListHandler;

import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.objects.UserData;
import co.q64.vpn.util.TimeUtil;

@Singleton
public class AccountSweeperTask implements Runnable {
	private @Inject TimeUtil time;
	private @Inject Database database;
	private @Inject Logger logger;

	@Override
	public void run() {
		try {
			String name = database.getTableName(UserData.class);
			long targetTime = time.getTargetTerminationTime();
			int removed = 0;
			StringBuilder statement = new StringBuilder();
			statement.append("SELECT * FROM ");
			statement.append(name);
			statement.append(" WHERE `endTime` < ?");
			List<UserData> lst = database.getQueryRunner().query(statement.toString(), new BeanListHandler<UserData>(UserData.class), System.currentTimeMillis());
			for (UserData ud : lst) {
				if (ud.getEndTime() < targetTime) {
					StringBuilder delstatement = new StringBuilder();
					delstatement.append("DELETE FROM ");
					delstatement.append(name);
					delstatement.append(" WHERE `id` = ?");
					removed += database.getQueryRunner().update(delstatement.toString(), ud.getId());
				}
			}
			if (removed > 0) {
				logger.info("Removed " + removed + " expired accounts");
			}
		} catch (Exception e) {
			logger.error("Error checking database for expired accounts");
			logger.error(e);
		}
	}
}
