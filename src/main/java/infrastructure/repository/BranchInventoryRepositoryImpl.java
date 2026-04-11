package infrastructure.repository;

import domain.dto.BranchInventoryDTO;
import domain.repository.IBranchInventoryRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranchInventoryRepositoryImpl implements IBranchInventoryRepository {

    @Override
    public List<BranchInventoryDTO> findByBranch(int maCN) {
        String sql = "SELECT tk.MaCN, tk.MaSP, tk.TonHienTai, tk.MucTonToiThieu, tk.MucTonMucTieu, tk.SoNgayLeadTime, tk.SoNgayTonAnToan, " +
                "sp.TenSP, cn.TenChiNhanh " +
                "FROM TonKhoChiNhanh tk " +
                "JOIN SanPham sp ON tk.MaSP = sp.MaSP " +
                "JOIN ChiNhanh cn ON tk.MaCN = cn.MaCN " +
                "WHERE tk.MaCN = ? " +
                "ORDER BY sp.TenSP";

        List<BranchInventoryDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maCN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BranchInventoryDTO dto = new BranchInventoryDTO();
                    dto.setMaCN(rs.getInt("MaCN"));
                    dto.setMaSP(rs.getInt("MaSP"));
                    dto.setTonHienTai(rs.getInt("TonHienTai"));
                    dto.setMucTonToiThieu(rs.getInt("MucTonToiThieu"));
                    dto.setMucTonMucTieu(rs.getInt("MucTonMucTieu"));
                    dto.setSoNgayLeadTime(rs.getInt("SoNgayLeadTime"));
                    dto.setSoNgayTonAnToan(rs.getInt("SoNgayTonAnToan"));
                    dto.setTenSanPham(rs.getNString("TenSP"));
                    dto.setTenChiNhanh(rs.getNString("TenChiNhanh"));
                    list.add(dto);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Loi doc ton kho chi nhanh", e);
        }
    }

    @Override
    public boolean upsert(BranchInventoryDTO dto) {
        String sql = "MERGE TonKhoChiNhanh AS target " +
                "USING (SELECT ? AS MaCN, ? AS MaSP) AS src " +
                "ON target.MaCN = src.MaCN AND target.MaSP = src.MaSP " +
                "WHEN MATCHED THEN UPDATE SET TonHienTai = ?, MucTonToiThieu = ?, MucTonMucTieu = ?, SoNgayLeadTime = ?, SoNgayTonAnToan = ?, CapNhatLuc = GETDATE() " +
                "WHEN NOT MATCHED THEN INSERT (MaCN, MaSP, TonHienTai, MucTonToiThieu, MucTonMucTieu, SoNgayLeadTime, SoNgayTonAnToan) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dto.getMaCN());
            ps.setInt(2, dto.getMaSP());
            ps.setInt(3, dto.getTonHienTai());
            ps.setInt(4, dto.getMucTonToiThieu());
            ps.setInt(5, dto.getMucTonMucTieu());
            ps.setInt(6, dto.getSoNgayLeadTime());
            ps.setInt(7, dto.getSoNgayTonAnToan());
            ps.setInt(8, dto.getMaCN());
            ps.setInt(9, dto.getMaSP());
            ps.setInt(10, dto.getTonHienTai());
            ps.setInt(11, dto.getMucTonToiThieu());
            ps.setInt(12, dto.getMucTonMucTieu());
            ps.setInt(13, dto.getSoNgayLeadTime());
            ps.setInt(14, dto.getSoNgayTonAnToan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Loi upsert ton kho chi nhanh", e);
        }
    }
}
