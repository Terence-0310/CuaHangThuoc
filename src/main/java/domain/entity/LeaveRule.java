package domain.entity;

public class LeaveRule {
    private int ruleID;
    private int maxPerMonth;
    private int maxPerYear;

    public int getRuleID() { return ruleID; }
    public void setRuleID(int ruleID) { this.ruleID = ruleID; }
    public int getMaxPerMonth() { return maxPerMonth; }
    public void setMaxPerMonth(int maxPerMonth) { this.maxPerMonth = maxPerMonth; }
    public int getMaxPerYear() { return maxPerYear; }
    public void setMaxPerYear(int maxPerYear) { this.maxPerYear = maxPerYear; }
}
