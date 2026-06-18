package com.example.fproject.Enum;

public enum SubscriptionPlanType {

    BASIC_MONTHLY(99.0, 1, 3, 1),
    PROFESSIONAL_MONTHLY(199.0, 3, 3, 1),
    PROFESSIONAL_YEARLY(1999.0, 3, 3, 12);

    private final Double price;
    private final Integer maxStores;
    private final Integer maxBranchesPerStore;
    private final Integer durationMonths;

    SubscriptionPlanType(Double price, Integer maxStores, Integer maxBranchesPerStore, Integer durationMonths) {
        this.price = price;
        this.maxStores = maxStores;
        this.maxBranchesPerStore = maxBranchesPerStore;
        this.durationMonths = durationMonths;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getMaxStores() {
        return maxStores;
    }

    public Integer getMaxBranchesPerStore() {
        return maxBranchesPerStore;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }
}