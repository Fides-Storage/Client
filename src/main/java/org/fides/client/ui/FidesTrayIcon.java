package org.fides.client.ui;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.ApplicationHandler;
import org.fides.client.files.FileSyncManager;
import org.fides.client.ui.settings.SettingsFrame;

/**
 * This class is responsible for adding an icon to the systemtray. This icon has a context menu with the options to open
 * the Fides folder, open the settings and close the application.
 */
public class FidesTrayIcon {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(FileSyncManager.class);

	private final ApplicationHandler appHandler;

	/**
	 * The constructor. It needs a FileSyncManager to prevent critical actions from being interrupted.
	 * 
	 * @param appHandler
	 * The handler which is responsible for starting up and killing the application's threads.
	 */
	public FidesTrayIcon(ApplicationHandler appHandler) {
		this.appHandler = appHandler;
	}

	/**
	 * Adds the icon to the system tray.
	 */
	public void addToSystemTray() {
		// Check if the SystemTray is supported
		if (!SystemTray.isSupported()) {
			log.debug("SystemTray is not supported");
			return;
		}

		Image icon16 = (new ImageIcon(getClass().getResource("/icon16.png"), "tray icon")).getImage();
		Image icon24 = (new ImageIcon(getClass().getResource("/icon24.png"), "tray icon")).getImage();
		Image icon32 = (new ImageIcon(getClass().getResource("/icon32.png"), "tray icon")).getImage();
		Image icon64 = (new ImageIcon(getClass().getResource("/icon64.png"), "tray icon")).getImage();

		final PopupMenu popup = new PopupMenu();

		final SystemTray tray = SystemTray.getSystemTray();

		TrayIcon trayIcon;
		switch (tray.getTrayIconSize().width) {
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

		// Open the Fides folder when the icon is double clicked.
		trayIcon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				appHandler.openFolder();
			}
		});

		// Open the Fides folder when Open Folder is selected
		MenuItem openFolderItem = new MenuItem("Open Folder");
		openFolderItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				appHandler.openFolder();
			}
		});

		// Open the Fides folder when Open Folder is selected\
		final CheckboxMenuItem pauseItem = new CheckboxMenuItem("Pause syncing");
		pauseItem.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (pauseItem.getState()) {
					appHandler.stopApplication();
				} else {
					appHandler.startApplication();
				}
			}
		});

		// Open settings when Settings is selected
		MenuItem settingsItem = new MenuItem("Settings");
		settingsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SettingsFrame(appHandler);
			}
		});

		// Exits the application when Exit is selected
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				appHandler.exit();
			}
		});

		// Add components to pop-up menu
		popup.add(openFolderItem);
		popup.add(pauseItem);
		popup.add(settingsItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			log.error("TrayIcon could not be added.");
		}
	}
}
