package co.q64.vpn.database;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;

import co.q64.vpn.api.config.Config;
import co.q64.vpn.api.database.Database;
import co.q64.vpn.api.log.Logger;
import co.q64.vpn.api.objects.DatabaseTable;
import co.q64.vpn.objects.UserData;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

@Singleton
public class MySQLDatabase implements Database {
	public static final String DEFAULT_NAME = "default";
	private final String DRIVER_NAME = "com.mysql.jdbc.Driver";

	private @Inject Logger logger;
	private @Inject Config config;

	private boolean valid;
	private List<Class<? extends DatabaseTable>> tables = new ArrayList<Class<? extends DatabaseTable>>();
	private Map<Class<?>, List<DatabaseTable>> data = new ConcurrentHashMap<Class<?>, List<DatabaseTable>>();
	private QueryRunner runner;
	private ScheduledExecutorService updater;
	private ExecutorService updateTasks;

	@Override
	public void addTable(Class<? extends DatabaseTable> clazz) {
		tables.add(clazz);
	}

	@Override
	public QueryRunner getQueryRunner() {
		return runner;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public void initDatabase() {
		this.updater = Executors.newSingleThreadScheduledExecutor();
		this.updateTasks = Executors.newCachedThreadPool();
		try {
			this.runner = new QueryRunner(getDataSource());
			boolean success = DbUtils.loadDriver(DRIVER_NAME);
			if (!success) {
				logger.error("Could not initilize the SQL Driver");
				valid = false;
				return;
			}
			if (!createTables()) {
				logger.error("Could not create tables");
				valid = false;
				return;
			}
		} catch (Exception e) {
			logger.error("Could not init SQL");
			e.printStackTrace();
			valid = false;
			return;
		}
		updater.scheduleAtFixedRate(new DataUpdateTask(), 500L, 500L, TimeUnit.MILLISECONDS);
		valid = true;
	}

	@Override
	public <T> T getData(Class<T> dataType, String id) {
		List<DatabaseTable> list = getDataList(dataType);
		if (list == null) {
			return null;
		}
		for (DatabaseTable pd : list) {
			if (pd.getId().equalsIgnoreCase(id)) {
				return dataType.cast(pd);
			}
		}
		try {
			DatabaseTable a = DatabaseTable.class.cast(dataType.getDeclaredConstructor().newInstance());
			a.setId(id);
			return dataType.cast(a);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// logger.error(e);
		}
		return null;
	}

	public <T> List<DatabaseTable> getDataList(Class<T> dataType) {
		if (!DatabaseTable.class.isAssignableFrom(dataType)) {
			logger.error("Could not get data instance because " + dataType.getSimpleName() + " is not an instance of data!");
			return null;
		}
		List<DatabaseTable> list = data.get(dataType);
		if (list == null) {
			list = new CopyOnWriteArrayList<DatabaseTable>();
			data.put(dataType, list);
			// Console.CONSOLE.log("Added new data list for data object type " +
			// dataType.getSimpleName());
		}
		return list;
	}

	public void disconnect(String id) {
		for (Class<?> clazz : data.keySet()) {
			@SuppressWarnings("unchecked")
			Class<DatabaseTable> casted = (Class<DatabaseTable>) clazz;
			if (data.get(clazz).remove(getData(casted, id))) {
				// Console.CONSOLE.log("Removed " + clazz.getSimpleName() + " for " +
				// u.toString() + " on disconnect");
			} else {
				// Bukkit.getLogger().warn("Did NOT remove " +
				// clazz.getSimpleName() + " for " + u.toString() + " on
				// disconnect");
			}
		}
	}

	/*
	 * public boolean enableCustomTable(Class<? extends DatabaseTable>
	 * bean) { if (!createTable(CoreAPI.getGameName() + "Mod", bean)) { return
	 * false; } tables.add(bean); return true; }
	 * 
	 * 
	 * public boolean enableTablePost(String name, Class<? extends
	 * DatabaseTable> bean) { if (!createTable(name, bean)) { return
	 * false; } tables.add(bean); return true; }
	 */

	public void queryData(String id) {
		try {
			ExecutorService executor = Executors.newCachedThreadPool();
			for (Class<? extends DatabaseTable> data : tables) {
				Future<?> task = executor.submit(new GetData(id, getTableName(data), new BeanHandler<>(data), data));
				executor.execute(new TimeoutGet(task, 4));
			}
			executor.shutdown();
			executor.awaitTermination(6, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Player login thread interrupted unexpectedly!");
			logger.error(e);
		}
	}

	private Map<String, Object> introspect(Object obj) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		try {
			Class<?> clazz = obj.getClass();
			if (!clazz.getSuperclass().getName().equals(DatabaseTable.class.getName())) {
				clazz = clazz.getSuperclass();
			}
			List<Field> declaredFields = Arrays.asList(clazz.getDeclaredFields());
			for (Field f : declaredFields) {
				f.setAccessible(true);
				Object value = f.get(obj);
				result.put(f.getName(), value);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean createTables() {
		boolean success = true;
		for (Class<? extends DatabaseTable> data : tables) {
			if (!createTable(getTableName(data), data)) {
				success = false;
			}
		}
		return success;
	}

	public String getTableName(Class<? extends DatabaseTable> data) {
		try {
			DatabaseTable inst = data.newInstance();
			return inst.getTableName();
		} catch (IllegalAccessException | IllegalArgumentException | SecurityException | InstantiationException e) {
			// logger.error("Error reflecting table name!", e);
		}
		return null;
	}

	private boolean createTable(String name, Class<?> columns) {
		List<Field> declaredFields = Arrays.asList(columns.getDeclaredFields());
		StringBuilder values = new StringBuilder();
		values.append("(");
		for (Field f : declaredFields) {
			values.append("`");
			values.append(f.getName());
			values.append("`");
			if (f.getType().isAssignableFrom(String.class)) {
				if (f.getName().endsWith("LL")) {
					values.append(" longtext");
				} else if (f.getName().endsWith("L")) {
					values.append(" mediumtext");
				} else {
					values.append(" varchar(255)");
				}
			} else if (f.getType().isAssignableFrom(float.class)) {
				values.append(" float");
			} else if (f.getType().isAssignableFrom(int.class)) {
				values.append(" int");
			} else if (f.getType().isAssignableFrom(long.class)) {
				values.append(" bigint");
			} else {
				continue;
			}
			if (declaredFields.indexOf(f) == (declaredFields.size() - 1)) {
				values.append(", PRIMARY KEY(id))");
			} else {
				values.append(", ");
			}
		}
		StringBuilder statement = new StringBuilder();
		statement.append("CREATE TABLE IF NOT EXISTS ");
		statement.append(name);
		statement.append(" ");
		statement.append(values.toString());
		statement.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8");
		try {
			runner.update(statement.toString());
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private MysqlDataSource getDataSource() {
		MysqlDataSource result = new MysqlDataSource();
		result.setUrl("jdbc:mysql://" + config.getSQLHost() + ":" + config.getSQLPort() + "/" + config.getSQLDatabaseName());
		result.setUser(config.getSQLUsername());
		result.setPassword(config.getSQLPassword());
		result.setCharacterEncoding("UTF-8");
		return result;
	}

	private void addData(final DatabaseTable add) {
		DatabaseTable enhanced = DatabaseTable.class.cast(Enhancer.create(add.getClass(), new SetterInvocationHandler()));
		Class<?> dataClass = enhanced.getClass();
		for (Entry<String, Object> field : introspect(add).entrySet()) {
			try {
				Field f = dataClass.getSuperclass().getDeclaredField(field.getKey());
				f.setAccessible(true);
				f.set(enhanced, field.getValue());
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				// logger.error("Error cloning data object to
				// proxy", e);
			}
		}
		// for (Field f : dataClass.getDeclaredFields()) {
		// Console.CONSOLE.log(f.getName());
		// }
		List<DatabaseTable> list = data.get(dataClass.getSuperclass());
		if (list == null) {
			list = new ArrayList<DatabaseTable>();
			data.put(dataClass.getSuperclass(), list);
			// Console.CONSOLE.log("Added new data list for data object type " +
			// dataClass.getSimpleName());
		}
		// Console.CONSOLE.log("Added player data type " + dataClass.getSimpleName() + "
		// for " + enhanced.getName());
		list.add(enhanced);
	}

	//public void manualUpdate(Class<? extends DatabaseTable> clazz, UUID player) {
	//	DatabaseTable data = getData(clazz, player);
	//	if (data != null && !data.getName().equals(DEFAULT_NAME)) {
	//		updateTasks.submit(new UpdateData(data, tablePrefix + data.getTableName(), true));
	//	}
	//}

	public void deleteData(DatabaseTable data) {
		updateTasks.submit(new RemoveData(data.getId(), data.getTableName()));
	}

	public class UpdateData implements Runnable {

		private DatabaseTable data;
		private String tableName;
		private boolean saveMode;

		public UpdateData(DatabaseTable data, String tableName) {
			this.data = data;
			this.tableName = tableName;
			this.saveMode = false;
		}

		public UpdateData(DatabaseTable data, String tableName, boolean saveMode) {
			this.data = data;
			this.tableName = tableName;
			this.saveMode = saveMode;
		}

		@Override
		public void run() {
			try {
				boolean exists = runner.query("SELECT * FROM " + tableName + " WHERE id=?", new ResultSetHandler<Boolean>() {
					@Override
					public Boolean handle(ResultSet rs) throws SQLException {
						return rs.next();
					}
				}, data.getId());
				Map<String, Object> introspected = introspect(data);
				if (exists) {
					if (!(data instanceof UserData) || !Boolean.valueOf(((UserData) data).getIsNew())) {
						StringBuilder statement = new StringBuilder();

						statement.append("UPDATE ");
						statement.append(tableName);
						statement.append(" SET ");
						Iterator<String> keys = introspected.keySet().iterator();
						while (keys.hasNext()) {
							String s = keys.next();
							statement.append(s);
							if (keys.hasNext()) {
								statement.append("=?, ");
							} else {
								statement.append("=? WHERE id=");
								statement.append("'");
								statement.append(data.getId());
								statement.append("'");
							}
						}
						runner.update(statement.toString(), introspected.values().toArray());
					}
				} else {
					StringBuilder statement = new StringBuilder();
					statement.append("INSERT INTO ");
					statement.append(tableName);
					statement.append(" VALUES(");
					Iterator<String> keys = introspected.keySet().iterator();
					while (keys.hasNext()) {
						keys.next();
						if (keys.hasNext()) {
							statement.append("?, ");
						} else {
							statement.append("?)");
						}
					}
					runner.update(statement.toString(), introspected.values().toArray());
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public class GetData implements Runnable {

		private String id;
		private String tableName;
		private BeanHandler<? extends DatabaseTable> definedHandler;
		private Class<? extends DatabaseTable> bean;

		public GetData(String id, String tableName, BeanHandler<? extends DatabaseTable> definedHandler, Class<? extends DatabaseTable> bean) {
			this.id = id;
			this.definedHandler = definedHandler;
			this.tableName = tableName;
			this.bean = bean;
		}

		@Override
		public void run() {
			try {
				DatabaseTable data = runner.query("SELECT * FROM " + tableName + " WHERE id=?", definedHandler, id.toString());
				if (data == null) {
					try {
						data = bean.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
					data.setId(id);
				}
				addData(data);
			} catch (SQLException e) {
				logger.error("Problem getting data:" + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public class RemoveData implements Runnable {
		private String itemId;
		private String tableName;

		public RemoveData(String itemId, String tableName) {
			this.itemId = itemId;
			this.tableName = tableName;
		}

		@Override
		public void run() {
			try {
				StringBuilder statement = new StringBuilder();
				statement.append("DELETE FROM ");
				statement.append(tableName);
				statement.append(" WHERE `id` = ?");
				runner.update(statement.toString(), itemId.toString());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private class TimeoutGet implements Runnable {

		private Future<?> task;
		private int timeout;

		public TimeoutGet(Future<?> task, int timeout) {
			this.task = task;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			try {
				task.get(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				// Bukkit.getLogger().warn("A task timed out!", e);
			}
		}
	}

	private class SetterInvocationHandler implements MethodInterceptor {

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Object result = proxy.invokeSuper(obj, args);
			if ((method.getName().startsWith("set") || method.getName().startsWith("add") || method.getName().startsWith("remove") || method.getName().startsWith("delete")) && !method.getName().equals("setId")) {
				if (DatabaseTable.class.isAssignableFrom(obj.getClass())) {
					DatabaseTable data = DatabaseTable.class.cast(obj);
					data.needsUpdate.set(true);
					// Console.CONSOLE.log("Detected set operation on " +
					// method.getDeclaringClass().getSimpleName());
				} else {
					// Bukkit.getLogger().warn("An intercepted method from " +
					// method.getDeclaringClass().getSimpleName() + " was not an
					// instance of DatabaseTable! ");
				}
			}
			return result;
		}
	}

	private class DataUpdateTask implements Runnable {

		@Override
		public void run() {
			try {
				for (List<DatabaseTable> list : data.values()) {
					for (DatabaseTable data : list) {
						if (data.needsUpdate.compareAndSet(true, false)) {
							// Console.CONSOLE.log("Updated " +
							// data.getClass().getSimpleName() + " data for " +
							// data.getName());
							updateTasks.submit(new UpdateData(data, data.getTableName(), false));
						}
					}
				}
			} catch (Exception e) {
			}
		}
	}
}
