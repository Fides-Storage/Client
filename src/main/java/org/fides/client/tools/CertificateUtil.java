package org.fides.client.tools;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A static class containing utils for certificates
 */
public final class CertificateUtil {
	/**
	 * Log for this class
	 */
	private static final Logger LOG = LogManager.getLogger(CertificateUtil.class);

	private CertificateUtil() {
	}

	/**
	 * Check if a certificate is valid
	 * 
	 * @param certificate
	 *            The certificate to check
	 * @return true if valid
	 */
	public static boolean checkValidCertificate(X509Certificate certificate) {
		try {
			certificate.checkValidity();
			// The rest of the checks are done by SSLSocket, if failed the socket is closed
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			LOG.error(e);
		}

		return false;
	}
}
