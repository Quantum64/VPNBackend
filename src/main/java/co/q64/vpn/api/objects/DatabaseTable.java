package co.q64.vpn.api.objects;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DatabaseTable {
	public AtomicBoolean needsUpdate = new AtomicBoolean();

	public abstract String getId();

	public abstract void setId(String id);

	public abstract String getTableName();
}
