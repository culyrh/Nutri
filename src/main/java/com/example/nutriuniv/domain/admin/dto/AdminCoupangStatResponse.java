package com.example.nutriuniv.domain.admin.dto;

import com.example.nutriuniv.domain.coupang.entity.CoupangDailyReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AdminCoupangStatResponse {
    private LocalDate date;
    private int click;
    private int orderCount;
    private int cancelCount;
    private int gmv;
    private int commission;

    public static AdminCoupangStatResponse from(CoupangDailyReport report) {
        return AdminCoupangStatResponse.builder()
                .date(report.getDate())
                .click(report.getClick())
                .orderCount(report.getOrderCount())
                .cancelCount(report.getCancelCount())
                .gmv(report.getGmv())
                .commission(report.getCommission())
                .build();
    }
}
