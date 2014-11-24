package org.fides.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.fides.client.connector.ServerConnector;
import org.fides.client.files.ClientFile;
import org.fides.client.files.FileCompareResult;
import org.fides.client.files.FileManager;
import org.fides.client.files.KeyFile;
import org.fides.client.ui.UsernamePasswordScreen;

/**
 * Hello world!
 *
 */
public class App {

  private static final String LOCAL_HASHSES_FILE = "./hashes.prop";

  /**
   * Main
   * 
   * @param args
   */
  public static void main(String[] args) {
    ServerConnector serverConnector = new ServerConnector();

    while (true) {

      String[] data = UsernamePasswordScreen.getUsernamePassword();

      if (data == null) {
        System.exit(1);
      }

      if ((data[0]).equals("register")) {

        // checks if password and password confirmation is the same
        if (data[2].equals(data[3])) {
          // register on the server
          if (serverConnector.register(data[1], data[2])) {
            System.out.println("Register succesfull");
          } else {
            System.out.println("Register failed");
          }
        } else {
          System.out.println("Register password confirmation is not valid.");
        }
      } else if ((data[0]).equals("login")) {
        if (serverConnector.login(data[1], data[2])) {
          break;
        } else {
          System.out.println("Login failed");
        }
      }

    }

    if (serverConnector.isConnected()) {
      // TODO: Do normal work
    } else {
      System.exit(1);

    }

  }

  public static void FileManagerCheck() {
    // TODO make it the real code, not half test code
    Properties localHashes = new Properties();
    try {
      File file = new File(LOCAL_HASHSES_FILE);
      if (file.exists()) {
        localHashes.loadFromXML(new FileInputStream(LOCAL_HASHSES_FILE));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    FileManager manager = new FileManager(localHashes);
    KeyFile keyFile = new KeyFile();
    keyFile.addClientFile(new ClientFile("Server.file", "gasdfa", null, null));
    keyFile.addClientFile(new ClientFile("SomeFiles2.txt", "gasdfa", null, null));

    Collection<FileCompareResult> results = manager.compareFiles(keyFile);
    System.out.println(results);

    System.out.println(UserSettings.getInstance().getFileDirectory());
  }

}
