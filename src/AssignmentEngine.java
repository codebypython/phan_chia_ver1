import java.util.*;

/**
 * AssignmentEngine.java
 * Thuật toán phân công giám thị coi thi.
 * * Ràng buộc:
 * - Mỗi phòng cần đúng 2 giám thị (GT1, GT2)
 * - Giám thị thừa → cán bộ giám sát hành lang (CBGS), chia đều cho các phòng
 * theo tầng/khu vực
 * - Qua mỗi ca: GT không được coi ở phòng cũ, không được ghép cặp với người cũ
 * - CBGS không có ràng buộc, có thể được xếp làm GT ở ca sau
 * - Hỗ trợ phân cụm theo vị trí địa lý (ghi chú) và Tầng
 * * Thuật toán: Greedy + Random Shuffle + Retry
 */
public class AssignmentEngine {

    // Giới hạn số lần thử lại để tránh loop vô tận và thoát Deadlock khi ghép cặp
    private static final int MAX_RETRY = 2000;

    // ============ DATA STRUCTURES ============

    public static class RoomAssignment {
        public String maPhong;
        public String maGV_GT1;
        public String hoTen_GT1;
        public String maGV_GT2;
        public String hoTen_GT2;

        public RoomAssignment(String maPhong, String maGV_GT1, String hoTen_GT1,
                String maGV_GT2, String hoTen_GT2) {
            this.maPhong = maPhong;
            this.maGV_GT1 = maGV_GT1;
            this.hoTen_GT1 = hoTen_GT1;
            this.maGV_GT2 = maGV_GT2;
            this.hoTen_GT2 = hoTen_GT2;
        }
    }

    public static class SupervisorAssignment {
        public String maGV;
        public String hoTen;
        public List<String> danhSachPhong; // Các phòng được giám sát

        public SupervisorAssignment(String maGV, String hoTen, List<String> danhSachPhong) {
            this.maGV = maGV;
            this.hoTen = hoTen;
            this.danhSachPhong = danhSachPhong;
        }
    }

    public static class SessionResult {
        public int caThi;
        public List<RoomAssignment> roomAssignments;
        public List<SupervisorAssignment> supervisorAssignments;

        public SessionResult(int caThi) {
            this.caThi = caThi;
            this.roomAssignments = new ArrayList<>();
            this.supervisorAssignments = new ArrayList<>();
        }
    }

    // ============ FIELDS ============

    /** Danh sách tất cả GV: maGV → hoTen */
    private Map<String, String> gvMap = new LinkedHashMap<>();

    /** Danh sách mã GV */
    private List<String> allMaGV = new ArrayList<>();

    /** Danh sách phòng thi: [maPhong, ghiChu] */
    private List<String[]> phongThiList = new ArrayList<>();

    private int soPhongChia;
    private int soNhanVienChia;

    // --- Lịch sử ràng buộc ---
    private Map<String, Set<String>> lichSuPhong = new HashMap<>();
    private Map<String, Set<String>> lichSuCap = new HashMap<>();

    /** maGV → Vị trí địa lý (cố định sau khi được phân công lần đầu) */
    private Map<String, String> gvLocation = new HashMap<>();

    /** maPhong → Vị trí địa lý */
    private Map<String, String> roomLocationMap = new HashMap<>();

    private Random random = new Random();

    // ============ KHỞI TẠO ============

    public void setup(List<String[]> danhSachGV, List<String[]> danhSachPhong,
            int soPhong, int soNhanVien) {
        // Clear state
        gvMap.clear();
        allMaGV.clear();
        phongThiList.clear();
        lichSuPhong.clear();
        lichSuCap.clear();
        gvLocation.clear();
        roomLocationMap.clear();

        // Load GV
        for (String[] gv : danhSachGV) {
            gvMap.put(gv[0], gv[1]);
            allMaGV.add(gv[0]);
        }

        // Load phòng thi
        phongThiList.addAll(danhSachPhong);
        for (String[] pt : danhSachPhong) {
            String loc = pt[1] != null ? pt[1].trim() : "";
            roomLocationMap.put(pt[0], loc);
        }

        this.soPhongChia = soPhong;
        this.soNhanVienChia = soNhanVien;

        // Khởi tạo lịch sử cho mỗi GV
        for (String maGV : allMaGV) {
            lichSuPhong.put(maGV, new HashSet<>());
            lichSuCap.put(maGV, new HashSet<>());
        }

        System.out.println("[Engine] Setup: " + allMaGV.size() + " GV, "
                + phongThiList.size() + " phòng thi");
        System.out.println("[Engine] Sẽ chia: " + soPhongChia + " phòng, "
                + soNhanVienChia + " nhân viên");
    }

    // ============ TÍNH TOÁN ============

