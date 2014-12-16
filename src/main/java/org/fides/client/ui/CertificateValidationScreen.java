package org.fides.client.ui;

import java.awt.Component;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;

/**
 * UI where a password can be submitted by a user
 */
public class CertificateValidationScreen {
	/**
	 * An html tab
	 */
	private static final String TAB = "&#09;";
	/**
	 * 2 html tabs
	 */
	private static final String DOUBLETAB = "&#09;&#09;";

	/**
	 * An html whitespace
	 */
	private static final String WHITESPACE = "&nbsp;";

	/**
	 * Show a dialog where the user can see and validate the certificate's information.
	 *
	 * @return whether the user accepts the server certificate
	 */
	public static boolean validateCertificate(X509Certificate certificate) {
		JFrame frame = new JFrame();
		frame.setUndecorated(true);
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);

		// Create a Panel
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Creates a titled border
		TitledBorder certificateBorder = new TitledBorder("Certificate");
		JPanel certificatePanel = new JPanel();
		certificatePanel.setBorder(certificateBorder);

		// Adds the certificate's information to the titled border
		JEditorPane certificateInfo = new JEditorPane("text/html", readableCertificate(certificate));
		certificatePanel.add(certificateInfo);
		panel.add(certificatePanel);

		// Adds a label
		JLabel acceptLabel = new JLabel("Do you trust and accept this server certificate?");
		acceptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		panel.add(acceptLabel);

		// Place the 2 buttons for Decline and Accept and show the dialog
		String[] options = new String[] { "Decline", "Accept" };
		int option = JOptionPane.showOptionDialog(frame, panel, "Validate Server Certificate", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

		frame.dispose();
		// If Accept was pressed, return true
		return option >= 0 && options[option].equals("Accept");
	}

	/**
	 * Creates a userfriendly string with the certificate's information
	 *
	 * @param certificate
	 *            The certificate to convert to a pretty string.
	 * @return The userfriendly string
	 */
	private static String readableCertificate(X509Certificate certificate) {
		StringBuilder builder = new StringBuilder();

		// Adds the key usages to the string.
		try {
			List<String> usages = certificate.getExtendedKeyUsage();
			builder.append("<b>This certificate has been verified for the following usages:</b>");
			if (usages != null) {
				for (String usage : usages) {
					builder.append("<br>" + TAB);
					builder.append(usage);
				}
			} else {
				builder.append("<br>" + TAB + "(none)");
			}
		} catch (CertificateParsingException e) {
			builder.append("<b>An exception has occurred while parsing the certificate usages.</b>");
		}

		// Adds the Subject's principal to the string
		builder.append("<br><br><b>Certificate issued for:</b>");
		builder.append(readablePrincipal(certificate.getSubjectX500Principal()));

		// Adds the Issuer's principal to the string
		builder.append("<br><br><b>Certificate issued by:</b>");
		builder.append(readablePrincipal(certificate.getIssuerX500Principal()));

		// Adds the certificate's dates to the string
		builder.append("<br><br><b>Issued on:</b>" + TAB + TAB);
		builder.append(certificate.getNotBefore());
		builder.append("<br><b>Expires on: </b>" + DOUBLETAB);
		builder.append(certificate.getNotAfter());

		// Adds some extra info to the string
		builder.append("<br><br><b>Certificate Serial Number:</b>" + TAB);
		builder.append(certificate.getSerialNumber());
		builder.append("<br><b>Certificate generated with:</b>" + TAB);
		builder.append(certificate.getSigAlgName());

		return builder.toString();
	}

	/**
	 * Creates a userfriendly string with the certificate's information
	 *
	 * @param principal
	 *            The principal to convert to a pretty string.
	 * @return The userfriendly string
	 */
	private static String readablePrincipal(X500Principal principal) {
		String readable = principal.getName();
		readable = readable.substring(0, readable.indexOf(",L="));
		readable = readable.replace("CN=", "<br>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Common Name: " + DOUBLETAB);
		readable = readable.replace(",OU=", "<br>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Organization Unit: " + TAB);
		readable = readable.replace(",O=", "<br>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Organization: " + DOUBLETAB);
		return readable;
	}

}
