package com.mercado_libre.gestion_productos.controller.admin;

import com.mercado_libre.gestion_productos.dto.AdminMetricsDTO;
import com.mercado_libre.gestion_productos.dto.AdminVendorUpdateRequest;
import com.mercado_libre.gestion_productos.dto.VendorAdminDTO;
import com.mercado_libre.gestion_productos.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/metrics")
    public AdminMetricsDTO metrics(@RequestParam(required = false) Integer threshold) {
        return adminService.getMetrics(threshold);
    }

    @GetMapping("/vendors")
    public List<VendorAdminDTO> listVendors() {
        return adminService.listVendors();
    }

    @PutMapping("/vendors/{id}")
    public VendorAdminDTO updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody AdminVendorUpdateRequest request
    ) {
        return adminService.updateVendor(id, request);
    }

    @DeleteMapping("/vendors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVendor(@PathVariable Long id) {
        adminService.deleteVendor(id);
    }
}