    public List<SessionResult> calculateMaxSessions() {
        List<SessionResult> allResults = new ArrayList<>();
        int ca = 1;

        System.out.println("[Engine] Bắt đầu tính max số ca...");

        while (true) {
            SessionResult result = assignOneSession(ca);
            if (result == null) {
                System.out.println("[Engine] Không thể xếp thêm ca " + ca + ". Tổng max = " + (ca - 1));
                break;
            }
            allResults.add(result);
            System.out.println("[Engine] ✓ Ca " + ca + " xếp thành công.");
            ca++;

            if (ca > 1000) {
                System.out.println("[Engine] Đạt giới hạn 1000 ca, dừng.");
                break;
            }
        }
        return allResults;
    }

    public List<SessionResult> assignSessions(int soCaThi) {
        List<SessionResult> allResults = new ArrayList<>();

        System.out.println("[Engine] Bắt đầu phân công " + soCaThi + " ca thi...");

        for (int ca = 1; ca <= soCaThi; ca++) {
            SessionResult result = assignOneSession(ca);
            if (result == null) {
                System.out.println("[Engine] THẤT BẠI ở ca " + ca
                        + ". Chỉ xếp được " + (ca - 1) + "/" + soCaThi + " ca.");
                return allResults;
            }
            allResults.add(result);
            System.out.println("[Engine] ✓ Ca " + ca + "/" + soCaThi + " xếp thành công.");
        }
        return allResults;
    }

    // ============ PHÂN CÔNG 1 CA THI ============

    private SessionResult assignOneSession(int caThi) {
        int soGTCanThiet = soPhongChia * 2;

        if (soNhanVienChia < soGTCanThiet) {
            System.out.println("[Engine] LỖI: Số nhân viên (" + soNhanVienChia
                    + ") < 2 x số phòng (" + soGTCanThiet + ")");
            return null;
        }

        List<String> participatingGV = new ArrayList<>(allMaGV.subList(0, Math.min(soNhanVienChia, allMaGV.size())));
        List<String> participatingRooms = new ArrayList<>();
        for (int i = 0; i < Math.min(soPhongChia, phongThiList.size()); i++) {
            participatingRooms.add(phongThiList.get(i)[0]);
        }

        Map<String, List<String>> roomsByLoc = new LinkedHashMap<>();
        for (String r : participatingRooms) {
            String loc = roomLocationMap.getOrDefault(r, "");
            roomsByLoc.computeIfAbsent(loc, k -> new ArrayList<>()).add(r);
        }

        int totalCBGS = participatingGV.size() - soGTCanThiet;
        Map<String, Integer> cbgsQuota = computeCbgsQuota(roomsByLoc, totalCBGS);

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            Map<String, String> currentGvLocation = new HashMap<>(gvLocation);
            List<String> gtPool = new ArrayList<>();
            List<String> cbgsPool = new ArrayList<>();

            Map<String, List<String>> gvByLoc = new HashMap<>();
            List<String> unassignedGV = new ArrayList<>();

            for (String gv : participatingGV) {
                String loc = currentGvLocation.get(gv);
                if (loc == null) {
                    unassignedGV.add(gv);
                } else {
                    gvByLoc.computeIfAbsent(loc, k -> new ArrayList<>()).add(gv);
                }
            }
            Collections.shuffle(unassignedGV, random);

            boolean sufficient = true;
            for (String loc : roomsByLoc.keySet()) {
                int gtNeeded = roomsByLoc.get(loc).size() * 2;
                int cbNeeded = cbgsQuota.getOrDefault(loc, 0);
                int totalNeeded = gtNeeded + cbNeeded;

                List<String> locGV = gvByLoc.getOrDefault(loc, new ArrayList<>());

                while (locGV.size() < totalNeeded && !unassignedGV.isEmpty()) {
                    locGV.add(unassignedGV.remove(unassignedGV.size() - 1));
                }

                if (locGV.size() < totalNeeded) {
                    sufficient = false;
                    break;
                }

                Collections.shuffle(locGV, random);
                gtPool.addAll(locGV.subList(0, gtNeeded));
                cbgsPool.addAll(locGV.subList(gtNeeded, locGV.size()));
            }

            if (!sufficient)
                continue;

            cbgsPool.addAll(unassignedGV);

            // Sắp xếp GT pool ưu tiên người có nhiều ràng buộc lịch sử nhất
            gtPool.sort((a, b) -> {
                int constraintA = lichSuPhong.getOrDefault(a, Collections.emptySet()).size()
                        + lichSuCap.getOrDefault(a, Collections.emptySet()).size();
                int constraintB = lichSuPhong.getOrDefault(b, Collections.emptySet()).size()
                        + lichSuCap.getOrDefault(b, Collections.emptySet()).size();
                return Integer.compare(constraintB, constraintA); // Giảm dần
            });

            List<String> shuffledRooms = new ArrayList<>(participatingRooms);
            Collections.shuffle(shuffledRooms, random);

            SessionResult result = tryGreedyAssignment(caThi, shuffledRooms, gtPool, cbgsPool, currentGvLocation);

            if (result != null) {
                updateHistory(result);
                gvLocation = currentGvLocation;

                // VẤN ĐỀ 1: Sắp xếp kết quả phòng thi theo trật tự số để xuất Excel đẹp
                result.roomAssignments.sort((a, b) -> Integer.compare(
                        extractNumber(a.maPhong), extractNumber(b.maPhong)));

                // Sắp xếp danh sách CBGS dựa theo phòng đầu tiên họ giám sát
                result.supervisorAssignments.sort((a, b) -> {
                    if (a.danhSachPhong.isEmpty() || b.danhSachPhong.isEmpty())
                        return 0;
                    return Integer.compare(
                            extractNumber(a.danhSachPhong.get(0)),
                            extractNumber(b.danhSachPhong.get(0)));
                });

                return result;
            }
        }

