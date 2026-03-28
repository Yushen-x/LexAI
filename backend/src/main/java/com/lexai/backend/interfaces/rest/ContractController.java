package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.contract.ContractListResponse;
import com.lexai.backend.application.dto.contract.ContractResponse;
import com.lexai.backend.application.dto.contract.CreateContractRequest;
import com.lexai.backend.application.dto.contract.UpdateContractStatusRequest;
import com.lexai.backend.application.service.ContractService;
import com.lexai.backend.common.api.ApiResponse;
import com.lexai.backend.domain.model.ContractStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * 分页列表：关键词匹配名称/编号，可选按状态、类型筛选。
     */
    @GetMapping
    public ApiResponse<ContractListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(contractService.list(keyword, status, type, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> get(@PathVariable long id) {
        return ApiResponse.success(contractService.getById(id));
    }

    @PostMapping
    public ApiResponse<ContractResponse> create(@Valid @RequestBody CreateContractRequest request) {
        return ApiResponse.success(contractService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ContractResponse> updateStatus(
            @PathVariable long id,
            @Valid @RequestBody UpdateContractStatusRequest request
    ) {
        return ApiResponse.success(contractService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        contractService.softDelete(id);
        return ApiResponse.success(null);
    }
}
