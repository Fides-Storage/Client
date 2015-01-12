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
	 * Creates a user friendly string with the certificate's information
	 *
	 * @param certificate
	 *            The certificate to convert to a pretty string.
	 * @return The user friendly string
	 */
	private static String readableCertificate(X509Certificate certificate) {
		StringBuilder builder = new StringBuilder();

		// Adds the key usages to the string.
		try {
			List<String> usages = certificate.getExtendedKeyUsage();
			builder.append("<b>This certificate has been verified for the following usages:</b>");
			if (usages != null) {
				for (String usage : usages) {
					builder.append("<br>");
					builder.append(usage);
				}
			} else {
				builder.append("<br> (none)");
			}
		} catch (CertificateParsingException e) {
			builder.append("<b>An exception has occurred while parsing the certificate usages.</b>");
		}

		builder.append("<table>");

		// Adds the Subject's principal to the string
		builder.append(createRow("Certificate issued for:"));
		builder.append(readablePrincipal(certificate.getSubjectX500Principal()));

		// Adds the Issuer's principal to the string
		builder.append(createRow("Certificate issued by:"));
		builder.append(readablePrincipal(certificate.getIssuerX500Principal()));

		// Adds the certificate's dates to the string
		builder.append(createRow("Issued on:", certificate.getNotBefore().toString()));

		builder.append(createRow("Expires on: ", certificate.getNotAfter().toString()));

		// Adds some extra info to the string
		builder.append(createRow("Certificate Serial Number:", certificate.getSerialNumber().toString()));

		builder.append(createRow("Certificate generated with:", certificate.getSigAlgName()));

		builder.append("</table>");
		return builder.toString();
	}

	/**
	 * Creates a user friendly string with the certificate's information
	 *
	 * @param principal
	 *            The principal to convert to a pretty string.
	 * @return The user friendly string
	 */
	private static String readablePrincipal(X500Principal principal) {
		String readable = principal.getName();
		readable = readable.substring(0, readable.indexOf(",L="));
		readable = readable.replace("CN=", "<tr><td>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Common Name: </td><td>") + "</td></tr>";
		readable = readable.replace(",OU=", "<tr><td>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Organization Unit: </td><td>") + "</td></tr>";
		readable = readable.replace(",O=", "<tr><td>" + WHITESPACE + WHITESPACE + WHITESPACE + WHITESPACE + "Organization: </td><td>") + "</td></tr>";
		return readable;
	}

	/**
	 * Create a row for inside the table
	 * 
	 * @param name
	 *            the first column which will be bolt
	 * @param values
	 *            the other columns which wont be bolt
	 * @return a row as string that can be placed inside a table
	 */
	private static String createRow(String name, String... values) {
		StringBuilder builder = new StringBuilder();
		builder.append("<tr>");
		builder.append(String.format("<td><b>%s</b></td>", name));
		for (String value : values) {
			builder.append(String.format("<td>%s</td>", value));
		}
		builder.append("</tr>");
		return builder.toString();
	}
}
