package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.CreditCardApplyDTO;
import com.student.bank.service.CreditCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {
    private final CreditCardService creditCardService;

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@Valid @RequestBody CreditCardApplyDTO dto) {
        return Result.success(creditCardService.apply(dto));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(creditCardService.listCards());
    }

    @PostMapping("/{cardId}/activate")
    public Result<Void> activate(@PathVariable Long cardId, @RequestBody Map<String, String> body) {
        creditCardService.activate(cardId, body.get("tradePassword"));
        return Result.success("激活成功", null);
    }

    @GetMapping("/{cardId}/bills")
    public Result<List<Map<String, Object>>> bills(@PathVariable Long cardId) {
        return Result.success(creditCardService.bills(cardId));
    }

    @PostMapping("/bills/{billId}/installment")
    public Result<Map<String, Object>> installment(@PathVariable Long billId, @RequestParam Integer periods) {
        return Result.success(creditCardService.installment(billId, periods));
    }
}
