package com.darwinsys.tinypad;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Simple text editor.
 * @author Ian Darwin - modernized, rewritten as TinyPad
 * @author Learning Tree Course 471 Development Team - original EzWriter
 */
public class TinyPad extends JFrame {

	private static final long serialVersionUID = 7333922195749071327L;
	private boolean unsavedChanges;
	
	private JTextArea mTextArea = new JTextArea(40, 70);
	private JLabel mStatus = new JLabel();
	private JFileChooser mFileChooser = new JFileChooser("/");

	/** Main program, starts the ball rolling. */
	public static void main(String[] args) {

		// Some things are best done before starting up Swing:
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		TinyPad app = new TinyPad();
		app.pack();
		app.setVisible(true);
		if (args.length > 0) {
			app.open(new File(args[0])); // XXX Create multiple views if > 1 file?
		}
	}

	/**
	 *  Opens a file and displays the contents in the component "myTextArea"
	 */
	protected void open(File file) {
		try (BufferedReader is = new BufferedReader(new FileReader(file))) {
			//  Clear the text as we are loading new text
			mTextArea.setText("");
			//  Read in the data and display in the JTextArea "mTextArea"
			String line = null;
			while ((line = is.readLine()) != null) {
				mTextArea.append(line);
				mTextArea.append("\n"); // XXX platform-dependent
			}
			setUnsavedChanges(false); // It's now same as on disk
			setTitle("TinyPad - " + file.getPath());
		} catch(IOException e) {
			JOptionPane.showMessageDialog(this, "Read error: " + e);
		}
	}

	/**
	 *  Construct the GUI
	 */
	public TinyPad() {

		setTitle(TinyPad.class.getSimpleName());

		// Get notified when the text is changed.
		mTextArea.getDocument().addDocumentListener(changeListener);

		Image iconImage = Toolkit.getDefaultToolkit().getImage("/images/Logo.png");
		setIconImage(iconImage);

		setLayout(new BorderLayout());

		add(BorderLayout.CENTER, new JScrollPane(mTextArea));

		// Don't close automatically, WindowCloser will exit when done.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			// Delegate all work to exit();
			public void windowClosing(WindowEvent e) {
					exit();
			}
		});

		// setup the menu
		JMenuBar myMenuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");

		JMenuItem openMenuItem = new JMenuItem("Open...");
		openMenuItem.addActionListener(openActionListener);
		fileMenu.add(openMenuItem);

		JMenuItem saveMenuItem = new JMenuItem("Save...");
		saveMenuItem.addActionListener(saveActionListener);
		fileMenu.add(saveMenuItem);

		fileMenu.addSeparator();
		
		JMenuItem closeMenuItem = new JMenuItem("Close");
		closeMenuItem.addActionListener(e-> { exit(); });
		fileMenu.add(closeMenuItem);

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(e-> { exit(); });
		fileMenu.add(exitMenuItem);

		myMenuBar.add(fileMenu);
		this.setJMenuBar(myMenuBar);

		// Setup the toolbar
		JToolBar toolBar = new JToolBar();
		ImageIcon openIcon = new ImageIcon(getClass().getResource("/images/open-128x128.png"));
		JButton openButton = new JButton(openIcon);
		openButton.setToolTipText("Open");
		openButton.addActionListener(openActionListener);
		toolBar.add(openButton);

		ImageIcon saveIcon = new ImageIcon(getClass().getResource("/images/save-128x128.png"));
		JButton saveButton = new JButton(saveIcon);
		saveButton.setToolTipText("Save");
		saveButton.addActionListener(saveActionListener);
		toolBar.add(saveButton);

		add(toolBar, BorderLayout.NORTH);
		
		mStatus.setText("TinyPad Ready");
		add(mStatus, BorderLayout.SOUTH);

		//  Set up mnemonics and accelerators
		fileMenu.setMnemonic('f');

		// set-up CTRL-O and CMD-O for open
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		openMenuItem.setMnemonic('o');

		// set-up CTRL-S and CMD-S for save
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveMenuItem.setMnemonic('s');
		
		// set-up CTRL-W and CMD-W to close one window
		closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		closeMenuItem.setMnemonic('w');

		// set-up CTRL-Q and CMD-Q for exit
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		exitMenuItem.setMnemonic('q');
	}

	DocumentListener changeListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {
			setUnsavedChanges(true);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			setUnsavedChanges(true);
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setUnsavedChanges(true);
		}		
	};

	/**
	 * Set saved/unsaved status variable AND titlebar
	 */
	public void setUnsavedChanges(boolean unsavedChanges) {
		if (this.unsavedChanges == unsavedChanges) {
			// Redundant, so just ignore it.
			return;
		}
		if (unsavedChanges) {	// Add unsaved tag
			setTitle("*" + " " + getTitle());
		} else {				// Chop unsaved tag
			setTitle(getTitle().substring(2));
		}
		this.unsavedChanges = unsavedChanges;
	}

	private final String[] unsavedOptions = {"Save", "Discard", "Cancel" };
	private final static int UNSAVED_OPTION_SAVE = 0; // index into above
	private final static int UNSAVED_OPTION_DISCARD = 1; // index into above
	
	/**
	 *  Check for unsaved changes; if so, prompt. 
	 *  When approved, hide the window, disposes resources, and exit.
	 */
	public void exit() {
		if (unsavedChanges) {
			int ret = JOptionPane.showOptionDialog(TinyPad.this, 
					"You have unsaved Changes!", "Warning",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, 
					null, unsavedOptions, unsavedOptions[0]);
			switch(ret) {
			case UNSAVED_OPTION_SAVE: // Save
				saveActionListener.actionPerformed(null);
				if (unsavedChanges) { // ie., save failed
					return;
				}
				break;
			case UNSAVED_OPTION_DISCARD: // Discard
				break;
				// NOTREACHED
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
				// Do nothing; in particular, do not exit.
				return;
			default:
				throw new IllegalStateException("Unexpected: " + ret);
			}
		}
		setVisible(false);
		dispose();
		System.exit(0);
	}

	/**
	 *  This code allows the user to select a file.
	 */
	ActionListener openActionListener = (e) -> {

		// show the open dialog
		int ret = mFileChooser.showOpenDialog(TinyPad.this);

		// retrieve the file name selected by the user
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = mFileChooser.getSelectedFile();
			open(file);
		}
	};

	/**
	 *  Allow the user to save a file
	 */
	ActionListener saveActionListener = e -> {
		// show the save dialog
		int ret = mFileChooser.showSaveDialog(TinyPad.this);

		// grab the filename chosen by the user
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = mFileChooser.getSelectedFile();

			try (PrintWriter pout = new PrintWriter(file)) {
				pout.print(mTextArea.getText());
				setUnsavedChanges(false);	// changes got written out
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, "Write Failure" + e1, "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	};
}
