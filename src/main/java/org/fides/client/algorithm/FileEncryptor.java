package org.fides.client.algorithm;

import java.io.InputStream;
import java.security.Key;

public interface FileEncryptor {

	public abstract void encrypt(InputStream instream, Key key);

	public abstract void decrypt(InputStream instream, Key key);

}
