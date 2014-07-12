package ch.view;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import ronproeditor.REApplication;
import ch.conn.framework.CHConnection;
import ch.conn.framework.CHUserState;
import ch.conn.framework.packets.CHFilelistRequest;
import ch.conn.framework.packets.CHLogoutRequest;
import ch.library.CHFileSystem;
import clib.common.filesystem.CDirectory;
import clib.common.filesystem.CFileFilter;

public class CHMemberSelectorFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String CH_DIR_PATH = "runtime-EclipseApplication/.ch";
	private static final int SYNC_BUTTON_INDEX = 5;
	private static final int PULL_BUTTON_INDEX = 6;

	private String user;
	private List<JButton> buttons = new ArrayList<JButton>();
	private CHConnection conn;
	private HashMap<String, REApplication> openedCHEditors = new HashMap<>();
	private HashMap<REApplication, String> openedUsers = new HashMap<>();
	private REApplication application = new REApplication();
	private List<CHUserState> userStates = new ArrayList<>();

	public CHMemberSelectorFrame(String user) {
		this.user = user;
	}

	public CHMemberSelectorFrame(String user, CHConnection conn) {
		this.user = user;
		this.conn = conn;
	}

	public void open() {
		this.setTitle("CheCoProMemberSelector " + user);
		this.setBounds(100, 100, 150, 500);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.addWindowListener(msWindowListner);
		this.setVisible(true);
	}

	// TODO MSが閉じたときLogout処理
	private WindowListener msWindowListner = new WindowAdapter() {

		@Override
		public void windowClosing(WindowEvent e) {
			conn.write(new CHLogoutRequest(user));
			close();
		}

		@Override
		public void windowClosed(WindowEvent e) {

		}
	};

	public void close() {
		application.doClose();
		this.dispose();
	}

	public void setMembers(List<CHUserState> userStates) {

		this.getContentPane().removeAll();
		JPanel buttonPanel = new JPanel();
		this.getContentPane().add(buttonPanel);

		for (final CHUserState aUserState : userStates) {
			JButton button = new JButton(aUserState.getUser());
			button.setBackground(aUserState.getColor());

			buttonPanel.add(button);

			if (!aUserState.isLogin()) {
				button.setForeground(Color.RED);
			}

			button.addActionListener(buttonAction);

			buttons.add(button);
		}
		this.getContentPane().validate();

	}

	private ActionListener buttonAction = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {

			String pushed = e.getActionCommand();

			if (pushed.equals(user)) {
				// TODO eclipse active
			} else if (openedCHEditors.containsKey(pushed)) {
				openedCHEditors.get(pushed).getFrame().toFront();
			} else {
				conn.write(new CHFilelistRequest(pushed));

				REApplication chApplication = application
						.doOpenNewRE(CH_DIR_PATH + "/" + pushed);
				openedCHEditors.put(pushed, chApplication);
				openedUsers.put(chApplication, pushed);
				initCHEditor(chApplication);
			}
		}
	};

	public void initCHEditor(REApplication application) {
		initCHEMenubar(application);
		initCHWindow(application);
	}

	private void initCHEMenubar(final REApplication application) {

		JMenuBar menuBar = application.getFrame().getJMenuBar();
		menuBar.add(initSyncButton(application));
		menuBar.add(initPullButton(application));
		application.getFrame().setJMenuBar(menuBar);
	}

	private JToggleButton initSyncButton(final REApplication application) {

		final JToggleButton syncButton = new JToggleButton("同期中", true);

		syncButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				if (syncButton.isSelected()) {
					conn.write(new CHFilelistRequest(user));
					application.doRefresh();
					((JButton) application.getFrame().getJMenuBar()
							.getComponent(PULL_BUTTON_INDEX)).setEnabled(false);
					syncButton.setText("同期中");
				} else if (!syncButton.isSelected()) {
					((JButton) application.getFrame().getJMenuBar()
							.getComponent(PULL_BUTTON_INDEX)).setEnabled(true);
					syncButton.setText("非同期中");
				}

				if (application.getFrame().getEditor() != null) {
					application.getFrame().getEditor().getViewer()
							.getTextPane()
							.setEditable(!syncButton.isSelected());
				}
			}
		});

		return syncButton;
	}

	private JButton initPullButton(final REApplication application) {

		JButton pullButton = new JButton("取り込み↓");
		pullButton.setEnabled(false);
		pullButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				CHPullDialog pullDialog = new CHPullDialog(openedUsers
						.get(application));
				pullDialog.open();

				boolean java = pullDialog.isJavaChecked();
				boolean material = pullDialog.isMaterialCecked();
				if (java || material) {
					doPull(openedUsers.get(application),
							makeCFileFilter(java, material));
				}
			}
		});

		return pullButton;
	}

	private CFileFilter makeCFileFilter(boolean java, boolean material) {
		if (java && material) {
			return CFileFilter.IGNORE_BY_NAME_FILTER(".*", "*.class", ".*xml");
		} else if (java && !material) {
			return CFileFilter.ACCEPT_BY_NAME_FILTER("*.java");
		} else if (!java && material) {
			return CFileFilter.IGNORE_BY_NAME_FILTER(".*", "*.class", "*.xml",
					"*.java");
		}
		return null;
	}

	private void doPull(String user, CFileFilter filter) {
		CDirectory from = CHFileSystem.getEclipseMemberDir(user);
		CDirectory to = CHFileSystem.getEclipseProjectDir();
		CHFileSystem.pull(from, to, filter);
	}

	private void initCHWindow(final REApplication application) {

		application.getFrame().setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		List<WindowListener> listeners = new ArrayList<WindowListener>();
		listeners = Arrays.asList(application.getFrame().getWindowListeners());
		for (WindowListener aListener : listeners) {
			application.getFrame().removeWindowListener(aListener);
		}

		application.getFrame().addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				String user = openedUsers.get(application);
				openedUsers.remove(application);
				openedCHEditors.remove(user);
				setMembers(userStates);
			}
		});
	}

	public boolean cheackCHEditor(String sender, String currentFileName) {

		// senderのCHEditorが開かれていない
		if (!openedCHEditors.containsKey(sender)) {
			return false;
		} else {
			JToggleButton syncButton = (JToggleButton) openedCHEditors
					.get(sender).getFrame().getJMenuBar()
					.getComponent(SYNC_BUTTON_INDEX);
			// 非同期中
			if (!syncButton.isSelected()) {
				return false;
			}
		}

		// editorが開いていない
		if (openedCHEditors.get(sender).getFrame().getEditor() == null) {
			return false;
		}

		// 開いているファイル名が一致していない
		if (!openedCHEditors.get(sender).getSourceManager().getCurrentFile()
				.getName().equals(currentFileName)) {
			return false;
		}
		return true;
	}

	public void showSource(final String sender, final String source) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				openedCHEditors.get(sender).getFrame().getEditor()
						.setText(source);
			}
		});
	}

	public List<JButton> getButtons() {
		return buttons;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setUserStates(List<CHUserState> userStates) {
		this.userStates = userStates;
	}

	public static void main(String[] args) {
		CHMemberSelectorFrame frame = new CHMemberSelectorFrame("name");
		List<CHUserState> userStates = new ArrayList<CHUserState>();
		userStates.add(new CHUserState("user1", true, Color.CYAN));
		userStates.add(new CHUserState("name", true, Color.LIGHT_GRAY));
		userStates.add(new CHUserState("user2", false, Color.MAGENTA));
		frame.open();
		frame.setMembers(userStates);
		userStates.add(new CHUserState("user3", true, Color.YELLOW));
		frame.setMembers(userStates);

	}
}