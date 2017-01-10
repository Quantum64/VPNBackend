package co.q64.vpn.api.log;

/**
 * Allows messages to be logged to the program output
 * @author Dylan
 *
 */
public interface Logger {
	/**
	 * Logs an informational message to the program output
	 * @param message the message to log
	 */
	public void info(String message);
	
	/**
	 * Logs a warning to the program output
	 * @param message message the message to log
	 */
	public void warn(String message);
	
	/**
	 * Logs an error to the program output
	 * @param message message the message to log
	 */
	public void error(String message);
	
	/**
	 * Prints the stack trace of an exception as a warning to the program output
	 * @param t the exception to print
	 */
	public void warn(Throwable t);
	
	/**
	 * Prints the stack trace of an exception as an error to the program output
	 * @param t the exception to print
	 */
	public void error(Throwable t);
}
