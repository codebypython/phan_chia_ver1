import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.util.List;

/**
 * ExcelWriter.java
 * Xuất kết quả phân công ra file Excel (.xlsx).
 * 
 * Output:
 * 1. DANHSACH_PHANCONG.XLSX — Danh sách giám thị phân công vào phòng thi
 * 2. DANHSACH_GIAMSAT.XLSX  — Danh sách cán bộ giám sát hành lang
 * 
 * Mỗi sheet chứa tối đa 24 dòng dữ liệu.
 * Tên sheet: "Ca X-a", "Ca X-b", "Ca X-c", ...
 */
public class ExcelWriter {

    private static final int MAX_ROWS_PER_SHEET = 24;

    // ============ XUẤT FILE PHÂN CÔNG ============

    /**
     * Xuất file DANHSACH_PHANCONG.XLSX
     * 
     * Cấu trúc bảng:
     * | STT | Mã GV | Họ và tên | Giám thị 1 | Giám thị 2 | Phòng thi |
     */
    public byte[] writePhanCong(List<AssignmentEngine.SessionResult> results) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Tạo styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            for (AssignmentEngine.SessionResult session : results) {
                List<AssignmentEngine.RoomAssignment> assignments = session.roomAssignments;

                // Mỗi phòng tạo 2 dòng (GT1 + GT2) → tổng dòng = 2 * số phòng
                int totalRows = assignments.size() * 2;
                int numSheets = (int) Math.ceil((double) totalRows / MAX_ROWS_PER_SHEET);

                int dataIndex = 0; // Index trong danh sách phòng (mỗi phòng 2 dòng)
                int globalRowCount = 0; // Đếm dòng liên tục

                for (int sheetIdx = 0; sheetIdx < numSheets; sheetIdx++) {
                    String sheetName = "Ca " + session.caThi + "-" + getSheetSuffix(sheetIdx);
                    Sheet sheet = workbook.createSheet(sheetName);

                    // Tạo header
                    createPhanCongHeader(sheet, headerStyle);

                    // Set column widths
                    sheet.setColumnWidth(0, 2000);  // STT
                    sheet.setColumnWidth(1, 4000);  // Mã GV
                    sheet.setColumnWidth(2, 7000);  // Họ và tên
                    sheet.setColumnWidth(3, 3500);  // Giám thị 1
                    sheet.setColumnWidth(4, 3500);  // Giám thị 2
                    sheet.setColumnWidth(5, 3500);  // Phòng thi

                    int rowInSheet = 0;
                    int stt = sheetIdx * MAX_ROWS_PER_SHEET + 1;

                    while (rowInSheet < MAX_ROWS_PER_SHEET && globalRowCount < totalRows) {
                        // Xác định đang ở phòng nào và dòng GT1 hay GT2
                        int roomIdx = globalRowCount / 2;
                        boolean isGT1 = (globalRowCount % 2 == 0);

                        if (roomIdx >= assignments.size()) break;

                        AssignmentEngine.RoomAssignment ra = assignments.get(roomIdx);

                        Row row = sheet.createRow(rowInSheet + 2); // +2 vì 2 dòng header

                        // STT
                        Cell cellSTT = row.createCell(0);
                        cellSTT.setCellValue(stt);
                        cellSTT.setCellStyle(centerStyle);

                        if (isGT1) {
                            // Dòng GT1
                            createStringCell(row, 1, ra.maGV_GT1, dataStyle);
                            createStringCell(row, 2, ra.hoTen_GT1, dataStyle);
                            createStringCell(row, 3, "x", centerStyle);  // Giám thị 1 = x
                            createStringCell(row, 4, "", centerStyle);   // Giám thị 2 = trống
                            createStringCell(row, 5, ra.maPhong, centerStyle);
                        } else {
                            // Dòng GT2
                            createStringCell(row, 1, ra.maGV_GT2, dataStyle);
                            createStringCell(row, 2, ra.hoTen_GT2, dataStyle);
                            createStringCell(row, 3, "", centerStyle);   // Giám thị 1 = trống
                            createStringCell(row, 4, "x", centerStyle);  // Giám thị 2 = x
                            createStringCell(row, 5, ra.maPhong, centerStyle);
                        }

                        stt++;
                        rowInSheet++;
                        globalRowCount++;
                    }
                }
            }

            // Ghi ra byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Tạo header cho bảng phân công.
     * Row 0: Tiêu đề "DANH SÁCH PHÂN CÔNG GIÁM THỊ"
     * Row 1: Header cột
     */
    private void createPhanCongHeader(Sheet sheet, CellStyle headerStyle) {
        // Row 0: Tiêu đề
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH PHÂN CÔNG GIÁM THỊ COI THI");

        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setFontName("Times New Roman");
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle);

