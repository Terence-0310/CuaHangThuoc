package service;

import domain.dto.BranchInventoryDTO;
import domain.entity.Branch;
import domain.entity.Region;

import java.util.List;

public interface IBranchService {
    List<Region> getRegions();
    List<Branch> getBranches();
    int createRegion(Region region);
    int createBranch(Branch branch);
    boolean updateBranch(Branch branch);
    boolean setBranchStatus(int maCN, boolean trangThai);
    List<BranchInventoryDTO> getBranchInventory(int maCN);
    boolean upsertBranchInventory(BranchInventoryDTO dto);
}
