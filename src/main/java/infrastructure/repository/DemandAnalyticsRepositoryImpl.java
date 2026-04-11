package infrastructure.repository;

import domain.dto.DemandHistoryPointDTO;
import domain.repository.IDemandAnalyticsRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DemandAnalyticsRepositoryImpl implements IDemandAnalyticsRepository {

    @Override
    public List<DemandHistoryPointDTO> getDailyDemandHistory(int maSP, int maCN, LocalDate fromDate, LocalDate toDate) {
        String sql = "SELECT dt.FullDate, f.SoLuongBan " +
                "FROM FactSalesDaily f " +
                "JOIN DimTime dt ON f.DateKey = dt.DateKey " +
                "WHERE f.MaSP = ? AND f.MaCN = ? AND dt.FullDate BETWEEN ? AND ? " +
                "ORDER BY dt.FullDate";
        List<DemandHistoryPointDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maSP);
            ps.setInt(2, maCN);
            ps.setDate(3, java.sql.Date.valueOf(fromDate));
            ps.setDate(4, java.sql.Date.valueOf(toDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DemandHistoryPointDTO(
                            rs.getDate("FullDate").toLocalDate(),
                            rs.getInt("SoLuongBan")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Loi tai demand history", e);
        }
    }

    @Override
    public boolean runDailyEtl(LocalDate fromDate, LocalDate toDate) {
        String sqlDim = "{CALL sp_PopulateDimTime(?, ?)}";
        String sqlFactSales = "{CALL sp_RefreshFactSalesDaily(?, ?)}";
        String sqlFactInventory = "{CALL sp_RefreshFactInventoryDaily(?)}";

        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareCall(sqlDim);
                 PreparedStatement ps2 = conn.prepareCall(sqlFactSales);
                 PreparedStatement ps3 = conn.prepareCall(sqlFactInventory)) {
                ps1.setDate(1, java.sql.Date.valueOf(fromDate));
                ps1.setDate(2, java.sql.Date.valueOf(toDate));
                ps1.execute();

                ps2.setDate(1, java.sql.Date.valueOf(fromDate));
                ps2.setDate(2, java.sql.Date.valueOf(toDate));
                ps2.execute();

                ps3.setDate(1, java.sql.Date.valueOf(toDate));
                ps3.execute();

                conn.commit();
                return true;
            } catch (SQLException innerEx) {
                conn.rollback();
                throw innerEx;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Loi khi chay ETL demand", e);
        }
    }
}
