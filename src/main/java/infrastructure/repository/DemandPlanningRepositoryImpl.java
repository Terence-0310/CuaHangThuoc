package infrastructure.repository;

import domain.dto.DemandForecastDTO;
import domain.dto.DemandPlanningEvaluationDTO;
import domain.dto.InternalTransferSuggestionDTO;
import domain.dto.ReplenishmentSuggestionDTO;
import domain.repository.IDemandPlanningRepository;
import infrastructure.database.DatabaseHelper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DemandPlanningRepositoryImpl implements IDemandPlanningRepository {
    @Override
    public void refreshDataMart(LocalDate fromDate, LocalDate toDate) {
        String sql = "{call sp_DemandPlanning_RefreshDataMart(?, ?)}";
        try (Connection conn = DatabaseHelper.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            if (fromDate != null) {
                cs.setDate(1, Date.valueOf(fromDate));
            } else {
                cs.setNull(1, Types.DATE);
            }
            if (toDate != null) {
                cs.setDate(2, Date.valueOf(toDate));
            } else {
                cs.setNull(2, Types.DATE);
            }
            cs.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot refresh demand planning datamart", e);
        }
    }

    @Override
    public void generateForecast(int horizonDays, int lookbackDays) {
        String sql = "{call sp_DemandPlanning_GenerateForecast(?, ?)}";
        try (Connection conn = DatabaseHelper.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, horizonDays);
            cs.setInt(2, lookbackDays);
            cs.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot generate demand forecast", e);
        }
    }

    @Override
    public void runOptimization(int horizonDays) {
        String sql = "{call sp_DemandPlanning_RunOptimization(?)}";
        try (Connection conn = DatabaseHelper.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, horizonDays);
            cs.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot run replenishment optimization", e);
        }
    }

    @Override
    public DemandPlanningEvaluationDTO evaluate(int backtestDays) {
        String sql = "{call sp_DemandPlanning_Evaluate(?)}";
        try (Connection conn = DatabaseHelper.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, backtestDays);
            try (ResultSet rs = cs.executeQuery()) {
                DemandPlanningEvaluationDTO dto = new DemandPlanningEvaluationDTO();
                if (rs.next()) {
                    dto.setSampleSize(rs.getInt("SampleSize"));
                    dto.setMae(rs.getDouble("MAE"));
                    dto.setRmse(rs.getDouble("RMSE"));
                    dto.setMape(rs.getDouble("MAPE"));
                }
                return dto;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot evaluate forecast quality", e);
        }
    }

    @Override
    public List<DemandForecastDTO> getLatestForecasts(int limit) {
        String sql = "SELECT TOP (?) ForecastDate, MaSP, MaCN, RegionKey, ForecastQty, LowerBoundQty, UpperBoundQty, ModelName, GeneratedAt " +
                "FROM DemandForecast ORDER BY GeneratedAt DESC, ForecastDate ASC";
        List<DemandForecastDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DemandForecastDTO dto = new DemandForecastDTO();
                    dto.setForecastDate(rs.getDate("ForecastDate").toLocalDate());
                    dto.setMaSP(rs.getInt("MaSP"));
                    dto.setMaCN(rs.getInt("MaCN"));
                    dto.setRegionKey(rs.getInt("RegionKey"));
                    dto.setForecastQty(rs.getBigDecimal("ForecastQty"));
                    dto.setLowerBoundQty(rs.getBigDecimal("LowerBoundQty"));
                    dto.setUpperBoundQty(rs.getBigDecimal("UpperBoundQty"));
                    dto.setModelName(rs.getNString("ModelName"));
                    dto.setGeneratedAt(rs.getTimestamp("GeneratedAt").toLocalDateTime());
                    list.add(dto);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load latest forecasts", e);
        }
    }

    @Override
    public List<ReplenishmentSuggestionDTO> getLatestReplenishmentSuggestions() {
        String sql = "SELECT TOP 200 MaSP, MaCN, ForecastWindowDays, CurrentStock, RequiredQty, SuggestedQty " +
                "FROM ReplenishmentSuggestion ORDER BY GeneratedAt DESC";
        List<ReplenishmentSuggestionDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReplenishmentSuggestionDTO dto = new ReplenishmentSuggestionDTO();
                dto.setMaSP(rs.getInt("MaSP"));
                dto.setMaCN(rs.getInt("MaCN"));
                dto.setForecastWindowDays(rs.getInt("ForecastWindowDays"));
                dto.setCurrentStock(rs.getInt("CurrentStock"));
                dto.setRequiredQty(rs.getInt("RequiredQty"));
                dto.setSuggestedQty(rs.getInt("SuggestedQty"));
                list.add(dto);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load replenishment suggestions", e);
        }
    }

    @Override
    public List<InternalTransferSuggestionDTO> getLatestTransferSuggestions() {
        String sql = "SELECT TOP 200 MaSP, FromMaCN, ToMaCN, SuggestedQty, Reason " +
                "FROM InternalTransferSuggestion ORDER BY GeneratedAt DESC";
        List<InternalTransferSuggestionDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InternalTransferSuggestionDTO dto = new InternalTransferSuggestionDTO();
                dto.setMaSP(rs.getInt("MaSP"));
                dto.setFromMaCN(rs.getInt("FromMaCN"));
                dto.setToMaCN(rs.getInt("ToMaCN"));
                dto.setSuggestedQty(rs.getInt("SuggestedQty"));
                dto.setReason(rs.getNString("Reason"));
                list.add(dto);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Cannot load transfer suggestions", e);
        }
    }
}
