package org.fides.client.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.ui.ErrorMessageScreen;

/**
 * Contains the settings on where files should be saved.
 * 
 * @author Koen
 *
 */
public final class UserProperties {

	/**
	 * The directory to store settings
	 */
	public static final File SETTINGS_DIRECTORY = new File("./Settings");

	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(UserProperties.class);

	/**
	 * The default directory to store files
	 */
	private static final String DEFAULT_FILE_DIR = "./Fides";

	/**
	 * The file containing the user settings
	 */
	private static final String USER_SETTINGS_FILE = "user.properties";

	/**
	 * The key to get the file storage directory from the properties
	 */
	private static final String FILE_DIRECTORY_KEY = "FidesFiles";

	/**
	 * The key to get the user name
	 */
	private static final String USERNAMEHASH_KEY = "UsernameHash";

	/**
	 * The key to get the password hash
	 */
	private static final String PASSWORDHASH_KEY = "PasswordHash";

	/**
	 * The key to get the server host
	 */
	private static final String HOST_KEY = "Host";

	/**
	 * The key to get the server host port
	 */
	private static final String HOSTPORT_KEY = "HostPort";

	/**
	 * The certificate id
	 */
	private static final String CERTIFICATEID_KEY = "CertificateId";

	/**
	 * The certificate issuer
	 */
	private static final String CERTIFICATEISSUER_KEY = "CertificateIssuer";

	/**
	 * The time used to check changes with the server in seconds
	 */
	private static final String CHECK_TIME_KEY = "CheckTime";

	/**
	 * Singleton instance
	 */
	private static UserProperties instance;

	/**
	 * The properties used for storing the settings
	 */
	private Properties properties;

	/**
	 * Constructor, reads the {@link UserProperties}
	 */
	private UserProperties() {
		properties = new Properties();

		if (!SETTINGS_DIRECTORY.exists()) {
			if (!SETTINGS_DIRECTORY.mkdirs()) {
				log.error("Could not create settings directory");
				ErrorMessageScreen.showErrorMessage("Could not create settings directory.", "Make sure you have the rights to create files.");
				System.exit(1);
			}
		}

		try {
			File file = new File(SETTINGS_DIRECTORY, USER_SETTINGS_FILE);
			if (file.exists()) {
				properties.load(new FileInputStream(file));
			}
		} catch (IOException e) {
			log.error(e);
		}

		createFileDirectory();

	}

	/**
	 * Create the a file referencing the the location were the files should be saved
	 */
	private void createFileDirectory() {
		String fileDirectoryName = properties.getProperty(FILE_DIRECTORY_KEY);
		if (StringUtils.isBlank(fileDirectoryName)) {
			fileDirectoryName = DEFAULT_FILE_DIR;

			properties.setProperty(FILE_DIRECTORY_KEY, fileDirectoryName);

			saveProperties();
		}
		File fileDirectory = new File(fileDirectoryName);
		if (!fileDirectory.exists()) {
			if (!fileDirectory.mkdirs()) {
				log.error("File directory can not be created");
				ErrorMessageScreen.showErrorMessage("File directory can not be created.", "Make sure you have the rights to create files");
				System.exit(1);
			}
		}

	}

	public File getFileDirectory() {
		return new File(properties.getProperty(FILE_DIRECTORY_KEY));
	}

	public String getUsernameHash() {
		return properties.getProperty(USERNAMEHASH_KEY);
	}

	/**
	 * Save user name hash
	 * 
	 * @param usernameHash
	 *            the given user name hash to save
	 */
	public void setUsernameHash(String usernameHash) {
		properties.setProperty(USERNAMEHASH_KEY, usernameHash);
		saveProperties();
	}

	public String getPasswordHash() {
		return properties.getProperty(PASSWORDHASH_KEY);
	}

	/**
	 * Save password hash
	 * 
	 * @param passwordHash
	 *            the given password hash to save
	 */
	public void setPasswordHash(String passwordHash) {
		properties.setProperty(PASSWORDHASH_KEY, passwordHash);
		saveProperties();
	}

	public String getHost() {
		return properties.getProperty(HOST_KEY);
	}

	/**
	 * Get host port
	 * 
	 * @return host port, if empty turn 0
	 */
	public int getHostPort() {
		String hostPort = properties.getProperty(HOSTPORT_KEY);
		if (StringUtils.isNotBlank(hostPort) && StringUtils.isNumeric(hostPort)) {
			return Integer.parseInt(hostPort);
		}
		return 0;
	}

	/**
	 * Get server address of the server
	 * 
	 * @return server address of the server
	 */
	public InetSocketAddress getServerAddress() {
		if (StringUtils.isNotBlank(getHost()) && getHostPort() > 0 & getHostPort() <= 65535) {
			return new InetSocketAddress(getHost(), getHostPort());
		}
		return null;
	}

	/**
	 * Saves the server address to config file
	 * 
	 * @param serverAddress
	 *            address of the server
	 */
	public void setServerAddress(InetSocketAddress serverAddress) {
		properties.setProperty(HOST_KEY, serverAddress.getHostString());
		properties.setProperty(HOSTPORT_KEY, Integer.toString(serverAddress.getPort()));
		saveProperties();
	}

	/**
	 * Get certificate id
	 * 
	 * @return certificate id
	 */
	public String getCertificateId() {
		return properties.getProperty(CERTIFICATEID_KEY);
	}

	/**
	 * Get certificate issuer
	 * 
	 * @return certificate issuer
	 */
	public String getCertificateIssuer() {
		return properties.getProperty(CERTIFICATEISSUER_KEY);
	}

	/**
	 * Saves the certificate to config file
	 * 
	 * @param certificate
	 *            certificate to save
	 */
	public void setCertificate(X509Certificate certificate) {
		properties.setProperty(CERTIFICATEID_KEY, certificate.getSerialNumber().toString());
		properties.setProperty(CERTIFICATEISSUER_KEY, certificate.getIssuerX500Principal().getName());
		saveProperties();
	}

	/**
	 * Sets the check time
	 * 
	 * @param secondes
	 *            between checks
	 */
	public void setCheckTimeInSeconds(int secondes) {
		if (secondes >= 1) {
			properties.setProperty(CHECK_TIME_KEY, Integer.toString(secondes));
			saveProperties();
		}
	}

	/**
	 * Get the check time
	 * 
	 * @return The time used to check changes with the server in seconds
	 */
	public int getCheckTimeInSeconds() {
		String checkTime = properties.getProperty(CHECK_TIME_KEY);
		int parsedCheckTime = 0;
		if (StringUtils.isNotBlank(checkTime) && StringUtils.isNumeric(checkTime)) {
			parsedCheckTime = Integer.parseInt(checkTime);
		}

		/**
		 * Failback to 5 min if not set or incorrect
		 */
		if (parsedCheckTime <= 0) {
			parsedCheckTime = 300;
			setCheckTimeInSeconds(parsedCheckTime);
		}

		return parsedCheckTime;

	}

	/**
	 * Save the properties
	 */
	private void saveProperties() {
		try (OutputStream out = new FileOutputStream(new File(SETTINGS_DIRECTORY, USER_SETTINGS_FILE))) {
			properties.store(out, "Fides user settings");
		} catch (IOException e) {
			log.error(e);
		}
	}

	/**
	 * Returns the instance of the {@link UserProperties}, this is a singleton. If the {@link UserProperties} are not
	 * loaded they will be.
	 * 
	 * @return The instance of the {@link UserProperties}
	 */
	public static UserProperties getInstance() {
		if (instance == null) {
			instance = new UserProperties();
		}
		return instance;
	}
}
