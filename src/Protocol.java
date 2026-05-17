/**
 * Protocol.java
 * Định nghĩa các hằng số giao thức giữa Client và Server.
 * 
 * Giao thức: Client gửi command → Server xử lý → Server trả response
 */
public class Protocol {

    // ===================== COMMANDS (Client → Server) =====================
    
    /** Upload file Excel lên Server. Theo sau là: [long fileSize] + [byte[] fileData] */
    public static final String CMD_UPLOAD_EXCEL = "CMD_UPLOAD_EXCEL";
    
    /** Yêu cầu xử lý phân công. Theo sau là: [int soPhong] + [int soNhanVien] + [int soCaThi] 
     *  soCaThi = 0 nghĩa là tự động tính max */
    public static final String CMD_PROCESS = "CMD_PROCESS";
    
    /** Yêu cầu tải file kết quả. Theo sau là: [String fileName] */
    public static final String CMD_DOWNLOAD_RESULT = "CMD_DOWNLOAD_RESULT";
    
    /** Client ngắt kết nối */
    public static final String CMD_EXIT = "CMD_EXIT";

    // ===================== RESPONSES (Server → Client) =====================
    
    /** Xử lý thành công. Theo sau là: [String message] */
    public static final String RESP_OK = "RESP_OK";
    
    /** Xử lý thất bại. Theo sau là: [String errorMessage] */
    public static final String RESP_ERROR = "RESP_ERROR";
    
    /** Gửi file. Theo sau là: [String fileName] + [long fileSize] + [byte[] fileData] */
    public static final String RESP_FILE = "RESP_FILE";
    
    /** Thông báo tiến trình. Theo sau là: [String progressMessage] */
    public static final String RESP_PROGRESS = "RESP_PROGRESS";

    // ===================== SERVER CONFIG =====================
    
    /** Cổng mặc định của Server */
    public static final int SERVER_PORT = 9999;
    
    /** Kích thước buffer đọc file (64KB) */
    public static final int BUFFER_SIZE = 65536;
    
    /** Số lần retry tối đa cho thuật toán phân công mỗi ca */
    public static final int MAX_RETRY = 1000;
}
