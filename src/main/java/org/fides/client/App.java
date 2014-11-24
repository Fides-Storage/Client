package org.fides.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.fides.client.ui.CertificateValidationScreen;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) {
		
		Properties systemProps = System.getProperties();
		systemProps.put( "javax.net.ssl.trustStore", "C:/Users/Thijs/Documents/Informatica/Jaar 4/Fides/Certificate/cert/truststore.ts");
		systemProps.put("javax.net.ssl.trustStorePassword", "");
		System.setProperties(systemProps);

		SSLSocket sslsocket;
		try {
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			sslsocket = (SSLSocket) sslsocketfactory.createSocket("localhost", 4444);

			SSLContext context = SSLContext.getInstance("TLS");

			SSLSession session = sslsocket.getSession();
			java.security.cert.Certificate[] servercerts = session.getPeerCertificates();

			CertificateValidationScreen.validateCertificate((X509Certificate) servercerts[0]);

			sslsocket.close();
		} catch (UnknownHostException e) {
			fail("UnknownHostException");
		} catch (IOException e) {
			fail("IOException");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		System.out.println("Hello World!");
	}
}
