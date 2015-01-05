package org.fides.client.ui.settings;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.fides.client.files.FileSyncManager;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

/**
 * A frame containing the settings of the program
 */
public class SettingsFrame extends JFrame {

	private final FileSyncManager syncManager;

	/**
	 * Constructor, creates and shows the settings window
	 * 
	 * @param syncManager
	 *            The {@link FileSyncManager} to use
	 */
	public SettingsFrame(FileSyncManager syncManager) {
		super("Settings");
		this.syncManager = syncManager;

		final SettingsJPanel[] settingsPanels = {
			new ChangePasswordPanel(syncManager.getEncManager()),
			new CheckTimePanel(syncManager)
		};

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Create the individual setting screens
		Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
		TitledBorder tileBorder = BorderFactory.createTitledBorder(loweredetched, "title");
		Border border = BorderFactory.createCompoundBorder(tileBorder, emptyBorder);
		for (SettingsJPanel settingsJPanel : settingsPanels) {
			JPanel container = new JPanel(new GridLayout(1, 1));
			tileBorder.setTitle(settingsJPanel.getName());
			container.setBorder(border);
			container.add(settingsJPanel);
			panel.add(container);
		}

		// Apply button
		JButton applyButton = new JButton("Apply Settings");
		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonPanel.add(applyButton);
		panel.add(buttonPanel);

		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<UserMessage> messages = new ArrayList<>();
				for (SettingsJPanel settingsJPanel : settingsPanels) {
					messages.addAll(settingsJPanel.applySettings());
				}
				JPanel errorPanel = new JPanel();
				UiUtils.setMessageLabels(errorPanel, messages);
				JFrame errorFrame = new JFrame();
				errorFrame.setContentPane(errorPanel);
				errorFrame.pack();
				errorFrame.setLocationRelativeTo(null);
				errorFrame.setVisible(true);
			}
		});

		// Pack and show
		setContentPane(panel);
		pack();
		setResizable(false);
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
