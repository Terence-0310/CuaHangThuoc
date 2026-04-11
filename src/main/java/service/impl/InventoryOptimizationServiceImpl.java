package service.impl;

import domain.dto.BranchInventoryDTO;
import domain.dto.ReorderSuggestionDTO;
import domain.dto.TransferSuggestionDTO;
import service.IInventoryOptimizationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InventoryOptimizationServiceImpl implements IInventoryOptimizationService {

    @Override
    public ReorderSuggestionDTO suggestReorder(BranchInventoryDTO inventory, int dailyDemandForecast) {
        int demand = Math.max(0, dailyDemandForecast);
        int safetyStock = demand * Math.max(1, inventory.getSoNgayTonAnToan());
        int reorderPoint = (demand * Math.max(1, inventory.getSoNgayLeadTime())) + safetyStock;

        int targetStock = Math.max(inventory.getMucTonMucTieu(), reorderPoint);
        int suggestion = Math.max(0, targetStock - inventory.getTonHienTai());

        ReorderSuggestionDTO dto = new ReorderSuggestionDTO();
        dto.setMaCN(inventory.getMaCN());
        dto.setMaSP(inventory.getMaSP());
        dto.setTonHienTai(inventory.getTonHienTai());
        dto.setDemandDailyForecast(demand);
        dto.setSafetyStock(safetyStock);
        dto.setReorderPoint(reorderPoint);
        dto.setSuggestedOrderQty(suggestion);
        return dto;
    }

    @Override
    public List<ReorderSuggestionDTO> suggestBulkReorder(List<BranchInventoryDTO> inventoryRows, int dailyDemandForecast) {
        List<ReorderSuggestionDTO> out = new ArrayList<>();
        for (BranchInventoryDTO row : inventoryRows) {
            out.add(suggestReorder(row, dailyDemandForecast));
        }
        return out;
    }

    @Override
    public List<TransferSuggestionDTO> suggestInternalTransfers(List<BranchInventoryDTO> inventoryRows) {
        List<TransferSuggestionDTO> out = new ArrayList<>();

        Map<Integer, List<BranchInventoryDTO>> byProduct = inventoryRows.stream()
                .collect(Collectors.groupingBy(BranchInventoryDTO::getMaSP));

        for (Map.Entry<Integer, List<BranchInventoryDTO>> entry : byProduct.entrySet()) {
            List<BranchInventoryDTO> rows = entry.getValue();
            List<BranchInventoryDTO> donors = rows.stream()
                    .filter(r -> r.getTonHienTai() > r.getMucTonMucTieu())
                    .sorted(Comparator.comparingInt(BranchInventoryDTO::getTonHienTai).reversed())
                    .collect(Collectors.toList());
            List<BranchInventoryDTO> receivers = rows.stream()
                    .filter(r -> r.getTonHienTai() < r.getMucTonToiThieu())
                    .sorted(Comparator.comparingInt(BranchInventoryDTO::getTonHienTai))
                    .collect(Collectors.toList());

            int di = 0;
            int ri = 0;
            while (di < donors.size() && ri < receivers.size()) {
                BranchInventoryDTO donor = donors.get(di);
                BranchInventoryDTO receiver = receivers.get(ri);

                int surplus = donor.getTonHienTai() - donor.getMucTonMucTieu();
                int shortage = receiver.getMucTonToiThieu() - receiver.getTonHienTai();
                int qty = Math.min(surplus, shortage);

                if (qty > 0) {
                    TransferSuggestionDTO transfer = new TransferSuggestionDTO();
                    transfer.setMaSP(entry.getKey());
                    transfer.setMaCNTu(donor.getMaCN());
                    transfer.setMaCNDen(receiver.getMaCN());
                    transfer.setSoLuongDeXuat(qty);
                    out.add(transfer);

                    donor.setTonHienTai(donor.getTonHienTai() - qty);
                    receiver.setTonHienTai(receiver.getTonHienTai() + qty);
                }

                if (donor.getTonHienTai() <= donor.getMucTonMucTieu()) {
                    di++;
                }
                if (receiver.getTonHienTai() >= receiver.getMucTonToiThieu()) {
                    ri++;
                }
            }
        }

        return out;
    }
}
