package org.fides.client.ui.settings;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;
import org.fides.client.tools.UserProperties;
import org.fides.client.ui.UserMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PrepareForTest({ UserProperties.class, Files.class })
@PowerMockIgnore({ "javax.management.*", "javax.swing.*" })
@RunWith(PowerMockRunner.class)
public class ChangeFolderPanelTest {

	private File mainDir;

	private File testDir;

	private ChangeFolderPanel panel;

	private UserProperties userPropMock;

	private JTextField newFolderFieldMock;
	
	@Before
	public void setUp() throws Exception {
		mainDir = UserProperties.getInstance().getFileDirectory();
		testDir = new File(mainDir, "Test");
		if (testDir.exists()) {
			FileUtils.deleteDirectory(testDir);
		}
		assertTrue(testDir.mkdirs());

		userPropMock = Mockito.mock(UserProperties.class);
		setFileDirectory(testDir);

		PowerMockito.mockStatic(UserProperties.class);
		Mockito.when(UserProperties.getInstance()).thenReturn(userPropMock);

		panel = new ChangeFolderPanel();

		newFolderFieldMock = mock(JTextField.class);
		Whitebox.setInternalState(panel, "fidesFolderField", newFolderFieldMock);
		setFolderField(testDir.getCanonicalPath());
	}

	private void setFileDirectory(File fileDirectory) {
		Mockito.when(userPropMock.getFileDirectory()).thenReturn(fileDirectory);
	}

	private void setFolderField(String newDirectory) {
		when(newFolderFieldMock.getText()).thenReturn(newDirectory);
	}

	@Test
	public void testChangeFolder() throws IOException {
		// Create old directory with a file
		File oldDir = new File(testDir, "ChangeFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		assertTrue(oldDir.mkdir());
		File file = new File(oldDir, "ChangeFolder-File");
		file.createNewFile();
		setFileDirectory(oldDir);

		// Create empty new directory
		File newDir = new File(testDir, "ChangeFolder-NewDir");
		if (newDir.exists()) {
			FileUtils.deleteDirectory(newDir);
		}
		assertTrue(newDir.mkdirs());
		setFolderField(newDir.getPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();
		File newFileLocation = new File(newDir, file.getName());

		assertFalse(oldDir.exists());
		assertFalse(file.exists());
		assertTrue(newFileLocation.exists());
		assertTrue(result.isEmpty());

		// Make sure the file directory was changed
		Mockito.verify(userPropMock).setFileDirectory(newDir);
	}

	@Test
	public void testSameFolder() throws IOException {
		PowerMockito.mockStatic(Files.class);
		
		// Create old directory with a file
		File oldDir = new File(testDir, "ChangeFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		assertTrue(oldDir.mkdir());
		File file = new File(oldDir, "ChangeFolder-File");
		file.createNewFile();
		setFileDirectory(oldDir);
		setFolderField(oldDir.getCanonicalPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();
		assertTrue(result.isEmpty());
		
		// Make sure the move was not called
		PowerMockito.verifyStatic(Mockito.times(0));
		Files.move(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class));
		
		// Make sure the file directory wasn't changed
		Mockito.verify(userPropMock, Mockito.never()).setFileDirectory(Mockito.any(File.class));
	}
	
	@Test
	public void testToFilledFolder() throws IOException {
		PowerMockito.mockStatic(Files.class);
		
		// Create old directory with a file
		File oldDir = new File(testDir, "ToFilledFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		assertTrue(oldDir.mkdir());
		setFileDirectory(oldDir);

		// Create empty new directory
		File newDir = new File(testDir, "ToFilledFolder-NewDir");
		if (newDir.exists()) {
			FileUtils.deleteDirectory(newDir);
		}
		assertTrue(newDir.mkdirs());
		File fillFile = new File(newDir, "ToFilledFolder-File");
		fillFile.createNewFile();
		setFolderField(newDir.getCanonicalPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();

		assertTrue(oldDir.exists());
		assertFalse(result.isEmpty());
		
		// Make sure the move was not called
		PowerMockito.verifyStatic(Mockito.times(0));
		Files.move(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class));
		
		// Make sure the file directory wasn't changed
		Mockito.verify(userPropMock, Mockito.never()).setFileDirectory(Mockito.any(File.class));
	}
	
	@Test
	public void testFromNullFolder() throws IOException {
		PowerMockito.mockStatic(Files.class);
		
		// Create old directory with a file
		File oldDir = new File(testDir, "FromNullFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		setFileDirectory(oldDir);

		// Create empty new directory
		File newDir = new File(testDir, "FromNullFolder-NewDir");
		if (newDir.exists()) {
			FileUtils.deleteDirectory(newDir);
		}
		assertTrue(newDir.mkdir());
		setFolderField(newDir.getPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();

		assertTrue(result.isEmpty());
		
		// Make sure the move was not called
		PowerMockito.verifyStatic(Mockito.times(0));
		Files.move(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class));
		
		// Make sure the file directory was changed
		Mockito.verify(userPropMock).setFileDirectory(newDir);
	}
	
	@Test
	public void testToNullFolder() throws IOException {
		PowerMockito.mockStatic(Files.class);
		
		// Create old directory with a file
		File oldDir = new File(testDir, "ToNullFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		assertTrue(oldDir.mkdir());
		setFileDirectory(oldDir);

		// Create empty new directory
		File newDir = new File(testDir, "ToNullFolder-NewDir");
		if (newDir.exists()) {
			FileUtils.deleteDirectory(newDir);
		}
		setFolderField(newDir.getCanonicalPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();

		assertFalse(result.isEmpty());
		
		// Make sure the move was not called
		PowerMockito.verifyStatic(Mockito.times(0));
		Files.move(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class));

		// Make sure the file directory wasn't changed
		Mockito.verify(userPropMock, Mockito.never()).setFileDirectory(Mockito.any(File.class));
	}
	
	@Test
	public void testToInnerFolder() throws IOException {
		PowerMockito.mockStatic(Files.class);
		
		// Create old directory with a file
		File oldDir = new File(testDir, "ToInnerFolder-OldDir");
		if (oldDir.exists()) {
			FileUtils.deleteDirectory(oldDir);
		}
		assertTrue(oldDir.mkdir());
		setFileDirectory(oldDir);

		// Create empty new directory
		File newDir = new File(oldDir, "ToInnerFolder-NewDir");
		if (newDir.exists()) {
			FileUtils.deleteDirectory(newDir);
		}
		assertTrue(newDir.mkdirs());
		setFolderField(newDir.getCanonicalPath());

		// Apply the Change Folder settings
		List<UserMessage> result = panel.applySettings();

		assertTrue(oldDir.exists());
		assertFalse(result.isEmpty());
		
		// Make sure the move was not called
		PowerMockito.verifyStatic(Mockito.times(0));
		Files.move(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class));
		
		// Make sure the file directory wasn't changed
		Mockito.verify(userPropMock, Mockito.never()).setFileDirectory(Mockito.any(File.class));
	}
	
	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(testDir);
		mainDir = null;
		testDir = null;
		panel = null;
		userPropMock = null;
		newFolderFieldMock = null;
	}
}
