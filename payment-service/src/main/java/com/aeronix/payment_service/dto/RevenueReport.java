package com.aeronix.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevenueReport {
    private Double totalRevenue;
    private Double periodRevenue;
    private Long totalTransactions;
    private Long periodTransactions;
    private String fromDate;
    private String toDate;
}