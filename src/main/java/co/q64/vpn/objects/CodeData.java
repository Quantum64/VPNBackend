package co.q64.vpn.objects;

import co.q64.vpn.api.objects.DatabaseTable;

public class CodeData extends DatabaseTable {
	private String id;
	private String isValid;
	private String isPerm;
	private String usage;
	private int amount;

	public CodeData() {
		this.id = new String();
		this.isValid = String.valueOf(false);
		this.isPerm = String.valueOf(false);
		this.usage = CodeUsage.ACCESS.name();
		this.amount = 1;
	}

	@Override
	public String getTableName() {
		return "CodeData";
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public String getIsValid() {
		return isValid;
	}

	public void setIsValid(String isValid) {
		this.isValid = isValid;
	}

	public String getIsPerm() {
		return isPerm;
	}

	public void setIsPerm(String isPerm) {
		this.isPerm = isPerm;
	}

	public static enum CodeUsage {
		ACCESS, TIME
	}
}
