package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.ExchangeService;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import com.ercanbeyen.bankingapplication.option.ExchangeFilteringOption;
import com.ercanbeyen.bankingapplication.util.ExchangeUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchanges")
public class ExchangeController extends BaseController<ExchangeDto, ExchangeFilteringOption> {
    private final ExchangeService exchangeService;

    public ExchangeController(ExchangeService exchangeService) {
        super(exchangeService);
        this.exchangeService = exchangeService;
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
