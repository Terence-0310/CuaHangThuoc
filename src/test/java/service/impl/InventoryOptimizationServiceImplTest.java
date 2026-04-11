package service.impl;

import domain.dto.BranchInventoryDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class InventoryOptimizationServiceImplTest {

    @Test
    void suggestReorder_shouldSuggestWhenBelowTarget() {
        InventoryOptimizationServiceImpl service = new InventoryOptimizationServiceImpl();
        BranchInventoryDTO row = new BranchInventoryDTO();
        row.setMaCN(1);
        row.setMaSP(1);
        row.setTonHienTai(20);
        row.setMucTonMucTieu(100);
        row.setSoNgayLeadTime(2);
        row.setSoNgayTonAnToan(5);

        var suggestion = service.suggestReorder(row, 10);
        Assertions.assertTrue(suggestion.getSuggestedOrderQty() > 0);
    }

    @Test
    void suggestInternalTransfers_shouldSuggestFromSurplusToShortage() {
        InventoryOptimizationServiceImpl service = new InventoryOptimizationServiceImpl();
        List<BranchInventoryDTO> rows = new ArrayList<>();

        BranchInventoryDTO donor = new BranchInventoryDTO();
        donor.setMaCN(1);
        donor.setMaSP(1);
        donor.setTonHienTai(200);
        donor.setMucTonMucTieu(100);
        donor.setMucTonToiThieu(60);
        rows.add(donor);

        BranchInventoryDTO receiver = new BranchInventoryDTO();
        receiver.setMaCN(2);
        receiver.setMaSP(1);
        receiver.setTonHienTai(20);
        receiver.setMucTonMucTieu(80);
        receiver.setMucTonToiThieu(60);
        rows.add(receiver);

        var transfers = service.suggestInternalTransfers(rows);
        Assertions.assertFalse(transfers.isEmpty());
        Assertions.assertEquals(1, transfers.get(0).getMaCNTu());
        Assertions.assertEquals(2, transfers.get(0).getMaCNDen());
    }
}
