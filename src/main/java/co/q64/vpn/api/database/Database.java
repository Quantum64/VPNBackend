package co.q64.vpn.api.database;

import org.apache.commons.dbutils.QueryRunner;

import co.q64.vpn.api.objects.DatabaseTable;

/**
 * Represents a database
 * @author Dylan
 *
 */
public interface Database {
	public void addTable(Class<? extends DatabaseTable> data);

	public void initDatabase();

	public boolean isValid();

	public <T> T getData(Class<T> dataType, String id);

	public void queryData(String id);

	public void disconnect(String id);
	
	public void deleteData(DatabaseTable data);

	public QueryRunner getQueryRunner();

	public String getTableName(Class<? extends DatabaseTable> clazz);
}
