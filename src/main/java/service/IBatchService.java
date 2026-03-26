package service;

import domain.entity.Batch;
import java.util.List;

/**
 * Service Interface: Quản lý lô hàng / nhập kho
 */
public interface IBatchService {
    int importBatch(Batch batch);
    List<Batch> getByProductId(int maSP);
    List<Batch> getAllWithProduct();

    // Clean Arch: BatchActionDialog — Trả hàng NCC / Hủy hàng
    void returnBatch(int maLo, int maSP, int soLuong, long giaNhapLo,
                     long tongTien, String hinhThucHoan, String tinhTrang, String ghiChu)
            throws java.sql.SQLException;

    void destroyBatch(int maLo, int maSP, int soLuong, long giaNhapLo,
                      long tongTien, String phanLoaiLyDo, String chiTietLyDo)
            throws java.sql.SQLException;

    List<Object[]> getSalesHistoryByBatch(int maLo);
}
