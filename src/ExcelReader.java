import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ExcelReader.java
 * Đọc file Excel input (.xlsx), tự động normalize tên sheet và tên cột
 * để nhận diện đúng sheet "Danh sách cán bộ" và "DS phòng thi"
 * bất kể tên viết hoa/thường, có dấu/không dấu.
 */
public class ExcelReader {

    // Các pattern để nhận diện sheet (sau khi normalize)
    private static final String[] CANBO_PATTERNS = {
        "danh_sach_can_bo", "danhsachcanbo", "danh_sach_cb", "ds_can_bo",
        "dscanbo", "danh_sach_giam_thi", "ds_giam_thi", "dsgv"
    };
    private static final String[] PHONGTHI_PATTERNS = {
        "ds_phong_thi", "dsphongthi", "danh_sach_phong_thi", "phong_thi",
        "phongthi", "ds_phong", "dsphong"
    };

    // Kết quả đọc file
    private List<String[]> danhSachCanBo = new ArrayList<>();  // [maGV, hoTen, ngaySinh, donVi]
    private List<String[]> dsPhongThi = new ArrayList<>();      // [maPhong, ghiChu]

    /**
     * Đọc file Excel từ đường dẫn.
     * @param filePath đường dẫn file .xlsx
     */
    public void readFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            readWorkbook(workbook);
        }
    }

    /**
     * Đọc file Excel từ InputStream (nhận từ network).
     */
    public void readFromStream(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            readWorkbook(workbook);
        }
    }

    /**
     * Đọc file Excel từ mảng byte (nhận từ network).
     */
    public void readFromBytes(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             Workbook workbook = new XSSFWorkbook(bais)) {
            readWorkbook(workbook);
        }
    }

    /**
     * Đọc tất cả sheets trong workbook.
     */
    private void readWorkbook(Workbook workbook) {
        danhSachCanBo.clear();
        dsPhongThi.clear();

        System.out.println("[ExcelReader] File có " + workbook.getNumberOfSheets() + " sheet(s):");

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            String normalizedName = normalizeVietnamese(sheetName);

            System.out.println("  Sheet " + (i + 1) + ": \"" + sheetName + "\" → normalized: \"" + normalizedName + "\"");

            if (matchesAny(normalizedName, CANBO_PATTERNS)) {
                System.out.println("    → Nhận diện: DANH SÁCH CÁN BỘ");
                readSheetCanBo(sheet);
            } else if (matchesAny(normalizedName, PHONGTHI_PATTERNS)) {
                System.out.println("    → Nhận diện: DS PHÒNG THI");
                readSheetPhongThi(sheet);
            } else {
                System.out.println("    → Bỏ qua (không nhận diện được)");
            }
        }

        System.out.println("[ExcelReader] Kết quả: " + danhSachCanBo.size() + " cán bộ, "
                + dsPhongThi.size() + " phòng thi.");
    }

    // ============ ĐỌC SHEET CÁN BỘ ============

    /**
     * Đọc sheet "Danh sách cán bộ" với các cột:
     * TT | Mã GV | Họ Tên | Ngày sinh | Đơn vị công tác
     * Tự động detect vị trí cột dựa trên tên cột (normalized).
     */
    private void readSheetCanBo(Sheet sheet) {
        // Tìm header row
        Row headerRow = null;
        int headerRowIdx = -1;
        for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row != null && isHeaderRow(row, new String[]{"ma_gv", "magv", "ma_giang_vien", "ma"})) {
                headerRow = row;
                headerRowIdx = r;
                break;
            }
        }

        if (headerRow == null) {
            // Fallback: Giả sử row 0 là header, cột theo thứ tự mặc định
            System.out.println("    [WARN] Không tìm thấy header, dùng thứ tự cột mặc định (TT, Mã GV, Họ Tên, Ngày sinh, Đơn vị)");
            headerRowIdx = 0;
        }

        // Detect vị trí các cột
        int colMaGV = -1, colHoTen = -1, colNgaySinh = -1, colDonVi = -1;

        if (headerRow != null) {
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String colName = normalizeVietnamese(getCellStringValue(cell));

                if (colMaGV == -1 && containsAny(colName, "ma_gv", "magv", "ma_giang_vien", "ma_cb", "macb")) {
                    colMaGV = c;
                } else if (colHoTen == -1 && containsAny(colName, "ho_ten", "hoten", "ho_va_ten", "hovaten", "ten")) {
                    colHoTen = c;
                } else if (colNgaySinh == -1 && containsAny(colName, "ngay_sinh", "ngaysinh", "sinh")) {
                    colNgaySinh = c;
                } else if (colDonVi == -1 && containsAny(colName, "don_vi", "donvi", "don_vi_cong_tac", "co_quan", "coquan")) {
                    colDonVi = c;
                }
            }
        }

        // Fallback nếu không detect được
        if (colMaGV == -1) colMaGV = 1;
        if (colHoTen == -1) colHoTen = 2;
        if (colNgaySinh == -1) colNgaySinh = 3;
        if (colDonVi == -1) colDonVi = 4;

        System.out.println("    Cột detect: MãGV=" + colMaGV + ", HọTên=" + colHoTen
                + ", NgàySinh=" + colNgaySinh + ", ĐơnVị=" + colDonVi);

        // Đọc dữ liệu từ dòng sau header
        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String maGV = getCellStringValue(row.getCell(colMaGV)).trim();
            if (maGV.isEmpty()) continue; // Bỏ qua dòng trống

            String hoTen = getCellStringValue(row.getCell(colHoTen)).trim();
            String ngaySinh = parseDateCell(row.getCell(colNgaySinh));
            String donVi = getCellStringValue(row.getCell(colDonVi)).trim();

            danhSachCanBo.add(new String[]{maGV, hoTen, ngaySinh, donVi});
        }
    }

    // ============ ĐỌC SHEET PHÒNG THI ============

    /**
     * Đọc sheet "DS phòng thi" với các cột:
     * STT | Phòng thi | Ghi chú
     */
    private void readSheetPhongThi(Sheet sheet) {
        Row headerRow = null;
        int headerRowIdx = -1;
        for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row != null && isHeaderRow(row, new String[]{"phong_thi", "phongthi", "phong", "ma_phong"})) {
                headerRow = row;
                headerRowIdx = r;
                break;
            }
        }

        if (headerRow == null) {
            headerRowIdx = 0;
        }

        // Detect cột
        int colPhong = -1, colGhiChu = -1;

        if (headerRow != null) {
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String colName = normalizeVietnamese(getCellStringValue(cell));

                if (colPhong == -1 && containsAny(colName, "phong_thi", "phongthi", "phong", "ma_phong", "maphong")) {
                    colPhong = c;
                } else if (colGhiChu == -1 && containsAny(colName, "ghi_chu", "ghichu", "vi_tri", "vitri", "dia_diem", "diadiem")) {
                    colGhiChu = c;
                }
            }
        }

        // Fallback
        if (colPhong == -1) colPhong = 1;
        if (colGhiChu == -1) colGhiChu = 2;

        System.out.println("    Cột detect: Phòng=" + colPhong + ", GhiChú=" + colGhiChu);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String phong = getCellStringValue(row.getCell(colPhong)).trim();
            if (phong.isEmpty()) continue;

            // Chuẩn hóa mã phòng (ví dụ: 1 → "001", 12 → "012")
            try {
                int phongNum = (int) Double.parseDouble(phong);
                phong = String.format("%03d", phongNum);
            } catch (NumberFormatException e) {
                // Giữ nguyên nếu không phải số
            }

            String ghiChu = getCellStringValue(row.getCell(colGhiChu)).trim();
            dsPhongThi.add(new String[]{phong, ghiChu});
        }
    }

    // ============ HELPER METHODS ============

    /**
     * Normalize chuỗi tiếng Việt:
     * "Danh sách Cán Bộ" → "danh_sach_can_bo"
     */
    public static String normalizeVietnamese(String input) {
        if (input == null) return "";
        // Chuyển thường
        String result = input.toLowerCase().trim();
        // Bỏ dấu tiếng Việt
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(result).replaceAll("");
        // Thay đ → d
        result = result.replace("đ", "d").replace("Đ", "D");
        // Thay khoảng trắng và ký tự đặc biệt bằng _
        result = result.replaceAll("[^a-z0-9]", "_");
        // Loại bỏ _ liên tiếp
        result = result.replaceAll("_+", "_");
        // Loại bỏ _ đầu cuối
        result = result.replaceAll("^_|_$", "");
        return result;
    }

    /**
     * Kiểm tra chuỗi normalized có khớp bất kỳ pattern nào không.
     */
    private boolean matchesAny(String normalized, String[] patterns) {
        for (String p : patterns) {
            if (normalized.contains(p) || p.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra chuỗi có chứa bất kỳ keyword nào không.
     */
    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw) || text.equals(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra row có phải header hay không (dựa trên keywords).
     */
    private boolean isHeaderRow(Row row, String[] keywords) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String val = normalizeVietnamese(getCellStringValue(cell));
            for (String kw : keywords) {
                if (val.contains(kw)) return true;
            }
        }
        return false;
    }

    /**
     * Lấy giá trị cell dưới dạng String, hỗ trợ nhiều kiểu dữ liệu.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(cell.getDateCellValue());
                }
                // Kiểm tra nếu là số nguyên
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    /**
     * Parse ngày sinh từ cell Excel, hỗ trợ nhiều định dạng:
     * dd/MM/yyyy, dd-MMM-yy, yyyy-MM-dd, ...
     * Trả về định dạng chuẩn yyyy-MM-dd để lưu vào DB.
     */
    private String parseDateCell(Cell cell) {
        if (cell == null) return "";

        // Nếu Excel nhận diện là Date
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(cell.getDateCellValue());
        }

        // Nếu là String, thử parse nhiều format
        String text = getCellStringValue(cell).trim();
        if (text.isEmpty()) return "";

        String[] formats = {
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "MM/dd/yyyy", "yyyy-MM-dd", "yyyy/MM/dd",
            "dd-MMM-yy", "dd-MMM-yyyy",
            "d/M/yyyy", "d-M-yyyy"
        };

        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(text);
                SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd");
                return outFmt.format(date);
            } catch (Exception ignored) {
                // Thử format tiếp theo
            }
        }

        System.out.println("    [WARN] Không parse được ngày: \"" + text + "\"");
        return "";
    }

    // ============ GETTERS ============

    public List<String[]> getDanhSachCanBo() {
        return danhSachCanBo;
    }

    public List<String[]> getDsPhongThi() {
        return dsPhongThi;
    }
}
