package com.codingshuttle.project.uber.uberApp.services;

import com.codingshuttle.project.uber.uberApp.dto.AdminRevenueDto;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    AdminRevenueDto getRevenueStats(Pageable pageable);
}
