package org.fides.client.ui;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.FileSyncManager;
import org.fides.client.tools.UserProperties;

/**
 * This class is responsible for adding an icon to the systemtray. This icon has a context menu with the options to open
 * the Fides folder, open the settings and close the application.
 */
public class FidesTrayIcon {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileSyncManager.class);

	private final FileSyncManager syncManager;

	/**
	 * The constructor. It needs a FileSyncManager to prevent critical actions from being interrupted.
	 * 
	 * @param syncManager
	 */
	public FidesTrayIcon(FileSyncManager syncManager) {
		this.syncManager = syncManager;
	}

	/**
	 * Adds the icon to the system tray.
	 */
	public void addToSystemTray() {
		// Check if the SystemTray is supported
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}

		URL iconUrl = getClass().getResource("/resources/images/FidesIcon.png");
		Image icon = (new ImageIcon(iconUrl, "tray icon")).getImage();

		final PopupMenu popup = new PopupMenu();
		final TrayIcon trayIcon = new TrayIcon(icon);
		final SystemTray tray = SystemTray.getSystemTray();

		// Create a pop-up menu components
		MenuItem openFolderItem = new MenuItem("Open Folder");
		openFolderItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openFolder();
			}
		});

		MenuItem settingsItem = new MenuItem("Settings");
		settingsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSettings();
			}
		});

		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});

		// Add components to pop-up menu
		popup.add(openFolderItem);
		popup.add(settingsItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);
		trayIcon.setImageAutoSize(true);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
		}
	}

	/**
	 * Exits the Fides application
	 */
	private void exit() {
		try {
			syncManager.waitForStop();
			System.exit(0);
		} catch (InterruptedException e) {
			log.error("Interrupted Exception while trying to safely stop the FileSyncManager");
			System.exit(-1);
		}
	}

	/**
	 * Opens the Fides folder
	 */
	private void openFolder() {
		try {
			Desktop.getDesktop().open(UserProperties.getInstance().getFileDirectory());
		} catch (IOException e) {
			log.error("Couldn't open the folder");
		}
	}

	private void openSettings() {
		// TODO: Fill
	}
}
