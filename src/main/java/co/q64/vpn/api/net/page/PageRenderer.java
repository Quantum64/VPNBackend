package co.q64.vpn.api.net.page;

import co.q64.vpn.api.objects.DatabaseTable;

/**
 * Renders a page on the server
 * @author Dylan
 *
 */
public interface PageRenderer {
	/**
	 * Renders page for user
	 * @param data the user to render the page for
	 * @return the completed html
	 */
	public String render(DatabaseTable data);
}
