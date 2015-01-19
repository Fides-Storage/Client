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

import org.fides.client.ApplicationHandler;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

/**
 * A frame containing the settings of the program
 */
public class SettingsFrame extends JFrame {
	private static final long serialVersionUID = 4971171028483297323L;

	private final List<SettingsJPanel> settingsPanels = new ArrayList<>();

	/**
	 * Constructor, creates and shows the settings window
	 * 
	 * @param appHandler
	 *            The {@link ApplicationHandler} which is responsible for starting up and killing the application's
	 *            threads
	 */
	public SettingsFrame(final ApplicationHandler appHandler) {
		super("Settings");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JTabbedPane tabbedPane = new JTabbedPane();

		JPanel basePanel = new JPanel();
		basePanel.setLayout(new BoxLayout(basePanel, BoxLayout.PAGE_AXIS));

		// Create the individual setting screens
		JPanel generalTabPanel = new JPanel();
		tabbedPane.addTab("General", generalTabPanel);
		generalTabPanel.setLayout(new BoxLayout(generalTabPanel, BoxLayout.PAGE_AXIS));

		generalTabPanel.add(preparePanel(new ChangeServerPanel(appHandler.getSyncManager().getEncManager())));
		generalTabPanel.add(preparePanel(new CheckIntervalPanel()));

		generalTabPanel.add(Box.createVerticalGlue());

		// Create the individual setting screens
		JPanel folderTabPanel = new JPanel();
		tabbedPane.addTab("Folder", folderTabPanel);
		folderTabPanel.setLayout(new BoxLayout(folderTabPanel, BoxLayout.PAGE_AXIS));
		folderTabPanel.add(preparePanel(new ChangeFolderPanel()));
		folderTabPanel.add(Box.createVerticalGlue());

		// Create the individual setting screens
		JPanel userTabPanel = new JPanel();
		tabbedPane.addTab("User", userTabPanel);
		userTabPanel.setLayout(new BoxLayout(userTabPanel, BoxLayout.PAGE_AXIS));
		userTabPanel.add(preparePanel(new ChangePasswordPanel(appHandler.getSyncManager().getEncManager())));
		userTabPanel.add(Box.createVerticalGlue());

		// Apply button
		JButton applyButton = new JButton("Apply Settings");
		JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
		buttonPanel.add(applyButton);

		applyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean selfStopped = appHandler.stopApplication();

				ArrayList<UserMessage> messages = new ArrayList<>();
				for (SettingsJPanel settingsJPanel : settingsPanels) {
					List<UserMessage> panelMessages = settingsJPanel.applySettings();
					if (panelMessages != null) {
						messages.addAll(panelMessages);
					}
				}
				if (!messages.isEmpty()) {
					JPanel errorPanel = new JPanel();
					errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.PAGE_AXIS));
					UiUtils.setMessageLabels(errorPanel, messages);

					final JFrame errorFrame = new JFrame();

					final JButton confirmButton = new JButton("OK");
					confirmButton.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							errorFrame.dispose();
						}
					});
					errorPanel.add(confirmButton);

					errorFrame.setContentPane(errorPanel);
					errorFrame.setResizable(false);
					errorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
					errorFrame.pack();
					errorFrame.setVisible(true);
					errorFrame.setLocationRelativeTo(null);
				} else {
					SettingsFrame.this.dispose();
				}
				if (selfStopped) {
					appHandler.startApplication();
				}
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

	/**
	 * Prepares a {@link SettingsJPanel} for usage. this adds a border to the panel and add its to the list for settings
	 * to apply.
	 * 
	 * @param settingsPanel
	 *            The panel to prepare
	 * @return The prepared panel
	 */
	private JPanel preparePanel(SettingsJPanel settingsPanel) {
		settingsPanels.add(settingsPanel);

		Border etchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
		TitledBorder tileBorder = BorderFactory.createTitledBorder(etchedBorder, settingsPanel.getName());

		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
		container.setBorder(tileBorder);
		container.add(settingsPanel);
		return container;
	}
}
