package domain.repository;

import domain.entity.Branch;
import domain.entity.Region;

import java.util.List;

public interface IBranchRepository {
    List<Region> findAllRegions();
    List<Branch> findAllBranches();
    int insertRegion(Region region);
    int insertBranch(Branch branch);
    boolean updateBranch(Branch branch);
    boolean setBranchStatus(int maCN, boolean trangThai);
}
