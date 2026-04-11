# Scope, KPI, va MVP cho de tai chuoi nha thuoc

## 1. Muc tieu nghiep vu
- Quan ly da chi nhanh trong mot he thong duy nhat.
- Theo doi nhu cau thuoc theo khu vuc va theo thoi gian.
- Ho tro quyet dinh nhap hang va dieu chuyen noi bo de giam het hang.

## 2. Pham vi MVP bat buoc
- Quan ly chi nhanh va gan khu vuc cho chi nhanh.
- Quan ly ton kho theo chi nhanh va san pham.
- Ghi nhan giao dich ban hang/nhap hang theo chi nhanh.
- Dashboard nhu cau theo khu vuc (ngay, tuan, thang).
- Forecast co ban theo SKU-khu vuc-thoi gian.
- Goi y nhap hang (reorder point + safety stock) va goi y dieu chuyen noi bo.

## 3. KPI nghiep vu
| KPI | Dinh nghia | Muc tieu de tai |
|---|---|---|
| StockoutRate | Ti le SKU-bi-thieu trong ky | Giam >= 20% so voi baseline |
| ServiceLevel | Ti le nhu cau duoc dap ung | Dat >= 95% |
| InventoryTurnover | Vong quay ton kho trung binh | Tang >= 10% |
| SlowMovingRate | Ti le ton kho cham luan chuyen (>45 ngay) | Giam >= 15% |
| ReportLatency | Thoi gian tong hop bao cao khu vuc | Giam >= 50% |

## 4. KPI mo hinh du bao
| KPI | Dinh nghia | Muc tieu de tai |
|---|---|---|
| MAE | Sai so tuyet doi trung binh | Thap hon baseline MovingAverage |
| RMSE | Sai so binh phuong trung binh | Theo doi on dinh theo tung nhom thuoc |
| SMAPE | Sai so phan tram doi xung | <= 25% voi nhom thuoc ban chay |
| Bias | Trung binh (du bao - thuc te) | Gan 0 de tranh over/under ordering |

## 5. Baseline doi chieu
- Baseline 1: Moving Average 7 ngay.
- Baseline 2: Seasonal naive (bang ngay cung thu trong tuan truoc).
- Model de tai duoc coi la dat khi MAE va SMAPE tot hon it nhat mot baseline tren >= 10%.

## 6. Nguyen tac trien khai
- Uu tien do tin cay va kha nang giai thich hon do phuc tap.
- Tuyen bo ro pham vi: AI ho tro ra quyet dinh, khong tu dong dat don.
- Tat ca goi y deu co thong tin giai thich (nhu cau du bao, ton hien tai, lead time, safety stock).
