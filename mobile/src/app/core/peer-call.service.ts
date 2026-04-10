import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { SessionService } from './session.service';

export type CallKind = 'voice' | 'video';

export interface IncomingCallEvent {
  kind: CallKind;
  fromUserId: string;
}

function signalingWsUrl(apiBase: string, token: string): string {
  const u = new URL(apiBase);
  const wsProto = u.protocol === 'https:' ? 'wss:' : 'ws:';
  const pathPrefix = u.pathname.replace(/\/$/, '');
  const path = `${pathPrefix}/ws/call`.replace(/\/\//g, '/');
  return `${wsProto}//${u.host}${path}?token=${encodeURIComponent(token)}`;
}

@Injectable({ providedIn: 'root' })
export class PeerCallService {
  /** Friend is ringing you (same chat thread open). */
  readonly incomingCall$ = new Subject<IncomingCallEvent>();
  /** WebRTC media from the peer (attach to <video>.srcObject). */
  readonly remoteStream$ = new Subject<MediaStream | null>();
  /** ICE + signaling connected. */
  readonly callConnected$ = new Subject<void>();
  /** Peer hung up or connection failed. */
  readonly callEnded$ = new Subject<void>();

  private ws: WebSocket | null = null;
  private activePeerId: string | null = null;
  private pc: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private isCaller = false;
  private iceBuffer: RTCIceCandidateInit[] = [];

  private readonly rtcConfig: RTCConfiguration = {
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
  };

  constructor(
    private session: SessionService,
    private zone: NgZone,
  ) {}

  /** Open WebSocket and join the 1:1 signaling room with this friend. */
  connectSignaling(peerId: string): void {
    const token = this.session.token;
    if (!token || !peerId) {
      return;
    }
    if (this.activePeerId === peerId && this.ws?.readyState === WebSocket.OPEN) {
      return;
    }
    this.disconnectSignaling();
    this.activePeerId = peerId;
    const url = signalingWsUrl(environment.apiBaseUrl, token);
    this.ws = new WebSocket(url);
    this.ws.onopen = () => {
      this.ws?.send(JSON.stringify({ type: 'join', peerUserId: peerId }));
    };
    this.ws.onmessage = ev => {
      try {
        const data = JSON.parse(ev.data as string) as { fromUserId: string; signal: Record<string, unknown> };
        void this.handleSignal(data);
      } catch {
        /* ignore */
      }
    };
    this.ws.onerror = () => {
      this.zone.run(() => this.callEnded$.next());
    };
    this.ws.onclose = () => {
      this.zone.run(() => this.callEnded$.next());
    };
  }

  disconnectSignaling(): void {
    if (this.ws) {
      this.ws.onclose = null;
      this.ws.close();
      this.ws = null;
    }
    this.activePeerId = null;
  }

  /** Caller: request media, signal peer, wait for accept, then send offer. */
  async startOutgoing(peerId: string, kind: CallKind): Promise<void> {
    this.hangupLocal();
    this.isCaller = true;
    this.connectSignaling(peerId);
    await this.waitWsOpen();
    this.localStream = await this.getMedia(kind);
    this.createPeerConnection();
    for (const t of this.localStream.getTracks()) {
      this.pc!.addTrack(t, this.localStream);
    }
    this.sendSignal({ type: 'call-start', callKind: kind });
  }

  /** Callee: after user taps Accept. */
  async acceptIncoming(peerId: string, kind: CallKind): Promise<void> {
    this.isCaller = false;
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || this.activePeerId !== peerId) {
      this.connectSignaling(peerId);
      await this.waitWsOpen();
    }
    this.localStream = await this.getMedia(kind);
    if (!this.pc) {
      this.createPeerConnection();
    }
    for (const t of this.localStream.getTracks()) {
      this.pc!.addTrack(t, this.localStream);
    }
    this.sendSignal({ type: 'call-accepted' });
  }

  rejectIncoming(): void {
    this.sendSignal({ type: 'call-reject' });
    this.hangupLocal();
  }

  hangup(): void {
    this.sendSignal({ type: 'hangup' });
    this.hangupLocal();
  }

  hangupLocal(): void {
    this.iceBuffer = [];
    if (this.pc) {
      this.pc.ontrack = null;
      this.pc.onicecandidate = null;
      this.pc.oniceconnectionstatechange = null;
      this.pc.close();
      this.pc = null;
    }
    if (this.localStream) {
      for (const t of this.localStream.getTracks()) {
        t.stop();
      }
      this.localStream = null;
    }
    this.remoteStream$.next(null);
    this.isCaller = false;
  }

  private sendSignal(payload: Record<string, unknown>): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload));
    }
  }

  private waitWsOpen(): Promise<void> {
    return new Promise((resolve, reject) => {
      const w = this.ws;
      if (!w) {
        reject(new Error('No signaling connection'));
        return;
      }
      if (w.readyState === WebSocket.OPEN) {
        resolve();
        return;
      }
      const t = window.setTimeout(() => reject(new Error('Signaling timeout')), 10000);
      w.addEventListener(
        'open',
        () => {
          clearTimeout(t);
          resolve();
        },
        { once: true },
      );
      w.addEventListener(
        'error',
        () => {
          clearTimeout(t);
          reject(new Error('Signaling failed'));
        },
        { once: true },
      );
    });
  }

  private async getMedia(kind: CallKind): Promise<MediaStream> {
    const video = kind === 'video';
    return navigator.mediaDevices.getUserMedia({
      audio: true,
      video: video ? { facingMode: 'user' } : false,
    });
  }

  private createPeerConnection(): void {
    this.pc = new RTCPeerConnection(this.rtcConfig);
    this.pc.onicecandidate = e => {
      if (e.candidate) {
        this.sendSignal({ type: 'ice', candidate: e.candidate.toJSON() });
      }
    };
    this.pc.ontrack = e => {
      const [stream] = e.streams;
      this.zone.run(() => this.remoteStream$.next(stream ?? null));
    };
    this.pc.oniceconnectionstatechange = () => {
      const st = this.pc?.iceConnectionState;
      if (st === 'connected' || st === 'completed') {
        this.zone.run(() => this.callConnected$.next());
      }
      if (st === 'failed' || st === 'closed') {
        this.zone.run(() => this.callEnded$.next());
      }
    };
  }

  private async handleSignal(data: { fromUserId: string; signal: Record<string, unknown> }): Promise<void> {
    if (!this.activePeerId || data.fromUserId !== this.activePeerId) {
      return;
    }
    const s = data.signal;
    const type = s['type'] as string;

    if (type === 'call-start') {
      const k = (s['callKind'] as CallKind) === 'video' ? 'video' : 'voice';
      this.zone.run(() => this.incomingCall$.next({ kind: k, fromUserId: data.fromUserId }));
      return;
    }
    if (type === 'call-reject' || type === 'hangup') {
      this.zone.run(() => {
        this.hangupLocal();
        this.callEnded$.next();
      });
      return;
    }

    if (type === 'call-accepted' && this.isCaller && this.pc) {
      const offer = await this.pc.createOffer();
      await this.pc.setLocalDescription(offer);
      this.sendSignal({ type: offer.type, sdp: offer.sdp });
      await this.flushIce();
      return;
    }

    if (type === 'offer' && this.pc) {
      await this.pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'offer', sdp: s['sdp'] as string }),
      );
      await this.flushIce();
      const answer = await this.pc.createAnswer();
      await this.pc.setLocalDescription(answer);
      this.sendSignal({ type: answer.type, sdp: answer.sdp });
      await this.flushIce();
      return;
    }

    if (type === 'answer' && this.pc) {
      await this.pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp: s['sdp'] as string }),
      );
      await this.flushIce();
      return;
    }

    if (type === 'ice' && s['candidate'] && this.pc) {
      const cand = s['candidate'] as RTCIceCandidateInit;
      if (!this.pc.remoteDescription) {
        this.iceBuffer.push(cand);
      } else {
        try {
          await this.pc.addIceCandidate(cand);
        } catch {
          /* ignore */
        }
      }
    }
  }

  private async flushIce(): Promise<void> {
    if (!this.pc?.remoteDescription) {
      return;
    }
    const pending = [...this.iceBuffer];
    this.iceBuffer = [];
    for (const c of pending) {
      try {
        await this.pc.addIceCandidate(c);
      } catch {
        /* ignore */
      }
    }
  }
}
