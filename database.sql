-- ============================================================
-- DATABASE: GiamThiDB
-- Mô tả: Cơ sở dữ liệu phục vụ phân chia giám thị coi thi
-- ============================================================

-- Chạy riêng lệnh sau trong pgAdmin để tạo database:
-- CREATE DATABASE "GiamThiDB";

-- Sau đó kết nối vào GiamThiDB và chạy các lệnh dưới:

-- 1. Xóa bảng cũ nếu tồn tại
DROP TABLE IF EXISTS lich_su_phan_cong CASCADE;
DROP TABLE IF EXISTS ds_phong_thi CASCADE;
DROP TABLE IF EXISTS danh_sach_can_bo CASCADE;

-- 2. Bảng danh sách cán bộ (đọc từ sheet "Danh sách cán bộ")
CREATE TABLE danh_sach_can_bo (
    ma_gv VARCHAR(20) PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    ngay_sinh DATE,
    don_vi_cong_tac VARCHAR(200)
);

-- 3. Bảng danh sách phòng thi (đọc từ sheet "DS phòng thi")
CREATE TABLE ds_phong_thi (
    ma_phong VARCHAR(10) PRIMARY KEY,
    ghi_chu VARCHAR(200)
);

-- 4. Bảng lịch sử phân công (lưu kết quả để tra cứu)
CREATE TABLE lich_su_phan_cong (
    id SERIAL PRIMARY KEY,
    phien_xu_ly TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ca_thi INT NOT NULL,
    ma_phong VARCHAR(10) REFERENCES ds_phong_thi(ma_phong),
    ma_gv VARCHAR(20) REFERENCES danh_sach_can_bo(ma_gv),
    vai_tro VARCHAR(20) NOT NULL CHECK (vai_tro IN ('GT1', 'GT2', 'GIAM_SAT'))
);

-- Index tối ưu tra cứu
CREATE INDEX idx_lspc_gv ON lich_su_phan_cong(ma_gv);
CREATE INDEX idx_lspc_ca ON lich_su_phan_cong(ca_thi);
CREATE INDEX idx_lspc_phong ON lich_su_phan_cong(ma_phong);
