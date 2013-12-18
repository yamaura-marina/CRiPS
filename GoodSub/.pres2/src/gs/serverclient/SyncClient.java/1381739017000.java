package gs.serverclient;

import gs.connection.Connection;
import gs.connection.DivideRoom;
import gs.connection.SendObjectList;
import gs.frame.GSFrame;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JScrollBar;

public class SyncClient {

	public static void main(String[] args) {
		new SyncClient().run();
	}

	private Connection conn;
	private GSFrame frame = new GSFrame();
	private int roomNum;
	private String text;
	private SendObjectList sendList = new SendObjectList();

	public void run() {

		// �e�L�X�g�G���A�̏�����
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.setBounds(100, 100, 300, 300);
		// frame.setTitle("Client4");

		// �L�[�������[�X���ꂽ���̓���i�e�L�X�g�G���A�j
		frame.getTextArea().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				String text = frame.getTextArea().getText();
				// conn.write(text);
				sendList.setSource(text);
				conn.write(sendList);
			}
		});

		// �h���b�O���ꂽ���̓���i�X�N���[���o�[�j
		frame.getvBar().addMouseMotionListener(new MouseAdapter() {
			public void mouseDragged(MouseEvent e) {
				JScrollBar vBar = frame.getvBar();
				int value = vBar.getValue();
				sendList.setScrollVal(value);
				// conn.write(value);
				conn.write(sendList);
			}
		});

		frame.open();

		// �ڑ�
		try (Socket sock = new Socket("localhost", 10000)) {
			conn = new Connection(sock);
			newConnectionOpened(conn);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void newConnectionOpened(Connection conn) {

		Object obj;

		conn.shakehandForClient();

		// �����ԍ��ݒ�
		DivideRoom room = new DivideRoom();
		try {
			roomNum = room.selectRoomNum();
		} catch (IOException e) {
			e.printStackTrace();
		}
		frame.setTitle("Client4[" + roomNum + "]");

		sendList.setRoomNum(roomNum);
		// �X�N���[���o�[��value�Ǝ��ʂ��邽�߂Ƀ}�C�i�X��
		// conn.write(roomNum * (-1));
		conn.write(sendList);

		System.out.println("client established");
		try {
			while (true) {
				obj = conn.read();
				if (obj instanceof String) { // String�Ȃ�e�L�X�g�G���A�ɕ\��
					String text = (String) obj;
					frame.getTextArea().setText(text);
				} else if (obj instanceof Integer) { // Integer�Ȃ�X�N���[���o�[����
					int value = (int) obj;
					frame.setvBar(value);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		conn.close();
		System.out.println("client closed");
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void keyReleased() {
		conn.write(text);
	}

	public void setFrame(GSFrame frame) {
		this.frame = frame;
	}

}