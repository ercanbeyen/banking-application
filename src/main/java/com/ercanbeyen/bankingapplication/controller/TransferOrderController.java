package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.option.TransferOrderOptions;
import com.ercanbeyen.bankingapplication.service.impl.TransferOrderService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer-orders")
public class TransferOrderController extends BaseController<TransferOrderDto, TransferOrderOptions> {
    private final TransferOrderService transferOrderService;

    public TransferOrderController(TransferOrderService transferOrderService) {
        super(transferOrderService);
        this.transferOrderService = transferOrderService;
    }


}
