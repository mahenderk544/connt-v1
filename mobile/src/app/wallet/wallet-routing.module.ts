import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { WalletAddMoneyPage } from './wallet-add-money.page';
import { WalletPage } from './wallet.page';

const routes: Routes = [
  {
    path: '',
    component: WalletPage,
  },
  {
    path: 'add',
    component: WalletAddMoneyPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class WalletPageRoutingModule {}
