package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.ExchangeService;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import com.ercanbeyen.bankingapplication.dto.option.ExchangeFilteringOption;
import com.ercanbeyen.bankingapplication.util.ExchangeUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchanges")
@SecurityRequirement(name = "Bearer Authentication")
public class ExchangeController extends BaseController<ExchangeDto, ExchangeFilteringOption> {
    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        super(exchangeService);
        this.exchangeService = exchangeService;
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @Override
    public ResponseEntity<List<ExchangeDto>> getEntities(ExchangeFilteringOption filteringOption) {
         return ResponseEntity.ok(exchangeService.getEntities(filteringOption));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @Override
    public ResponseEntity<ExchangeDto> getEntity(Integer id) {
        return ResponseEntity.ok(exchangeService.getEntity(id));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<ExchangeDto> createEntity(ExchangeDto request) {
        return ResponseEntity.ok(exchangeService.createEntity(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<ExchangeDto> updateEntity(Integer id, ExchangeDto request) {
        return ResponseEntity.ok(exchangeService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<Void> deleteEntity(Integer id) {
        exchangeService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/views")
    public ResponseEntity<List<ExchangeView>> getExchangeViews() {
        return ResponseEntity.ok(exchangeService.getExchangeViews());
    }

    @GetMapping("/{from}/{to}/{amount}")
    public ResponseEntity<MessageResponse<Double>> convertMoneyBetweenCurrencies(
            @PathVariable("from") Currency fromCurrency,
            @PathVariable("to") Currency toCurrency,
            @PathVariable("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        ExchangeUtil.checkCurrenciesBeforeMoneyExchange(fromCurrency, toCurrency);
        MessageResponse<Double> response = new MessageResponse<>(exchangeService.convertMoneyBetweenCurrencies(fromCurrency, toCurrency, amount));
        return ResponseEntity.ok(response);
    }
}
