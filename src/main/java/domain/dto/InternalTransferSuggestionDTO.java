package domain.dto;

public class InternalTransferSuggestionDTO {
    private int maSP;
    private int fromMaCN;
    private int toMaCN;
    private int suggestedQty;
    private String reason;

    public int getMaSP() {
        return maSP;
    }

    public void setMaSP(int maSP) {
        this.maSP = maSP;
    }

    public int getFromMaCN() {
        return fromMaCN;
    }

    public void setFromMaCN(int fromMaCN) {
        this.fromMaCN = fromMaCN;
    }

    public int getToMaCN() {
        return toMaCN;
    }

    public void setToMaCN(int toMaCN) {
        this.toMaCN = toMaCN;
    }

    public int getSuggestedQty() {
        return suggestedQty;
    }

    public void setSuggestedQty(int suggestedQty) {
        this.suggestedQty = suggestedQty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
