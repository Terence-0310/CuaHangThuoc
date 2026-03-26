package service.impl;

import domain.dto.InventoryBatchDTO;
import domain.repository.IInventoryRepository;
import service.IInventoryService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class InventoryServiceImpl implements IInventoryService {

    private final IInventoryRepository repository;

    public InventoryServiceImpl(IInventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InventoryBatchDTO> getPagedBatches(int offset, int pageSize, String keyword, String statusFilter, String sortCol, String sortDir) {
        return repository.getPagedBatches(offset, pageSize, keyword, statusFilter, sortCol, sortDir);
    }

    @Override
    public int countBatches(String keyword, String statusFilter) {
        return repository.countBatches(keyword, statusFilter);
    }

    @Override
    public boolean updateBatch(int maLo, BigDecimal giaNhap, LocalDate hanSuDung, int newSoLuong) {
        return repository.updateBatch(maLo, giaNhap, hanSuDung, newSoLuong);
    }

    @Override
    public InventoryBatchDTO getBatchInfo(String soLo, String tenSP) {
        return repository.getBatchInfo(soLo, tenSP);
    }

    @Override
    public boolean isDuplicateBatch(String soLo, String tenSP, int excludeMaLo) {
        return repository.isDuplicateBatch(soLo, tenSP, excludeMaLo);
    }

    @Override
    public String checkForeignKeyConstraints(int maLo) {
        return repository.checkForeignKeyConstraints(maLo);
    }

    @Override
    public boolean deleteBatch(int maLo) {
        return repository.deleteBatch(maLo);
    }

    @Override
    public List<Object[]> getBatchHistory(int maLo) {
        return repository.getBatchHistory(maLo);
    }

    @Override
    public List<Object[]> getReturnHistory() {
        return repository.getReturnHistory();
    }

    @Override
    public List<Object[]> getDestroyHistory() {
        return repository.getDestroyHistory();
    }

    @Override
    public int getOriginalQuantity(int maLo) {
        return repository.getOriginalQuantity(maLo);
    }

    @Override
    public int countHetHSD() {
        return repository.countHetHSD();
    }

    @Override
    public int countCanDate() {
        return repository.countCanDate();
    }

    @Override
    public int countHetHang() {
        return repository.countHetHang();
    }
}
