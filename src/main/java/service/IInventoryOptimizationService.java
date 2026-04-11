package service;

import domain.dto.BranchInventoryDTO;
import domain.dto.ReorderSuggestionDTO;
import domain.dto.TransferSuggestionDTO;

import java.util.List;

public interface IInventoryOptimizationService {
    ReorderSuggestionDTO suggestReorder(BranchInventoryDTO inventory, int dailyDemandForecast);
    List<ReorderSuggestionDTO> suggestBulkReorder(List<BranchInventoryDTO> inventoryRows, int dailyDemandForecast);
    List<TransferSuggestionDTO> suggestInternalTransfers(List<BranchInventoryDTO> inventoryRows);
}
