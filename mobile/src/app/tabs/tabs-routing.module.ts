import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TabsPage } from './tabs.page';

const routes: Routes = [
  {
    path: '',
    component: TabsPage,
    children: [
      {
        path: 'connect',
        loadChildren: () => import('../connect/connect.module').then(m => m.ConnectPageModule),
      },
      {
        path: 'profile',
        loadChildren: () =>
          import('../profile-tab/profile-tab.module').then(m => m.ProfileTabPageModule),
      },
      {
        path: 'wallet',
        loadChildren: () => import('../wallet/wallet.module').then(m => m.WalletPageModule),
      },
      {
        path: 'chat/:peerId',
        loadChildren: () =>
          import('../chat-thread/chat-thread.module').then(m => m.ChatThreadPageModule),
      },
      {
        path: '',
        redirectTo: 'connect',
        pathMatch: 'full',
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
})
export class TabsPageRoutingModule {}
