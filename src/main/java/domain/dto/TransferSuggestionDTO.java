package domain.dto;

public class TransferSuggestionDTO {
    private int maSP;
    private int maCNTu;
    private int maCNDen;
    private int soLuongDeXuat;

    public int getMaSP() {
        return maSP;
    }

    public void setMaSP(int maSP) {
        this.maSP = maSP;
    }

    public int getMaCNTu() {
        return maCNTu;
    }

    public void setMaCNTu(int maCNTu) {
        this.maCNTu = maCNTu;
    }

    public int getMaCNDen() {
        return maCNDen;
    }

    public void setMaCNDen(int maCNDen) {
        this.maCNDen = maCNDen;
    }

    public int getSoLuongDeXuat() {
        return soLuongDeXuat;
    }

    public void setSoLuongDeXuat(int soLuongDeXuat) {
        this.soLuongDeXuat = soLuongDeXuat;
    }
}
