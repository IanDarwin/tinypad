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

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * Simple text editor.
 * @author Ian Darwin - modernized, rewritten as TinyPad
 * @author Learning Tree Course 471 Development Team - original EzWriter
 */
public class TinyPad extends JFrame {
	private static final long serialVersionUID = 7333922195749071327L;

	private boolean mUnsavedChanges;
	private JTextArea mTextArea = new JTextArea(40, 70);
	private JFileChooser mFileChooser = new JFileChooser("/");
	private File mKnownFile = null;

	// Undo/Redo support using built-in Undo Manager
	private UndoManager mUndoManager = new UndoManager();
	private UndoAction undoAction;
	private RedoAction redoAction;

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
				mTextArea.append("\n"); // XXX platform-dependent(?)
			}
			setmUnsavedChanges(false); // It's now same as on disk
			mKnownFile = file;
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

		Image iconImage = Toolkit.getDefaultToolkit().getImage("/images/Logo.png");
		setIconImage(iconImage);

		setLayout(new BorderLayout());

		add(BorderLayout.CENTER, new JScrollPane(mTextArea));

		// Don't close automatically, WindowCloser will exit when done.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				doIfNoUnsavedChanges(() -> { setVisible(false); dispose(); System.exit(0); });
			}
		});

		// setup the menu
		JMenuBar myMenuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');

		JMenuItem newMenuItem = new JMenuItem("New");
		newMenuItem.addActionListener(e-> { doIfNoUnsavedChanges(() -> {
			mTextArea.setText("");
			mKnownFile = null;
			setmUnsavedChanges(false);
			});
		});
		fileMenu.add(newMenuItem);

		JMenuItem openMenuItem = new JMenuItem("Open...");
		openMenuItem.addActionListener(openActionListener);
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		openMenuItem.setMnemonic('o');
		fileMenu.add(openMenuItem);

		JMenuItem saveMenuItem = new JMenuItem("Save...");
		saveMenuItem.addActionListener(saveActionListener);
		saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveMenuItem.setMnemonic('s');
		fileMenu.add(saveMenuItem);

		fileMenu.addSeparator();
		
		JMenuItem closeMenuItem = new JMenuItem("Close");
		closeMenuItem.addActionListener(e-> {
			doIfNoUnsavedChanges(() -> { setVisible(false); dispose(); System.exit(0); });
		});
		closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		closeMenuItem.setMnemonic('w');
		fileMenu.add(closeMenuItem);

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(e-> {
			doIfNoUnsavedChanges(() -> { setVisible(false); dispose(); System.exit(0); });
		});
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		exitMenuItem.setMnemonic('q');
		fileMenu.add(exitMenuItem);

		myMenuBar.add(fileMenu);
		
		JMenu editMenu = new JMenu("Edit");
		
		// Copy/Cut/Paste using built-in support; see
		// https://docs.oracle.com/javase/tutorial/uiswing/dnd/textpaste.html
		JMenuItem copyItem = new JMenuItem(new DefaultEditorKit.CopyAction());
		copyItem.setText("Copy");
		copyItem.setMnemonic(KeyEvent.VK_C);
		editMenu.add(copyItem);
		JMenuItem cutItem = new JMenuItem(new DefaultEditorKit.CutAction());
		cutItem.setText("Cut");
		cutItem.setMnemonic(KeyEvent.VK_X);
		editMenu.add(cutItem);
		JMenuItem pasteItem = new JMenuItem(new DefaultEditorKit.PasteAction());
		pasteItem.setText("Paste");
		pasteItem.setMnemonic(KeyEvent.VK_V);
		editMenu.add(pasteItem);
		
		myMenuBar.add(editMenu);

		// Set up Undo/Redo actions
		undoAction = new UndoAction();
		editMenu.add(undoAction);
		undoAction.updateGuiState();

		redoAction = new RedoAction();
		editMenu.add(redoAction);
		redoAction.updateGuiState();
		
		this.setJMenuBar(myMenuBar);
		
		// Get notified when the text is changed.
		mTextArea.getDocument().addUndoableEditListener(e -> {
			//Remember the edit and update the menus
	        mUndoManager.addEdit(e.getEdit());
	        undoAction.updateGuiState();
	        redoAction.updateGuiState();
		});

		// Setup the toolbar
		JToolBar toolBar = new JToolBar();
		ImageIcon openIcon = new ImageIcon(getClass().getResource("/images/open-64x64.png"));
		JButton openButton = new JButton(openIcon);
		openButton.setToolTipText("Open");
		openButton.addActionListener(openActionListener);
		toolBar.add(openButton);

		ImageIcon saveIcon = new ImageIcon(getClass().getResource("/images/save-64x64.png"));
		JButton saveButton = new JButton(saveIcon);
		saveButton.setToolTipText("Save");
		saveButton.addActionListener(saveActionListener);
		toolBar.add(saveButton);

		add(toolBar, BorderLayout.NORTH);

	}
	
	class UndoAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent evt) {
		    try {
		        mUndoManager.undo();
		    } catch (CannotUndoException e) {
		        JOptionPane.showMessageDialog(TinyPad.this, "Unable to undo: " + e, "Error", JOptionPane.ERROR_MESSAGE);
		        e.printStackTrace();
		    }
		    updateGuiState();
		    redoAction.updateGuiState();
		}

		/** Could be inlined but must be called from RedoAction so must be a method */
		void updateGuiState() {
			final boolean canUndo = mUndoManager.canUndo();
			setEnabled(canUndo);
			putValue(NAME, canUndo ? mUndoManager.getUndoPresentationName() : "Undo");
			setmUnsavedChanges(canUndo);
		}
	};
	
	class RedoAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent evt) {
		    try {
		        mUndoManager.redo();
		    } catch (CannotRedoException e) {
		    	JOptionPane.showMessageDialog(TinyPad.this, "Unable to redo: " + e, "Error", JOptionPane.ERROR_MESSAGE);
		        e.printStackTrace();
		    }
		    updateGuiState();
		    undoAction.updateGuiState();
		}

		void updateGuiState() {
			setEnabled(mUndoManager.canRedo());
			putValue(NAME, mUndoManager.canRedo() ? mUndoManager.getRedoPresentationName() : "Redo");
		}
	};

	/**
	 * Set saved/unsaved status variable AND titlebar
	 */
	public void setmUnsavedChanges(boolean unsavedChanges) {
		if (this.mUnsavedChanges == unsavedChanges) {
			// Redundant, so just ignore it.
			return;
		}
		if (unsavedChanges) {	// Add unsaved tag
			setTitle("*" + " " + getTitle());
		} else {				// Chop unsaved tag
			setTitle(getTitle().substring(2));
		}
		this.mUnsavedChanges = unsavedChanges;
	}

	private final String[] unsavedOptions = {"Save", "Discard", "Cancel" };
	private final static int UNSAVED_OPTION_SAVE = 0; // index into above
	private final static int UNSAVED_OPTION_DISCARD = 1; // index into above
	
	/**
	 *  Check for unsaved changes; if so, prompt.
	 *  When approved, hide the window, disposes resources, and exit.
	 */
	public void doIfNoUnsavedChanges(Runnable r) {
		if (mUnsavedChanges) {
			int ret = JOptionPane.showOptionDialog(TinyPad.this,
					"You have unsaved Changes!", "Warning",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
					null, unsavedOptions, unsavedOptions[0]);
			switch(ret) {
			case UNSAVED_OPTION_SAVE: // Save
				saveActionListener.actionPerformed(null);
				if (mUnsavedChanges) { // ie., save failed
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
		// We got here, so run the Runnable.
		r.run();
	}

	/**
	 *  This code allows the user to select a file.
	 */
	ActionListener openActionListener = (e) -> {
		doIfNoUnsavedChanges(() -> {
			// show the open dialog
			int ret = mFileChooser.showOpenDialog(TinyPad.this);

			// retrieve the file name selected by the user
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = mFileChooser.getSelectedFile();
				open(file);
			}
		});
	};

	/**
	 *  Allow the user to save a file
	 */
	ActionListener saveActionListener = e -> {
		if (mKnownFile != null) {
			mFileChooser.setSelectedFile(mKnownFile);
		}
		// show the save dialog
		int ret = mFileChooser.showSaveDialog(TinyPad.this);

		// grab the filename chosen by the user
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = mFileChooser.getSelectedFile();

			try (PrintWriter pout = new PrintWriter(file)) {
				pout.print(mTextArea.getText());
				setmUnsavedChanges(false);	// changes got written out
			} catch (IOException e1) {
				JOptionPane.showMessageDialog(this, "Write Failure" + e1, "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	};
}
