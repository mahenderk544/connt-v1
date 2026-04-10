import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { finalize } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ConntoApiService } from '../core/connto-api.service';
import { ConnectionRequestRow, FriendSummary, Profile } from '../core/api.types';
import { SessionService } from '../core/session.service';

export type ExpertCategory = 'all' | 'star' | 'new' | 'happiness' | 'relationship';

/** When set, "Request" sends POST connection request to this app user (e.g. seed UUIDs in dev). */
export interface ExpertCard {
  id: string;
  name: string;
  emoji: string;
  age: number;
  title: string;
  rating: number;
  ratePerMin: number;
  languages: string;
  online: boolean;
  starRibbon: boolean;
  avatarGradient: [string, string];
  categories: ExpertCategory[];
  linkedUserId?: string;
  tone?: string;
  behaviours?: string;
  expectingFor?: string;
  voiceIntroUrl?: string | null;
}

@Component({
  selector: 'app-connect',
  templateUrl: './connect.page.html',
  styleUrls: ['./connect.page.scss'],
  standalone: false,
})
export class ConnectPage {
  mainView: 'experts' | 'friends' = 'experts';
  activeCategory: ExpertCategory = 'all';

  walletBalancePaise = 0;

  readonly categoryChips: { id: ExpertCategory; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'star', label: 'Star' },
    { id: 'new', label: 'New' },
    { id: 'happiness', label: 'Happiness' },
    { id: 'relationship', label: 'Relationship' },
  ];

  readonly experts: ExpertCard[] = [
    {
      id: '1',
      name: 'Priya',
      emoji: '🐱',
      age: 23,
      title: 'Relationship Expert',
      rating: 4.3,
      ratePerMin: 5,
      languages: 'Hindi · Bengali',
      online: true,
      starRibbon: false,
      avatarGradient: ['#e4e4e7', '#d4d4d8'],
      categories: ['all', 'relationship', 'new'],
      linkedUserId: '10000000-0000-4000-8000-000000000001',
      tone: 'Warm, patient, non-judgmental',
      behaviours: 'Listens first; asks gentle follow-ups; keeps boundaries clear.',
      expectingFor: 'Honesty about what you want from the conversation.',
    },
    {
      id: '2',
      name: 'Rashi',
      emoji: '✨',
      age: 26,
      title: 'Life Coach',
      rating: 4.8,
      ratePerMin: 8,
      languages: 'English · Hindi',
      online: true,
      starRibbon: true,
      avatarGradient: ['#f4f4f5', '#e4e4e7'],
      categories: ['all', 'star', 'happiness'],
      linkedUserId: '10000000-0000-4000-8000-000000000002',
      tone: 'Upbeat and grounding',
      behaviours: 'Short reflections, small action steps, checks in on how you feel.',
      expectingFor: 'Openness to try one tiny change between sessions.',
    },
    {
      id: '3',
      name: 'Ananya',
      emoji: '💜',
      age: 24,
      title: 'Happiness Guide',
      rating: 4.5,
      ratePerMin: 6,
      languages: 'Tamil · English',
      online: true,
      starRibbon: false,
      avatarGradient: ['#d4d4d8', '#c4c4c8'],
      categories: ['all', 'happiness', 'new'],
      tone: 'Soft and encouraging',
      behaviours: 'Validates feelings; suggests mindfulness in plain language.',
      expectingFor: 'A calm space — no rush to “fix” everything at once.',
    },
    {
      id: '4',
      name: 'Karan',
      emoji: '🎧',
      age: 28,
      title: 'Relationship Expert',
      rating: 4.2,
      ratePerMin: 7,
      languages: 'Hindi · Punjabi',
      online: false,
      starRibbon: false,
      avatarGradient: ['#e4e4e7', '#c4c4c8'],
      categories: ['all', 'relationship'],
      tone: 'Direct but kind',
      behaviours: 'Names patterns clearly; respects your pace.',
      expectingFor: 'Willingness to look at both sides of a situation.',
    },
    {
      id: '5',
      name: 'Meera',
      emoji: '🌸',
      age: 25,
      title: 'Wellness & Chat',
      rating: 4.6,
      ratePerMin: 5,
      languages: 'Marathi · Hindi',
      online: true,
      starRibbon: true,
      avatarGradient: ['#fafafa', '#e4e4e7'],
      categories: ['all', 'star', 'happiness', 'new'],
      tone: 'Friendly and steady',
      behaviours: 'Light humour when it helps; keeps focus on your goals.',
      expectingFor: 'What “better” would look like for you this week.',
    },
  ];

  query = '';
  results: Profile[] = [];
  incoming: ConnectionRequestRow[] = [];
  friends: FriendSummary[] = [];
  searching = false;
  loadingIncoming = false;
  loadingFriends = false;

  private friendIdSet = new Set<string>();

  /** Users you have already sent a pending connection request to (from server + optimistic updates). */
  private pendingOutgoingToIds = new Set<string>();

  constructor(
    private api: ConntoApiService,
    private toast: ToastController,
    private session: SessionService,
    private router: Router,
  ) {}

  get profileInitial(): string {
    const id = this.session.userId;
    if (id && id.length > 0) {
      return id.replace(/\D/g, '').slice(-1) || id.charAt(0).toUpperCase();
    }
    return 'U';
  }

  get filteredExperts(): ExpertCard[] {
    if (this.activeCategory === 'all') {
      return this.experts;
    }
    return this.experts.filter(e => e.categories.includes(this.activeCategory));
  }

  ionViewWillEnter(): void {
    this.loadIncoming();
    this.loadFriends();
    this.loadOutgoing();
    this.loadWallet();
  }

  /** Whole rupees for the top bar (paise rounded away for a clean pill). */
  walletBalanceDisplay(): string {
    return Math.floor(this.walletBalancePaise / 100).toLocaleString();
  }

  private loadWallet(): void {
    this.api.getWalletBalance().subscribe({
      next: b => {
        this.walletBalancePaise = b.balancePaise;
      },
      error: () => {
        this.walletBalancePaise = 0;
      },
    });
  }

  absoluteMediaUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }
    return `${environment.apiBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`;
  }

  isFriend(userId: string): boolean {
    return this.friendIdSet.has(userId);
  }

  hasPendingOutgoing(userId: string): boolean {
    return this.pendingOutgoingToIds.has(userId);
  }

  /** Search row: show Request only when not friends and no pending outgoing request. */
  canShowRequestForUser(userId: string): boolean {
    return !this.isFriend(userId) && !this.hasPendingOutgoing(userId);
  }

  expertShowMessage(e: ExpertCard): boolean {
    const id = e.linkedUserId;
    return !!id && this.isFriend(id);
  }

  expertShowPending(e: ExpertCard): boolean {
    const id = e.linkedUserId;
    return !!id && !this.isFriend(id) && this.hasPendingOutgoing(id);
  }

  expertShowRequest(e: ExpertCard): boolean {
    const id = e.linkedUserId;
    if (!id) {
      return true;
    }
    if (this.isFriend(id)) {
      return false;
    }
    return !this.hasPendingOutgoing(id);
  }

  openMessageWith(userId: string, displayName: string): void {
    void this.router.navigate(['/tabs/chat', userId], {
      queryParams: { name: displayName },
    });
  }

  onSearchResultNameClick(u: Profile, ev: Event): void {
    if (!this.isFriend(u.userId)) {
      return;
    }
    ev.stopPropagation();
    this.openMessageWith(u.userId, u.displayName);
  }

  setCategory(c: ExpertCategory): void {
    this.activeCategory = c;
  }

  async callExpert(expert: ExpertCard): Promise<void> {
    const t = await this.toast.create({
      message: `${expert.name} is a sample listing. Use Friends to call people you know.`,
      duration: 3200,
      position: 'bottom',
    });
    await t.present();
  }

  friendRequestExpert(expert: ExpertCard): void {
    const selfId = this.session.userId;
    if (!expert.linkedUserId) {
      void this.toast
        .create({
          message: `Open Friends and search to connect with ${expert.name}.`,
          duration: 3200,
          position: 'bottom',
        })
        .then(t => t.present());
      return;
    }
    if (selfId && expert.linkedUserId === selfId) {
      void this.toast
        .create({ message: 'That is your own account.', duration: 2200, position: 'bottom' })
        .then(t => t.present());
      return;
    }
    if (this.hasPendingOutgoing(expert.linkedUserId)) {
      void this.toast
        .create({
          message: `You already sent a request to ${expert.name}.`,
          duration: 2200,
          position: 'bottom',
        })
        .then(t => t.present());
      return;
    }
    this.api.sendConnectionRequest(expert.linkedUserId).subscribe({
      next: async () => {
        this.pendingOutgoingToIds = new Set([...this.pendingOutgoingToIds, expert.linkedUserId!]);
        const toast = await this.toast.create({
          message: `Friend request sent to ${expert.name}.`,
          duration: 2200,
          position: 'bottom',
        });
        await toast.present();
      },
      error: async e => {
        const toast = await this.toast.create({
          message: e.message,
          duration: 2800,
          position: 'bottom',
        });
        await toast.present();
      },
    });
  }

  search(): void {
    const q = this.query.trim();
    if (!q) {
      this.results = [];
      return;
    }
    this.searching = true;
    this.api
      .searchUsers(q)
      .pipe(finalize(() => (this.searching = false)))
      .subscribe({
        next: r => {
          this.results = r;
        },
        error: async e => {
          const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await toast.present();
        },
      });
  }

  loadIncoming(): void {
    this.loadingIncoming = true;
    this.api
      .incomingRequests()
      .pipe(finalize(() => (this.loadingIncoming = false)))
      .subscribe({
        next: r => {
          this.incoming = r;
        },
        error: async e => {
          const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await toast.present();
        },
      });
  }

  loadOutgoing(): void {
    this.api.outgoingRequests().subscribe({
      next: rows => {
        const pending = rows.filter(r => r.status === 'PENDING').map(r => r.toUserId);
        this.pendingOutgoingToIds = new Set(pending);
      },
      error: async e => {
        const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
        await toast.present();
      },
    });
  }

  loadFriends(): void {
    this.loadingFriends = true;
    this.api
      .listFriends()
      .pipe(finalize(() => (this.loadingFriends = false)))
      .subscribe({
        next: list => {
          this.friends = list;
          this.friendIdSet = new Set(list.map(f => f.userId));
          for (const id of this.friendIdSet) {
            this.pendingOutgoingToIds.delete(id);
          }
        },
        error: async e => {
          const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
          await toast.present();
        },
      });
  }

  connectTo(user: Profile): void {
    this.api.sendConnectionRequest(user.userId).subscribe({
      next: async () => {
        this.pendingOutgoingToIds = new Set([...this.pendingOutgoingToIds, user.userId]);
        const toast = await this.toast.create({
          message: `Request sent to ${user.displayName}`,
          duration: 2200,
          position: 'bottom',
        });
        await toast.present();
      },
      error: async e => {
        const toast = await this.toast.create({ message: e.message, duration: 2800, position: 'bottom' });
        await toast.present();
      },
    });
  }

  accept(id: string): void {
    this.api.acceptRequest(id).subscribe({
      next: () => {
        this.loadIncoming();
        this.loadFriends();
        this.loadOutgoing();
      },
      error: async e => {
        const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
        await toast.present();
      },
    });
  }

  decline(id: string): void {
    this.api.declineRequest(id).subscribe({
      next: () => this.loadIncoming(),
      error: async e => {
        const toast = await this.toast.create({ message: e.message, duration: 2500, position: 'bottom' });
        await toast.present();
      },
    });
  }
}
