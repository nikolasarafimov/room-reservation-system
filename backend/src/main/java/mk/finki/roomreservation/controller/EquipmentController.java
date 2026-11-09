package mk.finki.roomreservation.controller;

import mk.finki.roomreservation.model.EquipmentOption;
import mk.finki.roomreservation.service.EquipmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/api/equipment")
    public List<EquipmentOption> findAllEquipment() {
        return equipmentService.findAllEquipment();
    }
}