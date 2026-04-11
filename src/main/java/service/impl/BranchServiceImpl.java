package service.impl;

import domain.dto.BranchInventoryDTO;
import domain.entity.Branch;
import domain.entity.Region;
import domain.repository.IBranchInventoryRepository;
import domain.repository.IBranchRepository;
import infrastructure.repository.BranchInventoryRepositoryImpl;
import infrastructure.repository.BranchRepositoryImpl;
import service.IBranchService;

import java.util.List;

public class BranchServiceImpl implements IBranchService {
    private final IBranchRepository branchRepository;
    private final IBranchInventoryRepository branchInventoryRepository;

    public BranchServiceImpl() {
        this.branchRepository = new BranchRepositoryImpl();
        this.branchInventoryRepository = new BranchInventoryRepositoryImpl();
    }

    public BranchServiceImpl(IBranchRepository branchRepository, IBranchInventoryRepository branchInventoryRepository) {
        this.branchRepository = branchRepository;
        this.branchInventoryRepository = branchInventoryRepository;
    }

    @Override
    public List<Region> getRegions() {
        return branchRepository.findAllRegions();
    }

    @Override
    public List<Branch> getBranches() {
        return branchRepository.findAllBranches();
    }

    @Override
    public int createRegion(Region region) {
        return branchRepository.insertRegion(region);
    }

    @Override
    public int createBranch(Branch branch) {
        return branchRepository.insertBranch(branch);
    }

    @Override
    public boolean updateBranch(Branch branch) {
        return branchRepository.updateBranch(branch);
    }

    @Override
    public boolean setBranchStatus(int maCN, boolean trangThai) {
        return branchRepository.setBranchStatus(maCN, trangThai);
    }

    @Override
    public List<BranchInventoryDTO> getBranchInventory(int maCN) {
        return branchInventoryRepository.findByBranch(maCN);
    }

    @Override
    public boolean upsertBranchInventory(BranchInventoryDTO dto) {
        return branchInventoryRepository.upsert(dto);
    }
}
