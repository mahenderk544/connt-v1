import { Component } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { WalletTransaction } from '../../core/api.types';
import { ConntoApiService } from '../../core/connto-api.service';

@Component({
  selector: 'app-transactions',
  templateUrl: './transactions.page.html',
  styleUrls: ['./transactions.page.scss'],
  standalone: false,
})
export class TransactionsPage {
  items: WalletTransaction[] = [];
  loading = false;

  constructor(
    private api: ConntoApiService,
    private toast: ToastController,
  ) {}

  ionViewWillEnter(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.api
      .walletTransactions()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: rows => {
          this.items = rows;
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 2800, position: 'bottom' });
          await t.present();
        },
      });
  }

  /** Positive = credit to wallet, negative = charge. */
  signedRupees(tx: WalletTransaction): string {
    const rupees = tx.amountPaise / 100;
    const abs = Math.abs(rupees).toFixed(2);
    const sign = tx.amountPaise >= 0 ? '+' : '−';
    return `${sign}₹${abs}`;
  }

  formatWhen(iso: string): string {
    try {
      const d = new Date(iso);
      return d.toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  }
}
