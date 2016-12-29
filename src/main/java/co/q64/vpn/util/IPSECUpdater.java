package co.q64.vpn.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import co.q64.vpn.api.log.Logger;

import com.google.common.io.Files;

@Singleton
public class IPSECUpdater {
	private @Inject Logger logger;

	private File file;

	public IPSECUpdater() {
		file = new File("/etc/ipsec.secrets");
	}

	public String get(String username) {
		try {
			List<String> lines = Files.readLines(file, StandardCharsets.US_ASCII);
			for (Iterator<String> itr = lines.iterator(); itr.hasNext();) {
				String s = itr.next();
				if (s.contains(" :")) {
					if (s.split(Pattern.quote(" :"))[0].equals(username)) {
						return s.split(Pattern.quote("EAP "))[1].replace("\"", new String());
					}
				}
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return null;
	}

	public boolean update(String username, String password) {
		try {
			List<String> lines = Files.readLines(file, StandardCharsets.US_ASCII);
			for (Iterator<String> itr = lines.iterator(); itr.hasNext();) {
				String s = itr.next();
				if (s.contains(" :")) {
					if (s.split(Pattern.quote(" :"))[0].equals(username)) {
						itr.remove();
					}
				}
			}
			lines.add(username + " : EAP \"" + password + "\"");
			PrintWriter pw = new PrintWriter(file);
			for (String s : lines) {
				pw.println(s);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			logger.error(e);
			return false;
		}
		return true;
	}

	public boolean remove(String username) {
		try {
			List<String> lines = Files.readLines(file, StandardCharsets.US_ASCII);
			for (Iterator<String> itr = lines.iterator(); itr.hasNext();) {
				String s = itr.next();
				if (s.contains(" :")) {
					if (s.split(Pattern.quote(" :"))[0].equals(username)) {
						itr.remove();
					}
				}
			}
			PrintWriter pw = new PrintWriter(file);
			for (String s : lines) {
				pw.println(s);
			}
			pw.flush();
			pw.close();
		} catch (IOException e) {
			logger.error(e);
			return false;
		}
		return true;
	}

	public void updateNow() {
		try {
			Runtime.getRuntime().exec("ipsec rereadsecrets");
		} catch (IOException e) {
			logger.error(e);
		}
	}
}
