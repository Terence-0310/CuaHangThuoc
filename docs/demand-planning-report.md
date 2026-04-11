# Demand Planning Report

## 1. Scope implemented

- Designed branch/region/time dimensional model for demand analytics.
- Added ETL into `FactSalesDaily` and `FactInventoryDaily`.
- Implemented baseline demand forecast by `SKU x branch x day`.
- Added replenishment and internal transfer suggestions using forecast + safety stock.
- Added quantitative evaluation (MAE, RMSE, MAPE) for thesis/reporting.

## 2. Data model summary

- **Operational extension**
  - `ChiNhanh`
  - `HoaDon.MaCN`
  - `PhieuNhap.MaCN`
- **Data mart**
  - `DimTime`, `DimRegion`
  - `FactSalesDaily`, `FactInventoryDaily`
- **AI decision layer**
  - `DemandForecast`
  - `InventoryPolicy`
  - `ReplenishmentSuggestion`
  - `InternalTransferSuggestion`

Migration: `database/42_demand_planning_datamart.sql`

## 3. ETL + Forecast + Optimization pipeline

Stored procedures:

- `sp_DemandPlanning_RefreshDataMart(@FromDate, @ToDate)`
- `sp_DemandPlanning_GenerateForecast(@HorizonDays, @LookbackDays)`
- `sp_DemandPlanning_RunOptimization(@HorizonDays)`
- `sp_DemandPlanning_Evaluate(@BacktestDays)`

Application integration:

- Repository: `infrastructure/repository/DemandPlanningRepositoryImpl`
- Service: `service/IDemandPlanningService`, `service/impl/DemandPlanningServiceImpl`
- Runner: `tools/DemandPlanningRunner`

## 4. Evaluation metrics

The implemented evaluation uses:

- `MAE`: average absolute error.
- `RMSE`: root mean squared error.
- `MAPE`: mean absolute percentage error.

Metric query source:

- `vw_DemandPlanning_Evaluation`
- `sp_DemandPlanning_Evaluate`

## 5. Demo script (for defense)

1. Run all migrations:
   - `powershell -ExecutionPolicy Bypass -File .\database\run_all_migrations.ps1`
2. Run pipeline:
   - `powershell -ExecutionPolicy Bypass -File .\scripts\run-demand-planning.ps1`
3. Present outputs:
   - Forecast rows from `DemandForecast`
   - Replenishment suggestions from `ReplenishmentSuggestion`
   - Internal transfer proposals from `InternalTransferSuggestion`
   - KPI metrics: MAE/RMSE/MAPE from procedure output

## 6. Notes and limitations

- Baseline model is a rolling-mean approach suitable for MVP and explainability.
- Promotion/weather/outbreak features are not yet integrated in this baseline.
- Multi-branch stock is currently simulated on top of existing single-stock operations and should be extended in future UI workflows.
