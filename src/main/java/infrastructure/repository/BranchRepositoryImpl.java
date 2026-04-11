package infrastructure.repository;

import domain.entity.Branch;
import domain.entity.Region;
import domain.repository.IBranchRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BranchRepositoryImpl implements IBranchRepository {

    @Override
    public List<Region> findAllRegions() {
        String sql = "SELECT MaKhuVuc, MaKhuVucCode, TenKhuVuc, TinhThanh, TrangThai, NgayTao FROM KhuVuc ORDER BY TenKhuVuc";
        List<Region> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Region r = new Region();
                r.setMaKhuVuc(rs.getInt("MaKhuVuc"));
                r.setMaKhuVucCode(rs.getNString("MaKhuVucCode"));
                r.setTenKhuVuc(rs.getNString("TenKhuVuc"));
                r.setTinhThanh(rs.getNString("TinhThanh"));
                r.setTrangThai(rs.getBoolean("TrangThai"));
                Timestamp ts = rs.getTimestamp("NgayTao");
                if (ts != null) {
                    r.setNgayTao(ts.toLocalDateTime());
                }
                list.add(r);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Loi tai region list: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Branch> findAllBranches() {
        String sql = "SELECT cn.MaCN, cn.MaCNCode, cn.TenChiNhanh, cn.DiaChi, cn.QuanHuyen, cn.MaKhuVuc, cn.TrangThai, cn.NgayTao, kv.TenKhuVuc " +
                "FROM ChiNhanh cn JOIN KhuVuc kv ON cn.MaKhuVuc = kv.MaKhuVuc ORDER BY cn.TenChiNhanh";
        List<Branch> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Branch b = mapBranch(rs);
                list.add(b);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Loi tai branch list: " + e.getMessage(), e);
        }
    }

    @Override
    public int insertRegion(Region region) {
        String sql = "INSERT INTO KhuVuc (MaKhuVucCode, TenKhuVuc, TinhThanh, TrangThai) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, region.getMaKhuVucCode());
            ps.setNString(2, region.getTenKhuVuc());
            ps.setNString(3, region.getTinhThanh());
            ps.setBoolean(4, region.isTrangThai());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong tao duoc khu vuc", e);
        }
    }

    @Override
    public int insertBranch(Branch branch) {
        String sql = "INSERT INTO ChiNhanh (MaCNCode, TenChiNhanh, DiaChi, QuanHuyen, MaKhuVuc, TrangThai) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setNString(1, branch.getMaCNCode());
            ps.setNString(2, branch.getTenChiNhanh());
            ps.setNString(3, branch.getDiaChi());
            ps.setNString(4, branch.getQuanHuyen());
            ps.setInt(5, branch.getMaKhuVuc());
            ps.setBoolean(6, branch.isTrangThai());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong tao duoc chi nhanh", e);
        }
    }

    @Override
    public boolean updateBranch(Branch branch) {
        String sql = "UPDATE ChiNhanh SET TenChiNhanh = ?, DiaChi = ?, QuanHuyen = ?, MaKhuVuc = ?, TrangThai = ? WHERE MaCN = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setNString(1, branch.getTenChiNhanh());
            ps.setNString(2, branch.getDiaChi());
            ps.setNString(3, branch.getQuanHuyen());
            ps.setInt(4, branch.getMaKhuVuc());
            ps.setBoolean(5, branch.isTrangThai());
            ps.setInt(6, branch.getMaCN());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong cap nhat duoc chi nhanh", e);
        }
    }

    @Override
    public boolean setBranchStatus(int maCN, boolean trangThai) {
        String sql = "UPDATE ChiNhanh SET TrangThai = ? WHERE MaCN = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, trangThai);
            ps.setInt(2, maCN);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Khong doi trang thai chi nhanh", e);
        }
    }

    private Branch mapBranch(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setMaCN(rs.getInt("MaCN"));
        b.setMaCNCode(rs.getNString("MaCNCode"));
        b.setTenChiNhanh(rs.getNString("TenChiNhanh"));
        b.setDiaChi(rs.getNString("DiaChi"));
        b.setQuanHuyen(rs.getNString("QuanHuyen"));
        b.setMaKhuVuc(rs.getInt("MaKhuVuc"));
        b.setTrangThai(rs.getBoolean("TrangThai"));
        b.setTenKhuVuc(rs.getNString("TenKhuVuc"));
        Timestamp ts = rs.getTimestamp("NgayTao");
        if (ts != null) {
            b.setNgayTao(ts.toLocalDateTime());
        }
        return b;
    }
}