        // Merge title across columns
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Row 1: Header cột
        Row headerRow = sheet.createRow(1);
        String[] headers = {"STT", "Mã GV", "Họ và tên", "Giám thị 1", "Giám thị 2", "Phòng thi"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    // ============ XUẤT FILE GIÁM SÁT ============

    /**
     * Xuất file DANHSACH_GIAMSAT.XLSX
     * 
     * Cấu trúc bảng:
     * | STT | Mã GV | Họ và tên | Phòng thi được giám sát |
     */
    public byte[] writeGiamSat(List<AssignmentEngine.SessionResult> results) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle centerStyle = createCenterStyle(workbook);

            for (AssignmentEngine.SessionResult session : results) {
                List<AssignmentEngine.SupervisorAssignment> supervisors = session.supervisorAssignments;

                if (supervisors.isEmpty()) continue;

                int totalRows = supervisors.size();
                int numSheets = (int) Math.ceil((double) totalRows / MAX_ROWS_PER_SHEET);

                int dataIndex = 0;

                for (int sheetIdx = 0; sheetIdx < numSheets; sheetIdx++) {
                    String sheetName = "Ca " + session.caThi + "-" + getSheetSuffix(sheetIdx);
                    Sheet sheet = workbook.createSheet(sheetName);

                    // Header
                    createGiamSatHeader(sheet, headerStyle);

                    // Column widths
                    sheet.setColumnWidth(0, 2000);  // STT
                    sheet.setColumnWidth(1, 4000);  // Mã GV
                    sheet.setColumnWidth(2, 7000);  // Họ và tên
                    sheet.setColumnWidth(3, 12000); // Phòng thi được giám sát

                    int stt = sheetIdx * MAX_ROWS_PER_SHEET + 1;

                    for (int rowInSheet = 0; rowInSheet < MAX_ROWS_PER_SHEET && dataIndex < totalRows; rowInSheet++) {
                        AssignmentEngine.SupervisorAssignment sa = supervisors.get(dataIndex);

                        Row row = sheet.createRow(rowInSheet + 2); // +2 vì header

                        // STT
                        Cell cellSTT = row.createCell(0);
                        cellSTT.setCellValue(stt);
                        cellSTT.setCellStyle(centerStyle);

                        // Mã GV
                        createStringCell(row, 1, sa.maGV, dataStyle);

                        // Họ và tên
                        createStringCell(row, 2, sa.hoTen, dataStyle);

                        // Phòng thi được giám sát
                        String phongStr = String.join(", ", sa.danhSachPhong);
                        createStringCell(row, 3, phongStr, dataStyle);

                        stt++;
                        dataIndex++;
                    }
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Tạo header cho bảng giám sát.
     */
    private void createGiamSatHeader(Sheet sheet, CellStyle headerStyle) {
        // Row 0: Tiêu đề
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH CÁN BỘ GIÁM SÁT HÀNH LANG");

        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setFontName("Times New Roman");
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        // Row 1: Header cột
        Row headerRow = sheet.createRow(1);
        String[] headers = {"STT", "Mã GV", "Họ và tên", "Phòng thi được giám sát"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    // ============ XUẤT FILE RA DISK ============

    /**
     * Xuất cả 2 file kết quả ra thư mục chỉ định.
     * @param results kết quả phân công
     * @param outputDir thư mục xuất file
     */
    public void writeToFiles(List<AssignmentEngine.SessionResult> results, String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        // File phân công
        byte[] phanCongData = writePhanCong(results);
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "DANHSACH_PHANCONG.XLSX"))) {
            fos.write(phanCongData);
        }
        System.out.println("[ExcelWriter] Đã xuất: " + outputDir + "/DANHSACH_PHANCONG.XLSX");

        // File giám sát
        byte[] giamSatData = writeGiamSat(results);
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "DANHSACH_GIAMSAT.XLSX"))) {
            fos.write(giamSatData);
        }
        System.out.println("[ExcelWriter] Đã xuất: " + outputDir + "/DANHSACH_GIAMSAT.XLSX");
    }

    // ============ HELPER: STYLES ============

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setFontName("Times New Roman");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        font.setFontName("Times New Roman");
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private CellStyle createCenterStyle(XSSFWorkbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    // ============ HELPER: CELLS ============

    private void createStringCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Tạo suffix cho tên sheet: 0→"a", 1→"b", ..., 25→"z", 26→"aa", ...
     */
    private String getSheetSuffix(int index) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, (char) ('a' + (index % 26)));
            index = index / 26 - 1;
        } while (index >= 0);
        return sb.toString();
    }
}
