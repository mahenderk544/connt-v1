import { Component } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { ConntoApiService } from '../core/connto-api.service';

@Component({
  selector: 'app-wallet',
  templateUrl: './wallet.page.html',
  styleUrls: ['./wallet.page.scss'],
  standalone: false,
})
export class WalletPage {
  balancePaise = 0;
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
      .getWalletBalance()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: b => {
          this.balancePaise = b.balancePaise;
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 2800, position: 'bottom' });
          await t.present();
        },
      });
  }

  rupees(paise: number): string {
    return (paise / 100).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  }
}
