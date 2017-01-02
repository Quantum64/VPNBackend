package co.q64.vpn.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

public enum ApacheDefaultConfig {

	//formatter:off
	SERVER("server.properties",
			new ConfigurationValue(ConfigurationKeys.SERVER_PORT, 80),
			new ConfigurationValue(ConfigurationKeys.SERVER_JKS_PATH, "/ssl/keystore.jks"),
			new ConfigurationValue(ConfigurationKeys.SERVER_JKS_PASSWORD, "password"),
			new ConfigurationValue(ConfigurationKeys.SERVER_URL, "http://example.com")
			),
	SQL("mysql.properties",
			new ConfigurationValue(ConfigurationKeys.SQL_HOST, "localhosts"),
			new ConfigurationValue(ConfigurationKeys.SQL_PORT, 3306),
			new ConfigurationValue(ConfigurationKeys.SQL_DB, "VPNProvider"),
			new ConfigurationValue(ConfigurationKeys.SQL_USER, "root"),
			new ConfigurationValue(ConfigurationKeys.SQL_PASS, "password")
			),
	PAYPAL("paypal.properties",
			new ConfigurationValue(ConfigurationKeys.PAYPAL_ID, "paypalid"),
			new ConfigurationValue(ConfigurationKeys.PAYPAL_SECRET, "paypalsecret")
			);
	//formatter:on

	private String fileName;
	private ConfigurationValue[] values;
	private Configuration config;

	private ApacheDefaultConfig(String fileName, ConfigurationValue... values) {
		this.fileName = fileName;
		this.values = values;
	}

	public String getFileName() {
		return fileName;
	}

	public ConfigurationValue[] getValues() {
		return values;
	}

	public String loadFromFile() {
		Path path = Paths.get(fileName);
		File file = new File(path.toUri());
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String fullPath = file.getAbsolutePath();
		Parameters params = new Parameters();
		//formatter:off
		FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
				.configure(params.properties()
						.setFileName(fullPath)
						.setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
		//formatter:on
		try {
			config = builder.getConfiguration();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		fixDefaults();
		try {
			builder.save();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		return fullPath;
	}

	private void fixDefaults() {
		for (ConfigurationValue value : values) {
			if (config.containsKey(value.getKey())) {
				continue;
			}
			config.setProperty(value.getKey(), getDefaultValue(value.getKey()));
		}
	}

	public String getString(String key) {
		if (config.containsKey(key)) {
			return config.getString(key);
		}
		config.setProperty(key, getDefaultValue(key));
		return config.getString(key, (String) getDefaultValue(key));
	}

	public int getInt(String key) {
		if (config.containsKey(key)) {
			return config.getInt(key);
		}
		config.setProperty(key, getDefaultValue(key));
		return config.getInt(key, (int) getDefaultValue(key));
	}

	private Object getDefaultValue(String key) {
		for (ConfigurationValue value : values) {
			if (value.getKey().equals(key)) {
				return value.getValue();
			}
		}
		return null;
	}

	public static class ConfigurationValue {
		private String key;
		private Object value;

		public ConfigurationValue(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}
	}

	public static class ConfigurationKeys {
		public static String SERVER_PORT = "server-port";
		public static String SERVER_JKS_PATH = "jks-path";
		public static String SERVER_JKS_PASSWORD = "jks-password";
		public static String SERVER_URL = "server-url";
		public static String SQL_HOST = "mysql-host";
		public static String SQL_PORT = "mysql-port";
		public static String SQL_DB = "mysql-databse";
		public static String SQL_USER = "mysql-username";
		public static String SQL_PASS = "mysql-password";
		public static String PAYPAL_ID = "paypal-id";
		public static String PAYPAL_SECRET = "paypal-secret";
	}
}
