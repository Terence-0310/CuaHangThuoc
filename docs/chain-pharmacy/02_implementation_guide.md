# Huong dan trien khai module chuoi nha thuoc

## 1. Migration database
1. Chay `database/run_all_migrations.ps1`.
2. Script se tu dong chay them:
   - `42_chain_pharmacy_schema.sql`
   - `43_chain_pharmacy_etl_forecast.sql`

## 2. Core modules da bo sung
- Quan ly khu vuc va chi nhanh:
  - `domain.entity.Region`
  - `domain.entity.Branch`
  - `service.IBranchService`
  - `service.impl.BranchServiceImpl`
- Quan ly ton kho theo chi nhanh:
  - `domain.dto.BranchInventoryDTO`
  - `domain.repository.IBranchInventoryRepository`
  - `infrastructure.repository.BranchInventoryRepositoryImpl`

## 3. Forecast pipeline
- ETL:
  - `sp_PopulateDimTime`
  - `sp_RefreshFactSalesDaily`
  - `sp_RefreshFactInventoryDaily`
- Feature view:
  - `vw_DemandFeaturesDaily`
- Java service:
  - `service.IDemandForecastService`
  - `service.impl.DemandForecastServiceImpl`

## 4. Optimization layer
- Reorder suggestion:
  - `service.IInventoryOptimizationService#suggestReorder`
- Internal transfer suggestion:
  - `service.IInventoryOptimizationService#suggestInternalTransfers`
- DTO outputs:
  - `domain.dto.ReorderSuggestionDTO`
  - `domain.dto.TransferSuggestionDTO`

## 5. Cac buoc goi y tich hop UI
- Them tab "Quan ly chi nhanh" trong trang admin.
- Them dashboard "Nhu cau theo khu vuc" doc tu `vw_DemandByRegionDaily`.
- Them man hinh "Goi y nhap hang" doc tu service optimization.
- Them man hinh "Goi y dieu chuyen" de tao phieu `DieuChuyenKho`.
