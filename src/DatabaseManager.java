import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager.java
 * Quản lý kết nối và thao tác CRUD với PostgreSQL database GiamThiDB.
 */
public class DatabaseManager {

    // ============ CẤU HÌNH KẾT NỐI - THAY ĐỔI NẾU CẦN ============
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/GiamThiDB";
    private static final String USER = "thu2";
    private static final String PASS = "thu2";

    private Connection connection;

    // ============ KẾT NỐI ============

    /**
     * Mở kết nối tới database.
     */
    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("[DB] Kết nối PostgreSQL thành công!");
        }
    }

    /**
     * Đóng kết nối database.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Đã đóng kết nối PostgreSQL.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    /**
     * Lấy connection hiện tại.
     */
    public Connection getConnection() {
        return connection;
    }

    // ============ DANH SÁCH CÁN BỘ ============

    /**
     * Xóa toàn bộ dữ liệu cán bộ cũ (để cập nhật từ file mới).
     */
    public void clearCanBo() throws SQLException {
        // Xóa lịch sử phân công trước (do foreign key)
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM lich_su_phan_cong");
            stmt.executeUpdate("DELETE FROM danh_sach_can_bo");
        }
        System.out.println("[DB] Đã xóa dữ liệu cán bộ cũ.");
    }

    /**
     * Thêm một cán bộ vào database (UPSERT - thêm hoặc cập nhật nếu trùng mã GV).
     */
    public void upsertCanBo(String maGV, String hoTen, Date ngaySinh, String donVi) throws SQLException {
        String sql = "INSERT INTO danh_sach_can_bo (ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (ma_gv) DO UPDATE SET " +
                "ho_ten = EXCLUDED.ho_ten, ngay_sinh = EXCLUDED.ngay_sinh, " +
                "don_vi_cong_tac = EXCLUDED.don_vi_cong_tac";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, maGV);
            pstmt.setString(2, hoTen);
            pstmt.setDate(3, ngaySinh);
            pstmt.setString(4, donVi);
            pstmt.executeUpdate();
        }
    }

    /**
     * Thêm hàng loạt cán bộ (batch insert cho hiệu suất).
     */
    public void batchInsertCanBo(List<String[]> danhSach) throws SQLException {
        // Xóa dữ liệu cũ trước
        clearCanBo();
        clearPhongThi();

        String sql = "INSERT INTO danh_sach_can_bo (ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac) " +
                "VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (ma_gv) DO UPDATE SET " +
                "ho_ten = EXCLUDED.ho_ten, ngay_sinh = EXCLUDED.ngay_sinh, " +
                "don_vi_cong_tac = EXCLUDED.don_vi_cong_tac";

        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (String[] row : danhSach) {
                // row = [maGV, hoTen, ngaySinh(yyyy-MM-dd), donVi]
                pstmt.setString(1, row[0]);
                pstmt.setString(2, row[1]);
                if (row[2] != null && !row[2].isEmpty()) {
                    pstmt.setDate(3, Date.valueOf(row[2]));
                } else {
                    pstmt.setNull(3, Types.DATE);
                }
                pstmt.setString(4, row[3]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
            System.out.println("[DB] Đã thêm " + danhSach.size() + " cán bộ vào database.");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Lấy toàn bộ danh sách cán bộ từ database.
     * Trả về List of [maGV, hoTen, ngaySinh, donVi].
     */
    public List<String[]> getAllCanBo() throws SQLException {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT ma_gv, ho_ten, ngay_sinh, don_vi_cong_tac FROM danh_sach_can_bo ORDER BY ma_gv";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String[] row = new String[4];
                row[0] = rs.getString("ma_gv");
                row[1] = rs.getString("ho_ten");
                Date d = rs.getDate("ngay_sinh");
                row[2] = (d != null) ? d.toString() : "";
                row[3] = rs.getString("don_vi_cong_tac");
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Đếm số lượng cán bộ trong database.
     */
    public int countCanBo() throws SQLException {
        String sql = "SELECT COUNT(*) FROM danh_sach_can_bo";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    // ============ PHÒNG THI ============

    /**
     * Xóa toàn bộ phòng thi cũ.
     */
    public void clearPhongThi() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM lich_su_phan_cong");
            stmt.executeUpdate("DELETE FROM ds_phong_thi");
        }
        System.out.println("[DB] Đã xóa dữ liệu phòng thi cũ.");
    }

    /**
     * Thêm hàng loạt phòng thi.
     */
    public void batchInsertPhongThi(List<String[]> danhSach) throws SQLException {
        String sql = "INSERT INTO ds_phong_thi (ma_phong, ghi_chu) VALUES (?, ?) " +
                "ON CONFLICT (ma_phong) DO UPDATE SET ghi_chu = EXCLUDED.ghi_chu";

        connection.setAutoCommit(false);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (String[] row : danhSach) {
                // row = [maPhong, ghiChu]
                pstmt.setString(1, row[0]);
                pstmt.setString(2, row[1]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            connection.commit();
            System.out.println("[DB] Đã thêm " + danhSach.size() + " phòng thi vào database.");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Lấy toàn bộ danh sách phòng thi.
     * Trả về List of [maPhong, ghiChu].
     */
    public List<String[]> getAllPhongThi() throws SQLException {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT ma_phong, ghi_chu FROM ds_phong_thi ORDER BY ma_phong";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String[] row = new String[2];
                row[0] = rs.getString("ma_phong");
                row[1] = rs.getString("ghi_chu");
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Đếm số phòng thi.
     */
    public int countPhongThi() throws SQLException {
        String sql = "SELECT COUNT(*) FROM ds_phong_thi";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }
}
