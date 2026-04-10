import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProfileTabPage } from './profile-tab.page';

const routes: Routes = [
  {
    path: '',
    component: ProfileTabPage,
  },
  {
    path: 'transactions',
    loadChildren: () =>
      import('./transactions/transactions.module').then(m => m.TransactionsPageModule),
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ProfileTabPageRoutingModule {}
