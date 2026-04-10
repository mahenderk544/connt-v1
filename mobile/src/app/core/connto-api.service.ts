import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  AuthResponse,
  ConnectionRequestRow,
  DirectMessage,
  FriendSummary,
  OtpPurpose,
  OtpRequestResponse,
  Profile,
  TopupInitiateResponse,
  WalletBalance,
  WalletOffer,
  WalletTransaction,
} from './api.types';

function unwrapError(err: unknown): Observable<never> {
  const body = (err as { error?: { message?: string } })?.error;
  const msg =
    typeof body?.message === 'string'
      ? body.message
      : (err as { message?: string })?.message || 'Request failed';
  return throwError(() => new Error(msg));
}

@Injectable({ providedIn: 'root' })
export class ConntoApiService {
  constructor(private http: HttpClient) {}

  requestOtp(phone: string, purpose: OtpPurpose): Observable<OtpRequestResponse> {
    return this.http
      .post<OtpRequestResponse>('/api/v1/auth/otp/request', { phone, purpose })
      .pipe(catchError(unwrapError));
  }

  register(phone: string, otp: string, displayName?: string, password?: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/register', { phone, otp, displayName, password })
      .pipe(catchError(unwrapError));
  }

  login(phone: string, otp: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', { phone, otp })
      .pipe(catchError(unwrapError));
  }

  getMyProfile(): Observable<Profile> {
    return this.http.get<Profile>('/api/v1/me/profile').pipe(catchError(unwrapError));
  }

  updateProfile(patch: {
    displayName?: string;
    bio?: string;
    tags?: string;
    communicationTone?: string;
    behavioursSummary?: string;
    expectingFor?: string;
  }): Observable<Profile> {
    return this.http.patch<Profile>('/api/v1/me/profile', patch).pipe(catchError(unwrapError));
  }

  searchUsers(q: string): Observable<Profile[]> {
    const params = new HttpParams().set('q', q);
    return this.http.get<Profile[]>('/api/v1/users/search', { params }).pipe(catchError(unwrapError));
  }

  listFriends(): Observable<FriendSummary[]> {
    return this.http.get<FriendSummary[]>('/api/v1/connections/friends').pipe(catchError(unwrapError));
  }

  sendConnectionRequest(toUserId: string): Observable<ConnectionRequestRow> {
    return this.http
      .post<ConnectionRequestRow>('/api/v1/connections/requests', { toUserId })
      .pipe(catchError(unwrapError));
  }

  incomingRequests(): Observable<ConnectionRequestRow[]> {
    return this.http
      .get<ConnectionRequestRow[]>('/api/v1/connections/requests/incoming')
      .pipe(catchError(unwrapError));
  }

  outgoingRequests(): Observable<ConnectionRequestRow[]> {
    return this.http
      .get<ConnectionRequestRow[]>('/api/v1/connections/requests/outgoing')
      .pipe(catchError(unwrapError));
  }

  acceptRequest(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/connections/requests/${id}/accept`, {}).pipe(catchError(unwrapError));
  }

  declineRequest(id: string): Observable<void> {
    return this.http.post<void>(`/api/v1/connections/requests/${id}/decline`, {}).pipe(catchError(unwrapError));
  }

  messageThread(peerId: string): Observable<DirectMessage[]> {
    return this.http
      .get<DirectMessage[]>(`/api/v1/messages/thread/${peerId}`)
      .pipe(catchError(unwrapError));
  }

  sendMessage(toUserId: string, body: string): Observable<DirectMessage> {
    return this.http
      .post<DirectMessage>('/api/v1/messages', { toUserId, body })
      .pipe(catchError(unwrapError));
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>('/api/v1/me/account').pipe(catchError(unwrapError));
  }

  walletTransactions(): Observable<WalletTransaction[]> {
    return this.http
      .get<WalletTransaction[]>('/api/v1/me/wallet/transactions')
      .pipe(catchError(unwrapError));
  }

  getWalletOffers(): Observable<WalletOffer[]> {
    return this.http.get<WalletOffer[]>('/api/v1/wallet/offers').pipe(catchError(unwrapError));
  }

  getWalletBalance(): Observable<WalletBalance> {
    return this.http.get<WalletBalance>('/api/v1/me/wallet').pipe(catchError(unwrapError));
  }

  initiateWalletTopup(offerId: string): Observable<TopupInitiateResponse> {
    return this.http
      .post<TopupInitiateResponse>('/api/v1/me/wallet/topup/initiate', { offerId })
      .pipe(catchError(unwrapError));
  }

  completeMockWalletTopup(sessionId: string): Observable<WalletBalance> {
    return this.http
      .post<WalletBalance>('/api/v1/me/wallet/topup/complete-mock', { sessionId })
      .pipe(catchError(unwrapError));
  }

  completeUpiWalletTopup(sessionId: string): Observable<WalletBalance> {
    return this.http
      .post<WalletBalance>('/api/v1/me/wallet/topup/complete-upi', { sessionId })
      .pipe(catchError(unwrapError));
  }

  uploadVoiceIntro(file: Blob): Observable<Profile> {
    const fd = new FormData();
    fd.append('file', file, 'intro.webm');
    return this.http
      .post<Profile>('/api/v1/me/profile/voice-intro', fd)
      .pipe(catchError(unwrapError));
  }

  deleteVoiceIntro(): Observable<Profile> {
    return this.http.delete<Profile>('/api/v1/me/profile/voice-intro').pipe(catchError(unwrapError));
  }
}
