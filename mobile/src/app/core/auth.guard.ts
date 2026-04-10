import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { SessionService } from './session.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(
    private session: SessionService,
    private router: Router,
  ) {}

  canActivate(): boolean | UrlTree {
    if (this.session.isLoggedIn()) {
      return true;
    }
    return this.router.createUrlTree(['/login']);
  }
}
