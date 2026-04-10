import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AlertController, ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ConntoApiService } from '../core/connto-api.service';
import { SessionService } from '../core/session.service';
import { Profile } from '../core/api.types';

@Component({
  selector: 'app-profile-tab',
  templateUrl: './profile-tab.page.html',
  styleUrls: ['./profile-tab.page.scss'],
  standalone: false,
})
export class ProfileTabPage {
  readonly appVersion = environment.appVersion;

  profile: Profile | null = null;
  loading = false;
  editing = false;
  deleting = false;
  displayName = '';
  bio = '';
  tags = '';
  communicationTone = '';
  behavioursSummary = '';
  expectingFor = '';

  recording = false;
  recordElapsedSec = 0;
  voiceUploading = false;

  private mediaRecorder: MediaRecorder | null = null;
  private recordChunks: BlobPart[] = [];
  private mediaStream: MediaStream | null = null;
  private tickTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private api: ConntoApiService,
    private session: SessionService,
    private router: Router,
    private toast: ToastController,
    private alert: AlertController,
  ) {}

  ionViewWillEnter(): void {
    this.load();
  }

  ionViewWillLeave(): void {
    this.abortRecording();
  }

  absoluteVoiceUrl(): string | null {
    const path = this.profile?.voiceIntroUrl;
    if (!path) {
      return null;
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    return `${environment.apiBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
  }

  load(): void {
    this.loading = true;
    this.api
      .getMyProfile()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: p => {
          this.profile = p;
          this.displayName = p.displayName;
          this.bio = p.bio ?? '';
          this.tags = p.tags ?? '';
          this.communicationTone = p.communicationTone ?? '';
          this.behavioursSummary = p.behavioursSummary ?? '';
          this.expectingFor = p.expectingFor ?? '';
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await t.present();
        },
      });
  }

  startEdit(): void {
    this.editing = true;
  }

  cancelEdit(): void {
    this.abortRecording();
    this.editing = false;
    if (this.profile) {
      this.displayName = this.profile.displayName;
      this.bio = this.profile.bio ?? '';
      this.tags = this.profile.tags ?? '';
      this.communicationTone = this.profile.communicationTone ?? '';
      this.behavioursSummary = this.profile.behavioursSummary ?? '';
      this.expectingFor = this.profile.expectingFor ?? '';
    }
  }

  save(): void {
    this.api
      .updateProfile({
        displayName: this.displayName.trim() || undefined,
        bio: this.bio,
        tags: this.tags,
        communicationTone: this.communicationTone,
        behavioursSummary: this.behavioursSummary,
        expectingFor: this.expectingFor,
      })
      .subscribe({
        next: p => {
          this.profile = p;
          this.editing = false;
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await t.present();
        },
      });
  }

  async startVoiceRecord(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      const t = await this.toast.create({
        message: 'Recording is not available on this device or browser.',
        duration: 2800,
        position: 'bottom',
      });
      await t.present();
      return;
    }
    try {
      this.recordChunks = [];
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaStream = stream;
      const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';
      const mr = new MediaRecorder(stream, { mimeType: mime });
      this.mediaRecorder = mr;
      mr.ondataavailable = ev => {
        if (ev.data.size > 0) {
          this.recordChunks.push(ev.data);
        }
      };
      mr.start(200);
      this.recording = true;
      this.recordElapsedSec = 0;
      this.tickTimer = setInterval(() => {
        this.recordElapsedSec += 1;
        if (this.recordElapsedSec >= 30) {
          void this.stopVoiceRecord();
        }
      }, 1000);
    } catch {
      const t = await this.toast.create({
        message: 'Could not access the microphone.',
        duration: 2800,
        position: 'bottom',
      });
      await t.present();
    }
  }

  async stopVoiceRecord(): Promise<void> {
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
    const mr = this.mediaRecorder;
    if (!mr || mr.state === 'inactive') {
      this.cleanupMediaTracks();
      this.recording = false;
      return;
    }
    await new Promise<void>(resolve => {
      mr.onstop = () => {
        resolve();
      };
      try {
        mr.stop();
      } catch {
        resolve();
      }
    });
    const mime = mr.mimeType || 'audio/webm';
    const blob = new Blob(this.recordChunks, { type: mime });
    this.recordChunks = [];
    this.mediaRecorder = null;
    this.cleanupMediaTracks();
    this.recording = false;
    await this.uploadVoiceBlob(blob);
  }

  private cleanupMediaTracks(): void {
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(t => t.stop());
    }
    this.mediaStream = null;
  }

  abortRecording(): void {
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.onstop = null;
      try {
        this.mediaRecorder.stop();
      } catch {
        /* ignore */
      }
    }
    this.mediaRecorder = null;
    this.cleanupMediaTracks();
    this.recordChunks = [];
    this.recording = false;
  }

  private async uploadVoiceBlob(blob: Blob): Promise<void> {
    if (blob.size < 80) {
      const t = await this.toast.create({
        message: 'Recording too short. Try again.',
        duration: 2200,
        position: 'bottom',
      });
      await t.present();
      return;
    }
    this.voiceUploading = true;
    this.api.uploadVoiceIntro(blob).subscribe({
      next: p => {
        this.profile = p;
      },
      error: async e => {
        const t = await this.toast.create({ message: e.message, duration: 3200, position: 'bottom' });
        await t.present();
      },
      complete: () => {
        this.voiceUploading = false;
      },
    });
  }

  removeVoiceIntro(): void {
    this.voiceUploading = true;
    this.api.deleteVoiceIntro().subscribe({
      next: p => {
        this.profile = p;
      },
      error: async e => {
        const t = await this.toast.create({ message: e.message, duration: 2800, position: 'bottom' });
        await t.present();
      },
      complete: () => {
        this.voiceUploading = false;
      },
    });
  }

  async openHelp(): Promise<void> {
    const a = await this.alert.create({
      header: 'Help & support',
      message:
        'Need help using Connto? Email support or check our help center when it goes live. For now, use Connect to find friends and Profile to manage your account.',
      buttons: ['OK'],
    });
    await a.present();
  }

  async openPrivacy(): Promise<void> {
    const a = await this.alert.create({
      header: 'Privacy',
      message:
        'We only use your phone number and profile to run the app. A full privacy policy will be published before public launch.',
      buttons: ['OK'],
    });
    await a.present();
  }

  async openNotifications(): Promise<void> {
    const t = await this.toast.create({
      message: 'Notification settings will be available in a future update.',
      duration: 2600,
      position: 'bottom',
    });
    await t.present();
  }

  async sendFeedback(): Promise<void> {
    const t = await this.toast.create({
      message: 'Thanks for your interest. In-app feedback is coming soon.',
      duration: 2600,
      position: 'bottom',
    });
    await t.present();
  }

  async openAbout(): Promise<void> {
    const a = await this.alert.create({
      header: 'About Connto',
      message: `Version ${this.appVersion}\n\nConnect with people you trust. Experts and friends in one calm place.`,
      buttons: ['OK'],
    });
    await a.present();
  }

  async confirmDeleteAccount(): Promise<void> {
    const a = await this.alert.create({
      header: 'Delete account?',
      message:
        'This permanently deletes your account, profile, messages, and connections. You cannot undo this.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          role: 'destructive',
          handler: () => {
            void this.secondConfirmDelete();
          },
        },
      ],
    });
    await a.present();
  }

  private async secondConfirmDelete(): Promise<void> {
    const a = await this.alert.create({
      header: 'Are you sure?',
      message: 'Your account and all related data will be removed from the server immediately.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete my account',
          role: 'destructive',
          handler: () => {
            void this.performDeleteAccount();
          },
        },
      ],
    });
    await a.present();
  }

  private async performDeleteAccount(): Promise<void> {
    if (this.deleting) {
      return;
    }
    this.deleting = true;
    this.api.deleteAccount().subscribe({
      next: async () => {
        this.session.clear();
        const t = await this.toast.create({ message: 'Account deleted', duration: 1800, position: 'bottom' });
        await t.present();
        await this.router.navigate(['/login'], { replaceUrl: true });
        this.deleting = false;
      },
      error: async e => {
        this.deleting = false;
        const t = await this.toast.create({ message: e.message, duration: 3200, position: 'bottom' });
        await t.present();
      },
    });
  }

  async logout(): Promise<void> {
    this.session.clear();
    const t = await this.toast.create({ message: 'Signed out', duration: 1500, position: 'bottom' });
    await t.present();
    await this.router.navigate(['/login'], { replaceUrl: true });
  }
}
