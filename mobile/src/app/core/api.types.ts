export type OtpPurpose = 'REGISTER' | 'LOGIN';

export interface OtpRequestResponse {
  message: string;
  devCode?: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
}

export interface Profile {
  userId: string;
  displayName: string;
  bio: string | null;
  photoUrl: string | null;
  tags: string | null;
  /** Relative path e.g. /api/v1/media/voice/{userId} */
  voiceIntroUrl?: string | null;
  communicationTone?: string | null;
  behavioursSummary?: string | null;
  expectingFor?: string | null;
}

export interface FriendSummary {
  userId: string;
  displayName: string;
}

export interface ConnectionRequestRow {
  id: string;
  fromUserId: string;
  toUserId: string;
  status: string;
  fromDisplayName: string;
}

export interface DirectMessage {
  id: string;
  fromUserId: string;
  toUserId: string;
  body: string;
  createdAt: string;
}

/** Wallet ledger row (top-up, charge, refund). */
export interface WalletTransaction {
  id: string;
  type: string;
  amountPaise: number;
  label: string;
  createdAt: string;
}

/** Configurable top-up pack from DB (`wallet_topup_offers`). */
export interface WalletOffer {
  id: string;
  amountPaise: number;
  talkMinutes: number;
  label: string | null;
  sortOrder: number;
}

export interface WalletBalance {
  balancePaise: number;
}

export interface TopupInitiateResponse {
  sessionId: string;
  paymentProvider: string;
  amountPaise: number;
  talkMinutes: number;
  offerLabel: string | null;
  razorpayOrderId: string | null;
  clientMessage: string;
  /** NPCI upi://pay link when paymentProvider is UPI */
  upiDeepLink?: string | null;
  /** When true, show "I've paid" to credit wallet (MVP; use webhooks in production). */
  upiConfirmAllowed?: boolean;
}
