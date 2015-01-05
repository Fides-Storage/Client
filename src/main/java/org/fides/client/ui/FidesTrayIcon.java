package org.fides.client.ui;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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

		Image icon16 = (new ImageIcon(getClass().getResource("/icon16.png"), "tray icon")).getImage();
		Image icon24 = (new ImageIcon(getClass().getResource("/icon16.png"), "tray icon")).getImage();
		Image icon32 = (new ImageIcon(getClass().getResource("/icon16.png"), "tray icon")).getImage();
		Image icon64 = (new ImageIcon(getClass().getResource("/icon16.png"), "tray icon")).getImage();

		final PopupMenu popup = new PopupMenu();

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

		TrayIcon trayIcon;
		switch (SystemTray.getSystemTray().getTrayIconSize().width) {
		case 16:
			trayIcon = new TrayIcon(icon16);
			break;
		case 24:
			trayIcon = new TrayIcon(icon24);
			break;
		case 32:
			trayIcon = new TrayIcon(icon32);
			break;
		case 64:
			trayIcon = new TrayIcon(icon64);
			break;
		default:
			trayIcon = new TrayIcon(icon16);
			trayIcon.setImageAutoSize(true);
		}

		trayIcon.setPopupMenu(popup);
		Dimension iconDimension = trayIcon.getSize();
		log.debug("Tray Icon Size: " + iconDimension.width + "x" + iconDimension.height);

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
		// TODO: Check if supported
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
