package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.EquipmentDemandReport;
import mk.finki.roomreservation.model.RoomUtilizationReport;
import mk.finki.roomreservation.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/reports/room-utilization")
    public List<RoomUtilizationReport> roomUtilizationReport() {
        return reportService.roomUtilizationReport();
    }

    @GetMapping("/api/reports/equipment-demand")
    public List<EquipmentDemandReport> equipmentDemandReport() {
        return reportService.equipmentDemandReport();
    }
}