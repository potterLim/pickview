package com.pickview.api;

import com.pickview.domain.AccountId;
import com.pickview.domain.EApprovalDecision;
import com.pickview.domain.EProductDecision;
import com.pickview.domain.ERole;
import com.pickview.domain.ProductId;
import com.pickview.domain.TicketId;
import com.pickview.operations.OperationsService;
import com.pickview.security.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        return mOperations.getDashboard(mAccounts.requireAccount(new AccountId(principal.getName())));
    }

    @GetMapping("/api/seller/settlements")
    public OperationsService.SellerSettlementView getSellerSettlements(Principal principal) {
        return mOperations.getSellerSettlementSummary(new AccountId(principal.getName()));
    }

    @PostMapping("/api/admin/sellers/{id}")
    public void reviewSeller(Principal principal, @PathVariable String id, @Valid @RequestBody ApprovalRequest request) {
        mOperations.reviewSeller(mAccounts.requireAccount(new AccountId(principal.getName())), new AccountId(id), request.toDecision());
    }

    @PostMapping("/api/admin/products/{id}")
    public void reviewProduct(Principal principal, @PathVariable String id, @Valid @RequestBody DecisionRequest request) {
        mOperations.reviewProduct(mAccounts.requireAccount(new AccountId(principal.getName())), new ProductId(id), request.decision());
    }

    @PostMapping("/api/admin/tickets/{id}")
    public void resolveTicket(Principal principal, @PathVariable String id, @Valid @RequestBody ResolutionRequest request) {
        mOperations.resolveTicket(
            mAccounts.requireAccount(new AccountId(principal.getName())),
            new TicketId(id),
            request.reply(),
            request.toDecision()
        );
    }

    @PostMapping("/api/admin/roles/{id}")
    public void changeRole(Principal principal, @PathVariable String id, @Valid @RequestBody RoleRequest request) {
        mOperations.changeRole(mAccounts.requireAccount(new AccountId(principal.getName())), new AccountId(id), request.decision());
    }

    @PostMapping("/api/admin/settlements/{id}")
    public Map<String, Integer> settle(Principal principal, @PathVariable String id) {
        return Map.of("amountWon", mOperations.settle(mAccounts.requireAccount(new AccountId(principal.getName())), new AccountId(id)));
    }

    public record DecisionRequest(@NotNull EProductDecision decision) {
    }

    public record RoleRequest(@NotNull ERole decision) {
    }

    public record ApprovalRequest(@NotNull Boolean approve) {
        public EApprovalDecision toDecision() {
            if (approve == null) {
                throw new ApiFailure(400, "Explicit approval decision required");
            }
            return approve ? EApprovalDecision.APPROVE : EApprovalDecision.REJECT;
        }
    }

    public record ResolutionRequest(@NotNull Boolean approve, @NotBlank String reply) {
        public EApprovalDecision toDecision() {
            return new ApprovalRequest(approve).toDecision();
        }
    }
}
