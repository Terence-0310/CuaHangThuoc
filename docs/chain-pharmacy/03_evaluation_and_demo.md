# Evaluation va demo checklist

## 1. KPI nghiep vu can do
- StockoutRate theo chi nhanh va toan chuoi.
- ServiceLevel (so dong don dap ung / tong nhu cau).
- InventoryTurnover theo thang.
- SlowMovingRate (>45 ngay).

## 2. KPI mo hinh du bao
- MAE, RMSE, SMAPE theo SKU-khu vuc.
- Bias forecast de phat hien over-forecast va under-forecast.

## 3. Test plan
- Unit test:
  - `DemandForecastServiceImplTest`
  - `InventoryOptimizationServiceImplTest`
- Integration test (de nghi):
  - ETL procedure chay duoc voi khoang ngay 30-90 ngay.
  - FactSalesDaily dong bo dung so dong.
  - Goi y reorder thay doi theo lead-time/safety-stock.

## 4. Scenario demo de tai
1. Tao 2-3 chi nhanh o 2 khu vuc.
2. Chay ETL 90 ngay gan nhat.
3. Xem dashboard nhu cau theo khu vuc.
4. Chay forecast cho SKU ban chay.
5. Xem goi y nhap hang tai chi nhanh thieu.
6. Xem goi y dieu chuyen tu chi nhanh du.
7. Doi chieu KPI truoc/sau kich hoat goi y.

## 5. Gioi han va huong phat trien
- Forecast hien tai la moving-average baseline.
- Co the nang cap len LightGBM/XGBoost khi du du lieu.
- Co the them chi phi van chuyen vao thuat toan dieu chuyen noi bo.
