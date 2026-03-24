package domain.entity;

/**
 * Entity: Khách hàng
 */
public class Customer {
    private int maKH;
    private String soDT;
    private String tenKH;

    public Customer() {}

    public Customer(int maKH, String soDT, String tenKH) {
        this.maKH = maKH;
        this.soDT = soDT;
        this.tenKH = tenKH;
    }

    // --- Getters & Setters ---
    public int getMaKH() { return maKH; }
    public void setMaKH(int maKH) { this.maKH = maKH; }

    public String getSoDT() { return soDT; }
    public void setSoDT(String soDT) { this.soDT = soDT; }

    public String getTenKH() { return tenKH; }
    public void setTenKH(String tenKH) { this.tenKH = tenKH; }
}
