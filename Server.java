import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

public class Server extends JFrame implements ActionListener {
    private BufferedReader in = null;
    private BufferedWriter out = null;
    private ServerSocket listener = null;
    private Socket socket = null;
    private JTextPane receiver = null;
    private JTextField sender = null;
    private JButton btnSend = null;
    private List<BufferedWriter> clientOutputs = new ArrayList<>();
    private String clientNickname = null;

    public Server() {
        setTitle("Chat GPT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());

        receiver = new JTextPane();
        receiver.setEditable(false);
        receiver.setBackground(Color.WHITE);
        receiver.setFont(new Font("Arial", Font.PLAIN, 14));
        receiver.setMargin(new Insets(10, 10, 10, 10));

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

        try {
            setupConnection();
        } catch (IOException e) {
            handleError(e.getMessage());
        }

        new Thread(new Receiver()).start();
    }

    private void setupConnection() throws IOException {
        listener = new ServerSocket(9999);
        socket = listener.accept(); // Accept client connection
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        clientOutputs.add(out);

        clientNickname = in.readLine(); // Read the client's nickname
        appendMessage(clientNickname + " has joined the chat", false);

    }

    private void broadcastMessage(String message) {
        for (BufferedWriter writer : clientOutputs) {
            try {
                writer.write(message + "\n");
                writer.flush();
            } catch (IOException e) {
                handleError(e.getMessage());
            }
        }
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
                        appendMessage(msg, false);
                        broadcastMessage(msg);
                    }
                } catch (IOException e) {
                    handleError(e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sender || e.getSource() == btnSend) {
            String msg = sender.getText();
            try {
                String messageToSend = "Chat GPT: " + msg;
                appendMessage(messageToSend, true);
                broadcastMessage(messageToSend);
                sender.setText(""); // Clear the input field
            } catch (Exception e1) {
                handleError(e1.getMessage());
            }
        }
    }
}
