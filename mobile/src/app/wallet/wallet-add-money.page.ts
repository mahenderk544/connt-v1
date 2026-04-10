import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { TopupInitiateResponse, WalletOffer } from '../core/api.types';
import { ConntoApiService } from '../core/connto-api.service';

@Component({
  selector: 'app-wallet-add-money',
  templateUrl: './wallet-add-money.page.html',
  styleUrls: ['./wallet-add-money.page.scss'],
  standalone: false,
})
export class WalletAddMoneyPage {
  offers: WalletOffer[] = [];
  loadingOffers = false;
  busy = false;
  /** Last checkout step (after user picks a pack). */
  pending: TopupInitiateResponse | null = null;

  constructor(
    private api: ConntoApiService,
    private router: Router,
    private toast: ToastController,
  ) {}

  ionViewWillEnter(): void {
    this.loadOffers();
    this.pending = null;
  }

  loadOffers(): void {
    this.loadingOffers = true;
    this.api
      .getWalletOffers()
      .pipe(finalize(() => (this.loadingOffers = false)))
      .subscribe({
        next: o => {
          this.offers = o;
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 3000, position: 'bottom' });
          await t.present();
        },
      });
  }

  rupees(paise: number): string {
    return (paise / 100).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  }

  /** One-line summary e.g. "₹9 · 20 min talk" */
  offerSummary(o: WalletOffer): string {
    return `₹${this.rupees(o.amountPaise)} · ${o.talkMinutes} min talk`;
  }

  offerSummaryFromPending(): string {
    const p = this.pending;
    if (!p) {
      return '';
    }
    return `₹${this.rupees(p.amountPaise)} · ${p.talkMinutes} min talk`;
  }

  pickOffer(offer: WalletOffer): void {
    if (this.busy) {
      return;
    }
    this.busy = true;
    this.api.initiateWalletTopup(offer.id).subscribe({
      next: res => {
        this.pending = res;
        this.busy = false;
      },
      error: async e => {
        this.busy = false;
        const t = await this.toast.create({ message: e.message, duration: 4200, position: 'bottom' });
        await t.present();
      },
    });
  }

  cancelPending(): void {
    this.pending = null;
  }

  /** Opens GPay / PhonePe / Paytm chooser on mobile (NPCI UPI intent). */
  openUpiApp(): void {
    const url = this.pending?.upiDeepLink;
    if (!url) {
      return;
    }
    try {
      window.location.href = url;
    } catch {
      void this.toast.create({ message: 'Could not open UPI. Try on your phone.', duration: 3000, position: 'bottom' }).then(t => t.present());
    }
  }

  async completeMockPay(): Promise<void> {
    const p = this.pending;
    if (!p || p.paymentProvider.toUpperCase() !== 'MOCK') {
      return;
    }
    this.busy = true;
    this.api.completeMockWalletTopup(p.sessionId).subscribe({
      next: async () => {
        this.busy = false;
        const t = await this.toast.create({
          message: `Added ₹${this.rupees(p.amountPaise)} to wallet`,
          duration: 2200,
          position: 'bottom',
        });
        await t.present();
        await this.router.navigate(['/tabs/wallet'], { replaceUrl: false });
      },
      error: async e => {
        this.busy = false;
        const t = await this.toast.create({ message: e.message, duration: 3500, position: 'bottom' });
        await t.present();
      },
    });
  }

  async completeUpiPaid(): Promise<void> {
    const p = this.pending;
    if (!p || !p.upiConfirmAllowed) {
      return;
    }
    this.busy = true;
    this.api.completeUpiWalletTopup(p.sessionId).subscribe({
      next: async () => {
        this.busy = false;
        const t = await this.toast.create({
          message: `Wallet updated · ₹${this.rupees(p.amountPaise)} added`,
          duration: 2200,
          position: 'bottom',
        });
        await t.present();
        await this.router.navigate(['/tabs/wallet'], { replaceUrl: false });
      },
      error: async e => {
        this.busy = false;
        const t = await this.toast.create({ message: e.message, duration: 3500, position: 'bottom' });
        await t.present();
      },
    });
  }
}
