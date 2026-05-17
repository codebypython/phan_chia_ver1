import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * Client.java
 * Giao diện Swing cho Client kết nối tới Server phân công giám thị.
 * 
 * Chức năng:
 * - Nhập IP Server, số phòng thi, số nhân viên, số ca thi
 * - Upload file Excel
 * - Xử lý phân công và nhận kết quả (2 file XLSX)
 */
public class Client extends JFrame {

    // ============ UI COMPONENTS ============
    private JTextField txtServerIP;
    private JTextField txtServerPort;
    private JTextField txtSoPhong;
    private JTextField txtSoNhanVien;
    private JTextField txtSoCaThi;
    private JCheckBox chkAutoMaxCa;
    private JTextField txtFilePath;
    private JButton btnChooseFile;
    private JButton btnUpload;
    private JButton btnProcess;
    private JButton btnConnect;
    private JTextArea txtLog;
    private JProgressBar progressBar;
    private JLabel lblStatus;

    // ============ NETWORK ============
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean connected = false;
    private File selectedFile;

    // ============ CONSTRUCTOR ============

    public Client() {
        super("Hệ Thống Phân Công Giám Thị Coi Thi - Client");
        initUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 700);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private void initUI() {
        // Main panel với padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 245, 250));

        // ============ TOP: Kết nối Server ============
        JPanel panelConnect = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelConnect.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 1),
            " Kết nối Server ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), new Color(70, 130, 180)));
        panelConnect.setBackground(Color.WHITE);

        panelConnect.add(new JLabel("IP Server:"));
        txtServerIP = new JTextField("localhost", 12);
        panelConnect.add(txtServerIP);

        panelConnect.add(new JLabel("Port:"));
        txtServerPort = new JTextField(String.valueOf(Protocol.SERVER_PORT), 5);
        panelConnect.add(txtServerPort);

        btnConnect = new JButton("Kết nối");
        btnConnect.setBackground(new Color(70, 130, 180));
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setFocusPainted(false);
        btnConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panelConnect.add(btnConnect);

