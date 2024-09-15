import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class Client extends JFrame implements ActionListener {
    private BufferedReader in = null;
    private BufferedWriter out = null;
    private Socket socket = null;
    private JTextPane receiver = null;
    private JTextField sender = null;
    private JButton btnSend = null;
    private MyDialog dialog = null;
    private String nickname = null;

    public Client() {
        setTitle("Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());

        // TextPane for displaying messages
        receiver = new JTextPane();
        receiver.setEditable(false);
        receiver.setBackground(Color.WHITE);
        receiver.setFont(new Font("Arial", Font.PLAIN, 14));
        receiver.setMargin(new Insets(10, 10, 10, 10));

        // Message sending panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        sender = new JTextField();
        sender.setBorder(new EmptyBorder(10, 10, 10, 10));
        sender.setFont(new Font("Arial", Font.PLAIN, 14));
        sender.addActionListener(this);

        btnSend = new JButton("Send");
        btnSend.setFont(new Font("Arial", Font.BOLD, 12));
        btnSend.setBackground(new Color(0x00bfae)); // KakaoTalk blue-green color
        btnSend.setForeground(Color.WHITE);
        btnSend.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createLineBorder(new Color(0x00a89d), 1));
        btnSend.addActionListener(this);

        messagePanel.add(sender, BorderLayout.CENTER);
        messagePanel.add(btnSend, BorderLayout.EAST);
        c.add(new JScrollPane(receiver), BorderLayout.CENTER);
        c.add(messagePanel, BorderLayout.SOUTH);

        setSize(350, 500);
        setVisible(true);

        dialog = new MyDialog(this, "Join");
        dialog.setModal(true); // Modal dialog
        dialog.setVisible(true); // Show dialog
        nickname = dialog.getNickname();

        try {
            setupConnection();
        } catch (IOException e) {
            handleError(e.getMessage());
        }

        new Thread(new Receiver()).start();
    }

    private void setupConnection() throws IOException {
        setTitle(nickname);
        socket = new Socket("localhost", 9999);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        out.write(nickname + "\n");
        out.flush();
        appendMessage("Connected to Chat GPT", false);
    }

    private static void handleError(String message) {
        System.out.println(message);
        System.exit(1);
    }

    
    
    private void appendMessage(String message, boolean isSelfMessage) {
        try {
            StyledDocument doc = receiver.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();

            if (isSelfMessage) {
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                StyleConstants.setForeground(style, new Color(0x00bfae)); // KakaoTalk blue-green color
                // Remove the name prefix for self messages
                int colonIndex = message.indexOf(':');
                if (colonIndex != -1) {
                    message = message.substring(colonIndex + 1).trim(); // Remove prefix
                }
            } else {
                StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                StyleConstants.setForeground(style, Color.BLACK);
            }

            StyleConstants.setFontSize(style, 14);

            int length = doc.getLength();
            doc.insertString(length, message + "\n", style);
            doc.setParagraphAttributes(length, message.length() + 1, style, false);
            receiver.setCaretPosition(doc.getLength()); // Auto scroll to bottom
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Receiver implements Runnable {
        @Override
        public void run() {
            String msg;
            while (true) {
                try {
                    msg = in.readLine();
                    if (msg != null) {
                        // Display messages from server and other clients, but not self
                        if (!msg.startsWith(nickname + ":")) {
                            appendMessage(msg, false);
                        }
                    }
                } catch (IOException e) {
                    handleError(e.getMessage());
                }
            }
        }
    }

    private class MyDialog extends JDialog {
        JLabel lblNickname = null;
        JTextField tfNickname = null;
        JButton btnLogin = null;
        private String nickname = null;

        public MyDialog(JFrame frame, String title) {
            super(frame, title);
            this.setLayout(new FlowLayout());

            lblNickname = new JLabel("Screen Name : ");
            this.add(lblNickname);
            tfNickname = new JTextField(10);
            this.add(tfNickname);

            btnLogin = new JButton("Join");
            this.add(btnLogin);

            btnLogin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setNickname();
                }
            });

            // Add ActionListener to JTextField to handle Enter key press
            tfNickname.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setNickname();
                }
            });

            this.setSize(250, 120);
        }

        private void setNickname() {
            nickname = tfNickname.getText();
            if (!nickname.isEmpty()) {
                setVisible(false);
            }
        }

        public String getNickname() {
            return nickname;
        }
    }

    public static void main(String[] args) {
        new Client();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sender || e.getSource() == btnSend) {
            String msg = sender.getText();
            try {
                String messageToSend = nickname + ": " + msg;
                out.write(messageToSend + "\n");
                out.flush();
                appendMessage(messageToSend, true); // Append as self message
                sender.setText(""); // Clear the input field
            } catch (IOException e1) {
                handleError(e1.getMessage());
            }
        }
    }
}
