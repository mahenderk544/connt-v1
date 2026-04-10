import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AlertController, ToastController } from '@ionic/angular';
import { ConntoApiService } from '../../core/connto-api.service';
import { SessionService } from '../../core/session.service';
import { OtpPurpose } from '../../core/api.types';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
  standalone: false,
})
export class LoginPage implements OnInit {
  mode: 'LOGIN' | 'REGISTER' = 'LOGIN';
  phone = '';
  otp = '';
  displayName = '';
  otpSent = false;
  busy = false;

  constructor(
    private api: ConntoApiService,
    private session: SessionService,
    private router: Router,
    private toast: ToastController,
    private alert: AlertController,
  ) {}

  ngOnInit(): void {
    if (this.session.isLoggedIn()) {
      void this.router.navigate(['/tabs/connect'], { replaceUrl: true });
    }
  }

  purpose(): OtpPurpose {
    return this.mode === 'REGISTER' ? 'REGISTER' : 'LOGIN';
  }

  async requestOtp(): Promise<void> {
    const p = this.phone.trim();
    if (!p) {
      await this.showToast('Enter your phone number');
      return;
    }
    this.busy = true;
    this.api.requestOtp(p, this.purpose()).subscribe({
      next: async res => {
        this.otpSent = true;
        let msg = res.message;
        if (res.devCode) {
          msg += ` (dev code: ${res.devCode})`;
          await this.alert.create({
            header: 'Development OTP',
            message: `Your code is ${res.devCode}`,
            buttons: ['OK'],
          }).then(a => a.present());
        }
        await this.showToast(msg);
      },
      error: async e => {
        await this.showToast(e.message || 'Could not send code');
      },
      complete: () => {
        this.busy = false;
      },
    });
  }

  async submit(): Promise<void> {
    const p = this.phone.trim();
    const code = this.otp.trim();
    if (!p || !/^\d{6}$/.test(code)) {
      await this.showToast('Enter phone and 6-digit code');
      return;
    }
    this.busy = true;
    if (this.mode === 'REGISTER') {
      this.api.register(p, code, this.displayName.trim() || undefined).subscribe({
        next: async r => this.finishAuth(r.token, r.userId),
        error: async e => await this.showToast(e.message || 'Register failed'),
        complete: () => {
          this.busy = false;
        },
      });
    } else {
      this.api.login(p, code).subscribe({
        next: async r => this.finishAuth(r.token, r.userId),
        error: async e => await this.showToast(e.message || 'Login failed'),
        complete: () => {
          this.busy = false;
        },
      });
    }
  }

  private async finishAuth(token: string, userId: string): Promise<void> {
    this.session.setSession(token, userId);
    await this.showToast('Welcome!');
    await this.router.navigate(['/tabs/connect'], { replaceUrl: true });
  }

  private async showToast(message: string): Promise<void> {
    const t = await this.toast.create({ message, duration: 2800, position: 'bottom' });
    await t.present();
  }
}
