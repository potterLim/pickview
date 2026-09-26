package com.pickview.api;

import com.pickview.operations.OperationsService;
import com.pickview.security.AccountService;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationsController {

    private final OperationsService mOperations;
    private final AccountService mAccounts;

    public OperationsController(OperationsService operations, AccountService accounts) {
        mOperations = operations;
        mAccounts = accounts;
    }

    @GetMapping("/api/admin/dashboard")
    public OperationsService.DashboardView getDashboard(Principal principal) {
        return mOperations.getDashboard(mAccounts.requireAccount(principal.getName()));
    }

    @GetMapping("/api/seller/settlements")
    public OperationsService.SellerSettlementView getSellerSettlements(Principal principal) {
        return mOperations.getSellerSettlementSummary(principal.getName());
    }

    @PostMapping("/api/admin/sellers/{id}")
    public void reviewSeller(Principal principal, @PathVariable String id, @RequestBody DecisionRequest request) {
        mOperations.reviewSeller(mAccounts.requireAccount(principal.getName()), id, request.approve());
    }

    @PostMapping("/api/admin/products/{id}")
    public void reviewProduct(Principal principal, @PathVariable String id, @RequestBody DecisionRequest request) {
        mOperations.reviewProduct(mAccounts.requireAccount(principal.getName()), id, request.decision());
    }

    @PostMapping("/api/admin/tickets/{id}")
    public void resolveTicket(Principal principal, @PathVariable String id, @RequestBody DecisionRequest request) {
        mOperations.resolveTicket(
            mAccounts.requireAccount(principal.getName()),
            id,
            request.reply(),
            request.approve()
        );
    }

    @PostMapping("/api/admin/roles/{id}")
    public void changeRole(Principal principal, @PathVariable String id, @RequestBody DecisionRequest request) {
        mOperations.changeRole(mAccounts.requireAccount(principal.getName()), id, request.decision());
    }

    @PostMapping("/api/admin/settlements/{id}")
    public Map<String, Integer> settle(Principal principal, @PathVariable String id) {
        return Map.of("amountWon", mOperations.settle(mAccounts.requireAccount(principal.getName()), id));
    }

    public record DecisionRequest(String decision, String reply, boolean approve) {
        public DecisionRequest {
            decision = decision == null ? "" : decision;
            reply = reply == null ? "" : reply;
        }
    }
}
