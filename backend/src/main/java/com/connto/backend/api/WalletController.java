package com.connto.backend.api;

import com.connto.backend.service.WalletService;
import com.connto.backend.service.WalletService.OfferResponse;
import com.connto.backend.service.WalletService.TopupInitiateResponse;
import com.connto.backend.service.WalletService.TransactionResponse;
import com.connto.backend.service.WalletService.WalletBalanceResponse;
import com.connto.backend.web.CurrentUser;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallet/offers")
    public List<OfferResponse> offers() {
        return walletService.listActiveOffers();
    }

    @GetMapping("/me/wallet")
    public WalletBalanceResponse myWallet() {
        return walletService.getBalance(CurrentUser.id());
    }

    @GetMapping("/me/wallet/transactions")
    public List<TransactionResponse> transactions() {
        return walletService.listTransactions(CurrentUser.id());
    }

    @PostMapping("/me/wallet/topup/initiate")
    public TopupInitiateResponse initiateTopup(@RequestBody InitiateTopupRequest body) {
        return walletService.initiateTopup(CurrentUser.id(), body.offerId());
    }

    @PostMapping("/me/wallet/topup/complete-mock")
    public WalletBalanceResponse completeMock(@RequestBody CompleteMockRequest body) {
        return walletService.completeMockTopup(CurrentUser.id(), body.sessionId());
    }

    /** MVP: after user pays via UPI app, confirms here so wallet credits (disable in prod when webhooks exist). */
    @PostMapping("/me/wallet/topup/complete-upi")
    public WalletBalanceResponse completeUpi(@RequestBody CompleteMockRequest body) {
        return walletService.completeUpiTopup(CurrentUser.id(), body.sessionId());
    }

    public record InitiateTopupRequest(@NotNull UUID offerId) {}

    public record CompleteMockRequest(@NotNull UUID sessionId) {}
}
