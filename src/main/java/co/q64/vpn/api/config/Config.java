package co.q64.vpn.api.config;

/**
 * Represents a file configuration that contains data about dynamic program options
 * @author Dylan
 *
 */
public interface Config {
	/**
	 * Gets the port the web server should listen on
	 * @return the port number
	 */
	public int getServerPort();

	/**
	 * Gets the hostname for the MySQL server
	 * @return the MySQL server hostname
	 */
	public String getSQLHost();

	/**
	 * Gets the port for the MySQL server
	 * @return the port for the MySQL server
	 */
	public String getSQLPort();

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
}
