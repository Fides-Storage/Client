package org.fides.client.ui.settings;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.files.FileSyncManager;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

/**
 * A frame containing the settings of the program
 */
public class SettingsFrame extends JFrame {
	/**
	 * Log for this class
	 */
	private static Logger log = LogManager.getLogger(SettingsFrame.class);

	private final FileSyncManager syncManager;

	private final List<SettingsJPanel> settingsPanels = new ArrayList<>();

	/**
	 * Constructor, creates and shows the settings window
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} to use
	 */
	public SettingsFrame(FileSyncManager syncManager) {
		super("Settings");
		this.syncManager = syncManager;

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel basePanel = new JPanel();
		basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.PAGE_AXIS));

		// Create the individual setting screens
		JPanel generalTabPanel = new JPanel();
		tabbedPane.addTab("General", generalTabPanel);
		generalTabPanel.setLayout(new BoxLayout(generalTabPanel, BoxLayout.PAGE_AXIS));
		generalTabPanel.add(createBorder(new ChangeServerPanel(syncManager.getEncManager().getConnector())));
		generalTabPanel.add(createBorder(new CheckIntervalPanel(syncManager)));
		generalTabPanel.add(Box.createVerticalGlue());

		// Create the individual setting screens
		JPanel userTabPanel = new JPanel();
		tabbedPane.addTab("User", userTabPanel);
		userTabPanel.setLayout(new BoxLayout(userTabPanel, BoxLayout.PAGE_AXIS));
		userTabPanel.add(createBorder(new ChangePasswordPanel(syncManager.getEncManager())));
		userTabPanel.add(Box.createVerticalGlue());

		// Apply button
		JButton applyButton = new JButton("Apply Settings");
		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonPanel.add(applyButton);

		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SettingsFrame.this.syncManager.waitForStop();
				} catch (InterruptedException e1) {
					log.error("waitForStop was interrupted", e);
					return;
				}
				ArrayList<UserMessage> messages = new ArrayList<>();
				for (SettingsJPanel settingsJPanel : settingsPanels) {
					List<UserMessage> panelMessages = settingsJPanel.applySettings();
					if (panelMessages != null) {
						messages.addAll(panelMessages);
					}
				}
				if (!messages.isEmpty()) {
					JPanel errorPanel = new JPanel();
					UiUtils.setMessageLabels(errorPanel, messages);
					JFrame errorFrame = new JFrame();
					errorFrame.setContentPane(errorPanel);
					errorFrame.pack();
					errorFrame.setLocationRelativeTo(null);
					errorFrame.setVisible(true);
				}
				SettingsFrame.this.syncManager.reenable();
			}
		});

		basePanel.add(tabbedPane);
		basePanel.add(buttonPanel);

		// Pack and show
		setContentPane(basePanel);
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private JPanel createBorder(SettingsJPanel settingsPanel) {
		settingsPanels.add(settingsPanel);

		Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		TitledBorder tileBorder = BorderFactory.createTitledBorder(loweredetched, settingsPanel.getName());

		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		container.setBorder(tileBorder);
		container.add(settingsPanel);
		return container;
	}
}
