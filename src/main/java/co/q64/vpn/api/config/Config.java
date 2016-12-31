package co.q64.vpn.api.config;

/**
 * Represents a file configuration that contains data about dynamic program options
 * @author Dylan
 *
 */
public interface Config {
	/**
	 * Called once when the program starts, should be used for loading file configuration into memory
	 */
	public void load();

	/**
	 * Gets the port the web server should listen on
	 * @return the port number
	 */
	public int getServerPort();

	/**
	 * Gets the path of the JKS that contains our SSL cert
	 * @return the JKS path
	 */
	public String getJKSPath();

	/**
	 * Gets the password to decrypt the above JKS
	 * @return the JKS password
	 */
	public String getJKSPassword();

	/**
	 * Gets the URL users will access the server from
	 * @return the URL
	 */
	public String getServerURL();

	/**
	 * Gets the hostname for the MySQL server
	 * @return the MySQL server hostname
	 */
	public String getSQLHost();

	/**
	 * Gets the port for the MySQL server
	 * @return the port for the MySQL server
	 */
	public int getSQLPort();

	/**
	 * Gets the database name that tables will be created in
	 * This database should be empty for new installations of this software
	 * @return the name of the database
	 */
	public String getSQLDatabaseName();

	/**
	 * Gets the MySQL username for the user to connect to the MySQL database above
	 * This user only needs full permission for the above database
	 * @return the username for the MySQL server
	 */
	public String getSQLUsername();

	/**
	 * Gets the password to login to the MySQL database with the given user
	 * @return the MySQL password
	 */
	public String getSQLPassword();

	/**
	 * Gets the OAuth id for the integration clients will use to login to the service
	 * @return the oauth id
	 */
	public String getOAuthID();

	/**
	 * Gets the OAuth secret that corresponds with the above id
	 * @return the OAuth secret
	 */
	public String getOAuthSecret();

	/**
	 * Gets the Paypal id for express checkout
	 * @return the Paypal id
	 */
	public String getPaypalID();

	/**
	 * Gets the Paypal secret that corresponds with the above id
	 * @return the Paypal secret
	 */
	public String getPaypalSecret();
}
