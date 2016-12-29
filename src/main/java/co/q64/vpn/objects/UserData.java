package co.q64.vpn.objects;

import co.q64.vpn.api.objects.DatabaseTable;

public class UserData extends DatabaseTable {
	private String id;
	private String isNew;
	private String isAdmin;
	private String pass;
	private long joinTime;
	private long endTime;

	public UserData() {
		this.id = new String();
		this.isNew = String.valueOf(true);
		this.isAdmin = String.valueOf(false);
		this.pass = new String();
		this.joinTime = System.currentTimeMillis();
		this.endTime = System.currentTimeMillis();
	}

	@Override
	public String getTableName() {
		return "UserData";
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public long getJoinTime() {
		return joinTime;
	}

	public void setJoinTime(long time) {
		this.joinTime = time;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long time) {
		this.endTime = time;
	}

	public void setIsNew(String isNew) {
		this.isNew = isNew;
	}

	public String getIsNew() {
		return isNew;
	}

	public String getIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(String isAdmin) {
		this.isAdmin = isAdmin;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
}
