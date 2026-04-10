import { Injectable } from '@angular/core';

const TOKEN_KEY = 'connto_token';
const USER_ID_KEY = 'connto_user_id';

@Injectable({ providedIn: 'root' })
export class SessionService {
  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  get userId(): string | null {
    return localStorage.getItem(USER_ID_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.token;
  }

  setSession(token: string, userId: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_ID_KEY, userId);
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
  }
}
