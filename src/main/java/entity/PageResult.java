package entity;

import java.util.List;

/**
 * Generic class chứa kết quả phân trang
 * 
 * @param <T> Kiểu dữ liệu trong danh sách
 * @author Generated
 * @version 1.0
 */
public class PageResult<T> {
    private List<T> data;
    private int totalRows;
    private int totalPages;
    private int currentPage;
    private int pageSize;

    public PageResult(List<T> data, int totalRows, int totalPages, int currentPage, int pageSize) {
        this.data = data;
        this.totalRows = totalRows;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    public List<T> getData() {
        return data;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean hasNextPage() {
        return currentPage < totalPages;
    }

    public boolean hasPreviousPage() {
        return currentPage > 1;
    }
}