        return null;
    }

    private Map<String, Integer> computeCbgsQuota(Map<String, List<String>> roomsByLoc, int totalCBGS) {
        Map<String, Integer> cbgsQuota = new HashMap<>();
        int remainingCBGS = totalCBGS;

        for (String loc : roomsByLoc.keySet()) {
            if (remainingCBGS > 0) {
                cbgsQuota.put(loc, 1);
                remainingCBGS--;
            } else {
                cbgsQuota.put(loc, 0);
            }
        }

        while (remainingCBGS > 0) {
            String maxLoc = null;
            double maxRatio = -1;
            for (Map.Entry<String, List<String>> entry : roomsByLoc.entrySet()) {
                String loc = entry.getKey();
                int roomsInLoc = entry.getValue().size();
                int currentQuota = cbgsQuota.get(loc);
                double ratio = (double) roomsInLoc / (currentQuota + 1);
                if (ratio > maxRatio) {
                    maxRatio = ratio;
                    maxLoc = loc;
                }
            }
            if (maxLoc != null) {
                cbgsQuota.put(maxLoc, cbgsQuota.get(maxLoc) + 1);
                remainingCBGS--;
            } else {
                break;
            }
        }
        return cbgsQuota;
    }

    private SessionResult tryGreedyAssignment(int caThi, List<String> rooms,
            List<String> gtPool, List<String> cbgsPool, Map<String, String> currentGvLocation) {
        SessionResult result = new SessionResult(caThi);
        Set<String> usedGT = new HashSet<>();

        for (String room : rooms) {
            String roomLoc = roomLocationMap.getOrDefault(room, "");
            String gt1 = null, gt2 = null;

            // Tìm GT1
            for (String gv : gtPool) {
                if (usedGT.contains(gv))
                    continue;
                String gvLoc = currentGvLocation.get(gv);
                if (gvLoc != null && !gvLoc.equals(roomLoc))
                    continue;

                if (!lichSuPhong.getOrDefault(gv, Collections.emptySet()).contains(room)) {
                    gt1 = gv;
                    break;
                }
            }

            if (gt1 == null)
                return null; // Không tìm được GT1 → thất bại vòng này

            usedGT.add(gt1);
            currentGvLocation.put(gt1, roomLoc);

            // Tìm GT2
            for (String gv : gtPool) {
                if (usedGT.contains(gv))
                    continue;
                String gvLoc = currentGvLocation.get(gv);
                if (gvLoc != null && !gvLoc.equals(roomLoc))
                    continue;

                if (!lichSuPhong.getOrDefault(gv, Collections.emptySet()).contains(room) &&
                        !lichSuCap.getOrDefault(gv, Collections.emptySet()).contains(gt1)) {
                    gt2 = gv;
                    break;
                }
            }

            if (gt2 == null)
                return null; // Không tìm được GT2 → thất bại vòng này

            usedGT.add(gt2);
            currentGvLocation.put(gt2, roomLoc);

            result.roomAssignments.add(new RoomAssignment(
                    room,
                    gt1, gvMap.get(gt1),
                    gt2, gvMap.get(gt2)));
        }

        // VẤN ĐỀ 2: Nhóm phòng theo địa điểm và theo tầng để chia CBGS
        Map<String, Map<String, List<String>>> roomsByLocAndFloor = new LinkedHashMap<>();
        for (String r : rooms) {
            String loc = roomLocationMap.getOrDefault(r, "");
            String tang = getTang(r);
            roomsByLocAndFloor.computeIfAbsent(loc, k -> new LinkedHashMap<>())
                    .computeIfAbsent(tang, k -> new ArrayList<>()).add(r);
        }

        boolean cbgsSuccess = assignSupervisors(result, roomsByLocAndFloor, cbgsPool, currentGvLocation);
        if (!cbgsSuccess)
            return null;

        return result;
    }

    private boolean assignSupervisors(SessionResult result, Map<String, Map<String, List<String>>> roomsByLocAndFloor,
            List<String> cbgsPool, Map<String, String> currentGvLocation) {
        if (cbgsPool.isEmpty())
            return true;

        Set<String> usedCBGS = new HashSet<>();

        for (String loc : roomsByLocAndFloor.keySet()) {
            Map<String, List<String>> floorMap = roomsByLocAndFloor.get(loc);

            for (String tang : floorMap.keySet()) {
                List<String> locRooms = floorMap.get(tang);
                // Cứ 10 phòng trong 1 tầng thì điều 1 CBGS (có thể điều chỉnh số lượng này)
                int quota = Math.max(1, locRooms.size() / 10);

                List<String> selectedCBGS = new ArrayList<>();
                for (String gv : cbgsPool) {
                    if (selectedCBGS.size() >= quota)
                        break;
                    if (usedCBGS.contains(gv))
                        continue;

                    String gvLoc = currentGvLocation.get(gv);
                    if (gvLoc == null || gvLoc.equals(loc)) {
                        selectedCBGS.add(gv);
                        usedCBGS.add(gv);
                        currentGvLocation.put(gv, loc);
                    }
                }

                if (selectedCBGS.isEmpty())
                    continue; // Bỏ qua tầng này nếu không còn CBGS

                int roomsPerCBGS = locRooms.size() / selectedCBGS.size();
                int extra = locRooms.size() % selectedCBGS.size();
                int roomIndex = 0;

                for (int i = 0; i < selectedCBGS.size(); i++) {
                    String gv = selectedCBGS.get(i);
                    int numRooms = roomsPerCBGS + (i < extra ? 1 : 0);
                    List<String> assignedRooms = new ArrayList<>();
                    for (int j = 0; j < numRooms && roomIndex < locRooms.size(); j++) {
                        assignedRooms.add(locRooms.get(roomIndex));
                        roomIndex++;
                    }
                    if (!assignedRooms.isEmpty()) {
                        result.supervisorAssignments.add(new SupervisorAssignment(gv, gvMap.get(gv), assignedRooms));
                    }
                }
            }
        }
        return true;
    }

    private void updateHistory(SessionResult result) {
        for (RoomAssignment ra : result.roomAssignments) {
            lichSuPhong.computeIfAbsent(ra.maGV_GT1, k -> new HashSet<>()).add(ra.maPhong);
            lichSuPhong.computeIfAbsent(ra.maGV_GT2, k -> new HashSet<>()).add(ra.maPhong);
            lichSuCap.computeIfAbsent(ra.maGV_GT1, k -> new HashSet<>()).add(ra.maGV_GT2);
            lichSuCap.computeIfAbsent(ra.maGV_GT2, k -> new HashSet<>()).add(ra.maGV_GT1);
        }
    }

    // ============ HÀM TIỆN ÍCH (HELPER) ============

    /**
     * Trích xuất tầng từ mã phòng. Ví dụ: "105" -> "1", "205" -> "2", "A105" ->
     * "A1".
     * Giả định: 2 ký tự cuối cùng đại diện cho số phòng, phần còn lại phía trước là
     * số tầng.
     */
    private String getTang(String maPhong) {
        if (maPhong == null || maPhong.length() <= 2)
            return "0";
        return maPhong.substring(0, maPhong.length() - 2);
    }

    /**
     * Chuyển mã phòng thành số nguyên để sắp xếp (loại bỏ ký tự chữ cái nếu có).
     */
    private int extractNumber(String maPhong) {
        try {
            return Integer.parseInt(maPhong.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0; // Trả về 0 nếu không parse được
        }
    }

    // ============ THỐNG KÊ ============

    public void printStats(List<SessionResult> results) {
        System.out.println("\n========== THỐNG KÊ PHÂN CÔNG ==========");
        System.out.println("Tổng số ca thi: " + results.size());
        System.out.println("Số phòng thi: " + soPhongChia);
        System.out.println("Số nhân viên: " + soNhanVienChia);
        System.out.println("GT/ca: " + (soPhongChia * 2) + " | CBGS/ca: " + (soNhanVienChia - soPhongChia * 2));

        for (SessionResult sr : results) {
            System.out.println("\n--- Ca " + sr.caThi + " ---");
            System.out.println("  GT phân công: " + sr.roomAssignments.size() + " phòng");
            System.out.println("  CBGS: " + sr.supervisorAssignments.size() + " người");
        }
        System.out.println("==========================================\n");
    }
}