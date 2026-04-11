package domain.repository;

import domain.dto.BranchInventoryDTO;

import java.util.List;

public interface IBranchInventoryRepository {
    List<BranchInventoryDTO> findByBranch(int maCN);
    boolean upsert(BranchInventoryDTO dto);
}
