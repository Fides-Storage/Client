package org.fides.client.ui.settings;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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

/**
 * A UI Panel that allows the user to change his Fides folder
 */
public class ChangeFolderPanel extends SettingsJPanel implements ActionListener {

	private static final long serialVersionUID = 308292657820504236L;

	private static final Logger LOG = LogManager.getLogger(ChangeFolderPanel.class);

	private final JTextField fidesFolderField = new JTextField();

	private final String initialFolder;

	/**
	 * The constructor for the {@link ChangeFolderPanel}. Adding this panel to a view shows the current selected folder
	 * and gives the user a {@link JFileChooser} to change his Fides folder
	 */
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

	/**
	 * An action performed which can be used to call the changeSelectedFolder function when the 'Select' button is
	 * pressed
	 */
	public void actionPerformed(ActionEvent e) {
		changeSelectedFolder();
	};

	private void changeSelectedFolder() {
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

	@Override
	public List<UserMessage> applySettings() {
		ArrayList<UserMessage> messages = new ArrayList<>();
		File selectedFolder = new File(fidesFolderField.getText());
		try {
			// If the folder is validated and it's not the same as the previously selected directory
			if (!messages.addAll(validateFolder(selectedFolder))) {
				File oldFolder = UserProperties.getInstance().getFileDirectory();
				File newFolder = selectedFolder;
				if (!oldFolder.exists() || oldFolder.getCanonicalPath() != newFolder.getCanonicalPath()) {
					if (!oldFolder.exists() || !newFolder.getParent().contains(oldFolder.getCanonicalPath())) {
						if (oldFolder.exists()) {
							// Move the old folder's contents to the new folder
							Files.move(oldFolder.toPath(), newFolder.toPath(), REPLACE_EXISTING);
						}
						UserProperties.getInstance().setFileDirectory(selectedFolder);
					} else {
						messages.add(new UserMessage("Selected folder cannot be a subfolder of the old folder", true));
					}
				}
			}
		} catch (IOException e) {
			LOG.error(e, e);
			messages.add(new UserMessage("Something unexpected went wrong", true));
		}
		return messages;
	}

	private List<UserMessage> validateFolder(File selectedFolder) {
		ArrayList<UserMessage> messages = new ArrayList<>();
		boolean changed = true;
		try {
			changed = selectedFolder.getCanonicalPath() != UserProperties.getInstance().getFileDirectory().getCanonicalPath();
		} catch (IOException e) {
			// Empty, if this fails it will handle the error correctly later.
		}
		if (changed) {
			if (selectedFolder.exists()) {
				if (selectedFolder.list().length > 0) {
					messages.add(new UserMessage("The selected folder has to be empty", true));
				}
			} else {
				messages.add(new UserMessage("The selected folder doesn't exist", true));
			}
		}
		return messages;
	}
}