        lblStatus = new JLabel("⚪ Chưa kết nối");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 12));
        panelConnect.add(lblStatus);

        // ============ CENTER: Nhập liệu ============
        JPanel panelInput = new JPanel(new GridBagLayout());
        panelInput.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 179, 113), 1),
            " Thông tin đầu vào ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), new Color(60, 179, 113)));
        panelInput.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: File Excel
        gbc.gridx = 0; gbc.gridy = 0;
        panelInput.add(new JLabel("File Excel:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtFilePath = new JTextField(25);
        txtFilePath.setEditable(false);
        panelInput.add(txtFilePath, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        btnChooseFile = new JButton("Chọn file...");
        btnChooseFile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panelInput.add(btnChooseFile, gbc);
        gbc.gridx = 3;
        btnUpload = new JButton("📤 Tải lên");
        btnUpload.setBackground(new Color(255, 165, 0));
        btnUpload.setForeground(Color.WHITE);
        btnUpload.setFocusPainted(false);
        btnUpload.setEnabled(false);
        btnUpload.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panelInput.add(btnUpload, gbc);

        // Row 1: Số phòng thi
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panelInput.add(new JLabel("Số phòng thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtSoPhong = new JTextField("1000", 10);
        panelInput.add(txtSoPhong, gbc);

        // Row 2: Số nhân viên
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panelInput.add(new JLabel("Số nhân viên:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtSoNhanVien = new JTextField("2500", 10);
        panelInput.add(txtSoNhanVien, gbc);

        // Row 3: Số ca thi
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panelInput.add(new JLabel("Số ca thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel caPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        caPanel.setBackground(Color.WHITE);
        txtSoCaThi = new JTextField("3", 5);
        caPanel.add(txtSoCaThi);
        chkAutoMaxCa = new JCheckBox("Tự động tính max");
        chkAutoMaxCa.setBackground(Color.WHITE);
        caPanel.add(chkAutoMaxCa);
        panelInput.add(caPanel, gbc);

        // Row 4: Nút xử lý
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 10, 5, 10);
        btnProcess = new JButton("⚙ XỬ LÝ PHÂN CÔNG & XUẤT KẾT QUẢ");
        btnProcess.setFont(new Font("Arial", Font.BOLD, 14));
        btnProcess.setBackground(new Color(34, 139, 34));
        btnProcess.setForeground(Color.WHITE);
        btnProcess.setFocusPainted(false);
        btnProcess.setEnabled(false);
        btnProcess.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnProcess.setPreferredSize(new Dimension(0, 45));
        panelInput.add(btnProcess, gbc);

        // Progress bar
        gbc.gridy = 5;
        gbc.insets = new Insets(5, 10, 5, 10);
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Sẵn sàng");
        panelInput.add(progressBar, gbc);

        // ============ BOTTOM: Log ============
        JPanel panelLog = new JPanel(new BorderLayout());
        panelLog.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(128, 128, 128), 1),
            " Nhật ký hoạt động ",
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12), Color.GRAY));
        panelLog.setBackground(Color.WHITE);

        txtLog = new JTextArea(12, 50);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(0, 255, 100));
        txtLog.setCaretColor(Color.GREEN);
        JScrollPane scrollLog = new JScrollPane(txtLog);
        panelLog.add(scrollLog, BorderLayout.CENTER);

        // ============ LAYOUT ============
        mainPanel.add(panelConnect, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(new Color(245, 245, 250));
        centerPanel.add(panelInput, BorderLayout.NORTH);
        centerPanel.add(panelLog, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // ============ EVENT HANDLERS ============
        setupEventHandlers();

        // Initial log
        log("Hệ thống Phân công Giám thị Coi thi - Client khởi động.");
        log("Bước 1: Nhập IP Server và nhấn 'Kết nối'");
        log("Bước 2: Chọn và tải lên file Excel danh sách");
        log("Bước 3: Nhập thông số và nhấn 'Xử lý phân công'");
    }

    // ============ EVENT HANDLERS ============

    private void setupEventHandlers() {
        // Kết nối Server
        btnConnect.addActionListener(e -> {
            if (!connected) {
                connectToServer();
            } else {
                disconnectFromServer();
            }
        });

        // Chọn file
        btnChooseFile.addActionListener(e -> chooseFile());

        // Upload file
        btnUpload.addActionListener(e -> uploadFile());

        // Xử lý phân công
        btnProcess.addActionListener(e -> processAssignment());

        // Checkbox auto max
        chkAutoMaxCa.addActionListener(e -> {
            txtSoCaThi.setEnabled(!chkAutoMaxCa.isSelected());
            if (chkAutoMaxCa.isSelected()) {
                txtSoCaThi.setText("0");
                txtSoCaThi.setBackground(Color.LIGHT_GRAY);
            } else {
                txtSoCaThi.setText("3");
                txtSoCaThi.setBackground(Color.WHITE);
            }
        });

        // Đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectFromServer();
            }
        });
    }

    // ============ KẾT NỐI ============

    private void connectToServer() {
        String ip = txtServerIP.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtServerPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                log("Đang kết nối tới " + ip + ":" + port + "...");
                socket = new Socket(ip, port);
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                connected = true;

                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("🟢 Đã kết nối");
                    lblStatus.setForeground(new Color(34, 139, 34));
                    btnConnect.setText("Ngắt kết nối");
                    btnConnect.setBackground(new Color(220, 20, 60));
                    btnUpload.setEnabled(true);
                    btnProcess.setEnabled(true);
                    txtServerIP.setEnabled(false);
                    txtServerPort.setEnabled(false);
                });

                log("✓ Kết nối thành công tới " + ip + ":" + port);

            } catch (IOException e) {
                log("✗ KHÔNG THỂ KẾT NỐI: " + e.getMessage());
                JOptionPane.showMessageDialog(Client.this,
                    "Không thể kết nối tới Server!\n" + e.getMessage(),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void disconnectFromServer() {
        try {
            if (out != null && connected) {
                out.writeUTF(Protocol.CMD_EXIT);
                out.flush();
            }
        } catch (IOException ignored) {}

        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}

        connected = false;
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("⚪ Chưa kết nối");
            lblStatus.setForeground(Color.DARK_GRAY);
            btnConnect.setText("Kết nối");
            btnConnect.setBackground(new Color(70, 130, 180));
            btnUpload.setEnabled(false);
            btnProcess.setEnabled(false);
            txtServerIP.setEnabled(true);
            txtServerPort.setEnabled(true);
        });
        log("Đã ngắt kết nối.");
    }

    // ============ CHỌN FILE ============

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));
        chooser.setDialogTitle("Chọn file Excel danh sách cán bộ");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            txtFilePath.setText(selectedFile.getAbsolutePath());
            btnUpload.setEnabled(connected);
            log("Đã chọn file: " + selectedFile.getName() + " (" + selectedFile.length() + " bytes)");
        }
    }

    // ============ UPLOAD FILE ============

    private void uploadFile() {
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Chưa chọn file!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setProcessing(true, "Đang tải file lên Server...");

        new Thread(() -> {
            try {
                byte[] fileData = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                log("Đang gửi file " + selectedFile.getName() + " (" + fileData.length + " bytes)...");

                out.writeUTF(Protocol.CMD_UPLOAD_EXCEL);
                out.writeLong(fileData.length);
                out.write(fileData);
                out.flush();

                // Nhận phản hồi
                String resp = in.readUTF();
                String msg = in.readUTF();

                if (Protocol.RESP_OK.equals(resp)) {
                    log("✓ " + msg);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(Client.this, msg,
                            "Tải lên thành công", JOptionPane.INFORMATION_MESSAGE);
                    });
                } else {
                    log("✗ " + msg);
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(Client.this, msg,
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                }

            } catch (IOException e) {
                log("✗ Lỗi tải file: " + e.getMessage());
                handleConnectionLost();
            } finally {
                setProcessing(false, "Sẵn sàng");
            }
        }).start();
    }

    // ============ XỬ LÝ PHÂN CÔNG ============

    private void processAssignment() {
        int soPhong, soNhanVien, soCaThi;
        try {
            soPhong = Integer.parseInt(txtSoPhong.getText().trim());
            soNhanVien = Integer.parseInt(txtSoNhanVien.getText().trim());
            soCaThi = chkAutoMaxCa.isSelected() ? 0 : Integer.parseInt(txtSoCaThi.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (soPhong <= 0 || soNhanVien <= 0) {
            JOptionPane.showMessageDialog(this, "Số phòng và số nhân viên phải > 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (soNhanVien < soPhong * 2) {
            JOptionPane.showMessageDialog(this,
                "Số nhân viên (" + soNhanVien + ") phải >= 2 x số phòng (" + (soPhong * 2) + ")!",
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setProcessing(true, "Đang xử lý phân công...");

        new Thread(() -> {
            try {
                log("Gửi yêu cầu phân công: " + soPhong + " phòng, " + soNhanVien + " nhân viên, "
                        + (soCaThi == 0 ? "auto max ca" : soCaThi + " ca"));

                out.writeUTF(Protocol.CMD_PROCESS);
                out.writeInt(soPhong);
                out.writeInt(soNhanVien);
                out.writeInt(soCaThi);
                out.flush();

                // Nhận phản hồi (có thể nhiều message: PROGRESS, FILE, OK/ERROR)
                boolean done = false;
                int filesReceived = 0;

                // Chọn thư mục lưu kết quả
                String saveDir = selectedFile != null
                        ? selectedFile.getParent()
                        : System.getProperty("user.dir");

                while (!done) {
                    String resp = in.readUTF();

                    switch (resp) {
                        case Protocol.RESP_PROGRESS:
                            String progress = in.readUTF();
                            log("⏳ " + progress);
                            SwingUtilities.invokeLater(() -> progressBar.setString(progress));
                            break;

                        case Protocol.RESP_FILE:
                            String fileName = in.readUTF();
                            long fileSize = in.readLong();
                            byte[] data = new byte[(int) fileSize];
                            int totalRead = 0;
                            while (totalRead < fileSize) {
                                int read = in.read(data, totalRead, (int)(fileSize - totalRead));
                                if (read == -1) throw new IOException("Mất kết nối khi nhận file");
                                totalRead += read;
                            }

                            // Lưu file
                            File outFile = new File(saveDir, fileName);
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                fos.write(data);
                            }
                            log("✓ Đã nhận và lưu: " + outFile.getAbsolutePath() + " (" + fileSize + " bytes)");
                            filesReceived++;
                            break;

                        case Protocol.RESP_OK:
                            String msg = in.readUTF();
                            log("✓ " + msg);
                            done = true;

                            final int fCount = filesReceived;
                            final String fDir = saveDir;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(Client.this,
                                    msg + "\n\nĐã lưu " + fCount + " file vào:\n" + fDir,
                                    "Phân công thành công!", JOptionPane.INFORMATION_MESSAGE);
                            });
                            break;

                        case Protocol.RESP_ERROR:
                            String err = in.readUTF();
                            log("✗ LỖI: " + err);
                            done = true;

                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(Client.this, err,
                                    "Lỗi phân công", JOptionPane.ERROR_MESSAGE);
                            });
                            break;

                        default:
                            log("? Phản hồi không xác định: " + resp);
                            done = true;
                    }
                }

            } catch (IOException e) {
                log("✗ Lỗi xử lý: " + e.getMessage());
                handleConnectionLost();
            } finally {
                setProcessing(false, "Hoàn tất");
            }
        }).start();
    }

    // ============ HELPER METHODS ============

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void setProcessing(boolean processing, String statusText) {
        SwingUtilities.invokeLater(() -> {
            btnUpload.setEnabled(!processing && connected);
            btnProcess.setEnabled(!processing && connected);
            progressBar.setIndeterminate(processing);
            progressBar.setString(statusText);
        });
    }

    private void handleConnectionLost() {
        connected = false;
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("🔴 Mất kết nối");
            lblStatus.setForeground(Color.RED);
            btnConnect.setText("Kết nối");
            btnConnect.setBackground(new Color(70, 130, 180));
            btnUpload.setEnabled(false);
            btnProcess.setEnabled(false);
            txtServerIP.setEnabled(true);
            txtServerPort.setEnabled(true);
            JOptionPane.showMessageDialog(Client.this,
                "Mất kết nối tới Server!\nHãy kiểm tra Server và kết nối lại.",
                "Mất kết nối", JOptionPane.WARNING_MESSAGE);
        });
    }

    // ============ MAIN ============

    public static void main(String[] args) {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new Client().setVisible(true);
        });
    }
}
