import { Component, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonContent, ToastController } from '@ionic/angular';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ConntoApiService } from '../core/connto-api.service';
import { PeerCallService } from '../core/peer-call.service';
import { SessionService } from '../core/session.service';
import { DirectMessage } from '../core/api.types';

@Component({
  selector: 'app-chat-thread',
  templateUrl: './chat-thread.page.html',
  styleUrls: ['./chat-thread.page.scss'],
  standalone: false,
})
export class ChatThreadPage {
  @ViewChild(IonContent) content?: IonContent;
  @ViewChild('remoteVideo') remoteVideo?: ElementRef<HTMLVideoElement>;

  peerId = '';
  peerName = 'Chat';
  myId: string | null = null;
  messages: DirectMessage[] = [];
  draft = '';
  loading = false;
  callOpen = false;
  callKind: 'voice' | 'video' = 'voice';
  /** Friend started a call; show Accept / Decline. */
  incomingRing = false;
  callConnected = false;
  private callTimer: ReturnType<typeof setInterval> | null = null;
  callSeconds = 0;

  private peerSub: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private api: ConntoApiService,
    private session: SessionService,
    private toast: ToastController,
    private peerCall: PeerCallService,
  ) {}

  ionViewWillEnter(): void {
    this.myId = this.session.userId;
    this.peerId = this.route.snapshot.paramMap.get('peerId') ?? '';
    this.peerName = this.route.snapshot.queryParamMap.get('name') || 'Chat';
    this.peerSub?.unsubscribe();
    this.peerSub = new Subscription();
    this.peerCall.connectSignaling(this.peerId);
    this.peerSub.add(
      this.peerCall.incomingCall$.subscribe(e => {
        if (e.fromUserId !== this.peerId) {
          return;
        }
        this.incomingRing = true;
        this.callKind = e.kind;
        this.callOpen = true;
        this.callConnected = false;
      }),
    );
    this.peerSub.add(
      this.peerCall.callConnected$.subscribe(() => {
        this.callConnected = true;
        this.incomingRing = false;
        if (this.callTimer) {
          clearInterval(this.callTimer);
        }
        this.callTimer = setInterval(() => {
          this.callSeconds += 1;
        }, 1000);
      }),
    );
    this.peerSub.add(this.peerCall.callEnded$.subscribe(() => this.resetCallUi()));
    this.peerSub.add(
      this.peerCall.remoteStream$.subscribe(stream => {
        setTimeout(() => {
          const el = this.remoteVideo?.nativeElement;
          if (el) {
            el.srcObject = stream;
          }
        }, 0);
      }),
    );
    this.load();
  }

  ionViewWillLeave(): void {
    this.peerSub?.unsubscribe();
    this.peerSub = null;
    this.peerCall.hangup();
    this.peerCall.disconnectSignaling();
    this.resetCallUi();
  }

  load(): void {
    if (!this.peerId) {
      return;
    }
    this.loading = true;
    this.api
      .messageThread(this.peerId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: m => {
          this.messages = m;
          setTimeout(() => void this.content?.scrollToBottom(300), 50);
        },
        error: async e => {
          const t = await this.toast.create({ message: e.message, duration: 3000, position: 'bottom' });
          await t.present();
        },
      });
  }

  send(): void {
    const body = this.draft.trim();
    if (!body || !this.peerId) {
      return;
    }
    this.api.sendMessage(this.peerId, body).subscribe({
      next: msg => {
        this.messages = [...this.messages, msg];
        this.draft = '';
        setTimeout(() => void this.content?.scrollToBottom(300), 50);
      },
      error: async e => {
        const t = await this.toast.create({ message: e.message, duration: 2800, position: 'bottom' });
        await t.present();
      },
    });
  }

  isMine(m: DirectMessage): boolean {
    return m.fromUserId === this.myId;
  }

  openCall(kind: 'voice' | 'video'): void {
    this.incomingRing = false;
    this.callKind = kind;
    this.callOpen = true;
    this.callConnected = false;
    this.callSeconds = 0;
    void this.peerCall.startOutgoing(this.peerId, kind).catch(async err => {
      const msg = err instanceof Error ? err.message : 'Call failed';
      const t = await this.toast.create({ message: msg, duration: 2800, position: 'bottom' });
      await t.present();
      this.resetCallUi();
    });
  }

  acceptIncomingCall(): void {
    this.incomingRing = false;
    void this.peerCall.acceptIncoming(this.peerId, this.callKind).catch(async err => {
      const msg = err instanceof Error ? err.message : 'Could not answer';
      const t = await this.toast.create({ message: msg, duration: 2800, position: 'bottom' });
      await t.present();
      this.resetCallUi();
    });
  }

  declineIncomingCall(): void {
    this.peerCall.rejectIncoming();
    this.resetCallUi();
  }

  endCallUi(): void {
    this.peerCall.hangup();
    this.resetCallUi();
  }

  private resetCallUi(): void {
    if (this.callTimer) {
      clearInterval(this.callTimer);
      this.callTimer = null;
    }
    this.callOpen = false;
    this.callConnected = false;
    this.incomingRing = false;
    this.callSeconds = 0;
    setTimeout(() => {
      const el = this.remoteVideo?.nativeElement;
      if (el) {
        el.srcObject = null;
      }
    }, 0);
  }

  formatDuration(total: number): string {
    const m = Math.floor(total / 60);
    const s = total % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }
}
