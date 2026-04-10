import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { ConntoApiService } from '../core/connto-api.service';
import { FriendSummary } from '../core/api.types';

@Component({
  selector: 'app-chats',
  templateUrl: './chats.page.html',
  styleUrls: ['./chats.page.scss'],
  standalone: false,
})
export class ChatsPage {
  friends: FriendSummary[] = [];
  loading = false;

  constructor(
    private api: ConntoApiService,
    private router: Router,
    private toast: ToastController,
  ) {}

  ionViewWillEnter(): void {
    this.load();
  }

  load(ev?: { target: { complete: () => Promise<void> } }): void {
    this.loading = true;
    this.api
      .listFriends()
      .pipe(
        finalize(async () => {
          this.loading = false;
          if (ev?.target) {
            await ev.target.complete();
          }
        }),
      )
      .subscribe({
        next: f => {
          this.friends = f;
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await t.present();
        },
      });
  }

  openThread(f: FriendSummary): void {
    void this.router.navigate(['/tabs/chat', f.userId], {
      queryParams: { name: f.displayName },
    });
  }
}
