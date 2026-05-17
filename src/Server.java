import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Server.java
 * TCP Server xử lý phân công giám thị coi thi.
 * 
 * Chức năng:
 * - Lắng nghe kết nối từ Client trên cổng 9999
 * - Nhận file Excel → đọc → lưu vào PostgreSQL
 * - Thực hiện thuật toán phân công
 * - Xuất file kết quả → gửi về Client
 * 
 * Multi-thread: mỗi Client được xử lý trong thread riêng.
 */
public class Server {

    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   HỆ THỐNG PHÂN CÔNG GIÁM THỊ COI THI     ║");
        System.out.println("║              SERVER v1.0                    ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        // 1. Kết nối Database
        dbManager = new DatabaseManager();
        try {
            dbManager.connect();
        } catch (SQLException e) {
            System.err.println("[SERVER] KHÔNG THỂ KẾT NỐI DATABASE!");
            System.err.println("[SERVER] Lỗi: " + e.getMessage());
            System.err.println("[SERVER] Hãy kiểm tra:");
            System.err.println("  - PostgreSQL đã bật chưa?");
            System.err.println("  - Database 'GiamThiDB' đã tạo chưa?");
            System.err.println("  - User/Password trong DatabaseManager.java đúng chưa?");
            return;
        }

        // 2. Mở cổng mạng
        try (ServerSocket serverSocket = new ServerSocket(Protocol.SERVER_PORT)) {
            // Cho phép kết nối từ bất kỳ máy nào (bind to 0.0.0.0)
            System.out.println("[SERVER] Đang lắng nghe tại port " + Protocol.SERVER_PORT);
            System.out.println("[SERVER] IP của máy này:");
            printLocalIPs();
            System.out.println("[SERVER] Client hãy kết nối tới 1 trong các IP trên, port " + Protocol.SERVER_PORT);
            System.out.println("[SERVER] Đang chờ Client kết nối...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n>>> Client mới kết nối: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("[SERVER] Lỗi mạng: " + e.getMessage());
        } finally {
            dbManager.disconnect();
        }
    }

    /**
     * In tất cả IP address của máy Server.
     */
    private static void printLocalIPs() {
        try {
            java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                java.util.Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        System.out.println("  → " + addr.getHostAddress() + " (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (SocketException e) {
            System.out.println("  → localhost (không thể lấy IP)");
        }
    }

    // =========================================================================
    // Handler cho mỗi Client
    // =========================================================================
    static class ClientHandler implements Runnable {

        private final Socket socket;
        private DataInputStream in;
        private DataOutputStream out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                while (true) {
                    String cmd = in.readUTF();
                    System.out.println("[SERVER] Nhận lệnh: " + cmd);

                    switch (cmd) {
                        case Protocol.CMD_UPLOAD_EXCEL:
                            handleUploadExcel();
                            break;

                        case Protocol.CMD_PROCESS:
                            handleProcess();
                            break;

                        case Protocol.CMD_DOWNLOAD_RESULT:
                            handleDownloadResult();
                            break;

                        case Protocol.CMD_EXIT:
                            System.out.println("[SERVER] Client ngắt kết nối.");
                            return;

                        default:
                            sendError("Lệnh không hợp lệ: " + cmd);
                    }
                }

            } catch (EOFException e) {
                System.out.println("[SERVER] Client đã ngắt kết nối (EOF).");
            } catch (IOException e) {
                System.err.println("[SERVER] Lỗi I/O: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        // ============ XỬ LÝ UPLOAD EXCEL ============

        private void handleUploadExcel() throws IOException {
            try {
                // Nhận file size
                long fileSize = in.readLong();
                System.out.println("[SERVER] Đang nhận file Excel (" + fileSize + " bytes)...");

                // Nhận file data
                byte[] fileData = new byte[(int) fileSize];
                int totalRead = 0;
                while (totalRead < fileSize) {
                    int bytesRead = in.read(fileData, totalRead, (int) (fileSize - totalRead));
                    if (bytesRead == -1) throw new IOException("Client ngắt kết nối giữa chừng");
                    totalRead += bytesRead;
                }
                System.out.println("[SERVER] Đã nhận " + totalRead + " bytes.");

                // Đọc file Excel
                ExcelReader reader = new ExcelReader();
                reader.readFromBytes(fileData);

                List<String[]> danhSachCanBo = reader.getDanhSachCanBo();
                List<String[]> dsPhongThi = reader.getDsPhongThi();

                if (danhSachCanBo.isEmpty()) {
                    sendError("Không tìm thấy dữ liệu cán bộ trong file Excel!");
                    return;
                }

                // Lưu vào database
                synchronized (dbManager) {
                    if (danhSachCanBo.size() > 0) {
                        dbManager.batchInsertCanBo(danhSachCanBo);
                    }
                    if (dsPhongThi.size() > 0) {
                        dbManager.batchInsertPhongThi(dsPhongThi);
                    }
                }

                String msg = "Đã tải lên và lưu vào database: "
                        + danhSachCanBo.size() + " cán bộ, "
                        + dsPhongThi.size() + " phòng thi.";
                sendOk(msg);

            } catch (Exception e) {
                sendError("Lỗi xử lý file Excel: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ============ XỬ LÝ PHÂN CÔNG ============

        private void handleProcess() throws IOException {
            try {
                int soPhong = in.readInt();
                int soNhanVien = in.readInt();
                int soCaThi = in.readInt(); // 0 = tự động tính max

                System.out.println("[SERVER] Yêu cầu phân công: "
                        + soPhong + " phòng, " + soNhanVien + " nhân viên, "
                        + (soCaThi == 0 ? "tự động max" : soCaThi + " ca"));

                // Đọc dữ liệu từ database
                List<String[]> danhSachGV;
                List<String[]> danhSachPhong;
                synchronized (dbManager) {
                    danhSachGV = dbManager.getAllCanBo();
                    danhSachPhong = dbManager.getAllPhongThi();
                }

                // Validate
                if (danhSachGV.isEmpty()) {
                    sendError("Chưa có dữ liệu cán bộ trong database! Hãy tải file Excel trước.");
                    return;
                }

                if (soNhanVien > danhSachGV.size()) {
                    sendError("Số nhân viên yêu cầu (" + soNhanVien
                            + ") lớn hơn số cán bộ trong database (" + danhSachGV.size() + ")!");
                    return;
                }

                if (soNhanVien < soPhong * 2) {
                    sendError("Số nhân viên (" + soNhanVien
                            + ") phải >= 2 x số phòng (" + (soPhong * 2) + ")!");
                    return;
                }

                // Nếu phòng thi trong DB ít hơn số phòng yêu cầu, tự tạo thêm
                if (danhSachPhong.size() < soPhong) {
                    System.out.println("[SERVER] DB có " + danhSachPhong.size()
                            + " phòng, cần " + soPhong + " phòng. Tự tạo thêm...");
                    for (int i = danhSachPhong.size(); i < soPhong; i++) {
                        danhSachPhong.add(new String[]{String.format("%03d", i + 1), ""});
                    }
                }

                // Chạy thuật toán phân công
                AssignmentEngine engine = new AssignmentEngine();
                engine.setup(danhSachGV, danhSachPhong, soPhong, soNhanVien);

                List<AssignmentEngine.SessionResult> results;
                if (soCaThi == 0) {
                    // Tự động tính max
                    sendProgress("Đang tính số ca thi tối đa...");
                    results = engine.calculateMaxSessions();
                } else {
                    sendProgress("Đang phân công " + soCaThi + " ca thi...");
                    results = engine.assignSessions(soCaThi);
                }

                if (results.isEmpty()) {
                    sendError("Không thể phân công bất kỳ ca thi nào! Kiểm tra lại dữ liệu.");
                    return;
                }

                engine.printStats(results);

                // Xuất file Excel kết quả
                sendProgress("Đang xuất file Excel kết quả...");
                ExcelWriter writer = new ExcelWriter();

                byte[] phanCongData = writer.writePhanCong(results);
                byte[] giamSatData = writer.writeGiamSat(results);

                // Gửi file phân công
                out.writeUTF(Protocol.RESP_FILE);
                out.writeUTF("DANHSACH_PHANCONG.XLSX");
                out.writeLong(phanCongData.length);
                out.write(phanCongData);
                out.flush();

                // Gửi file giám sát
                out.writeUTF(Protocol.RESP_FILE);
                out.writeUTF("DANHSACH_GIAMSAT.XLSX");
                out.writeLong(giamSatData.length);
                out.write(giamSatData);
                out.flush();

                // Gửi thông báo hoàn tất
                String msg = "HOÀN TẤT! Đã phân công " + results.size() + " ca thi.\n"
                        + "- Số phòng: " + soPhong + "\n"
                        + "- Số nhân viên: " + soNhanVien + "\n"
                        + "- GT mỗi ca: " + (soPhong * 2) + "\n"
                        + "- CBGS mỗi ca: " + (soNhanVien - soPhong * 2);
                sendOk(msg);

            } catch (SQLException e) {
                sendError("Lỗi database: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                sendError("Lỗi xử lý: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ============ XỬ LÝ DOWNLOAD FILE ============

        private void handleDownloadResult() throws IOException {
            String fileName = in.readUTF();
            File file = new File("output", fileName);

            if (!file.exists()) {
                sendError("File không tồn tại: " + fileName);
                return;
            }

            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            out.writeUTF(Protocol.RESP_FILE);
            out.writeUTF(fileName);
            out.writeLong(data.length);
            out.write(data);
            out.flush();
        }

        // ============ HELPER: SEND RESPONSES ============

        private void sendOk(String message) throws IOException {
            out.writeUTF(Protocol.RESP_OK);
            out.writeUTF(message);
            out.flush();
            System.out.println("[SERVER] → OK: " + message);
        }

        private void sendError(String message) throws IOException {
            out.writeUTF(Protocol.RESP_ERROR);
            out.writeUTF(message);
            out.flush();
            System.err.println("[SERVER] → ERROR: " + message);
        }

        private void sendProgress(String message) throws IOException {
            out.writeUTF(Protocol.RESP_PROGRESS);
            out.writeUTF(message);
            out.flush();
            System.out.println("[SERVER] → PROGRESS: " + message);
        }
    }
}
