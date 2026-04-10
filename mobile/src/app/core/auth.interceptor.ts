import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SessionService } from './session.service';
import { environment } from '../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private session: SessionService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.session.token;
    const url = req.url.startsWith('http')
      ? req.url
      : `${environment.apiBaseUrl}${req.url.startsWith('/') ? '' : '/'}${req.url}`;

    let r = req.clone({ url });
    if (token) {
      r = r.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next.handle(r);
  }
}
