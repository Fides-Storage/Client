package org.fides.client.ui.settings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.UiUtils;
import org.fides.client.ui.UserMessage;

public class ChangeFolderPanel extends SettingsJPanel implements ActionListener {

	private static final Logger LOG = LogManager.getLogger(ChangeFolderPanel.class);

	private final JTextField fidesFolderField = new JTextField();

	private final String initialFolder;

	public ChangeFolderPanel() {
		super("Fides folder");

		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JLabel labelFolder = new JLabel("Location:");
		this.add(UiUtils.leftAlignBox(labelFolder));

		// Adding a panel with the folderfield and the selection button.
		JPanel folderPicker = new JPanel();
		folderPicker.setLayout(new BoxLayout(folderPicker, BoxLayout.LINE_AXIS));

		UiUtils.setMaxHeightToPreferred(fidesFolderField);
		String fileDirectory = "";
		try {
			fileDirectory = UserProperties.getInstance().getFileDirectory().getCanonicalPath();
		} catch (IOException e) {
			LOG.error(e);
		}
		initialFolder = fileDirectory;
		fidesFolderField.setText(initialFolder);
		fidesFolderField.setAlignmentY(Component.TOP_ALIGNMENT);
		folderPicker.add(fidesFolderField);

		JButton folderButton = new JButton("Select");
		Dimension buttonDimension = folderButton.getSize();
		buttonDimension.height = fidesFolderField.getPreferredSize().height - 1;
		folderButton.setMaximumSize(buttonDimension);
		folderButton.setAlignmentY(Component.TOP_ALIGNMENT);

		folderButton.addActionListener(this);

		folderPicker.add(folderButton);
		this.add(folderPicker);
	}

	@Override
	public List<UserMessage> applySettings() {
		// TODO Auto-generated method stub
		return null;
	}

	public void actionPerformed(ActionEvent event) {
		JFileChooser chooser = new JFileChooser();

		File lastSelected = new File(fidesFolderField.getText());
		if (lastSelected.exists()) {
			chooser.setCurrentDirectory(lastSelected);
		} else {
			chooser.setCurrentDirectory(UserProperties.getInstance().getFileDirectory());
		}
		chooser.setDialogTitle("Select a Fides folder");

		// Let the user select only directories and disable the "All files" option.
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				fidesFolderField.setText(chooser.getSelectedFile().getCanonicalPath());
			} catch (IOException e) {
				LOG.error(e);
			}
		}
	}
}
