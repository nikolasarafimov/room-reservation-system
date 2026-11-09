package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.DashboardStats;
import mk.finki.roomreservation.model.HealthStatus;
import mk.finki.roomreservation.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/health")
    public HealthStatus health() {
        return dashboardService.health();
    }

    @GetMapping("/api/dashboard")
    public DashboardStats dashboard() {
        return dashboardService.getDashboardStats();
    }
}