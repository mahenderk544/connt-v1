import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { RouterModule } from '@angular/router';
import { WalletAddMoneyPage } from './wallet-add-money.page';
import { WalletPage } from './wallet.page';
import { WalletPageRoutingModule } from './wallet-routing.module';

@NgModule({
  imports: [CommonModule, IonicModule, RouterModule, WalletPageRoutingModule],
  declarations: [WalletPage, WalletAddMoneyPage],
})
export class WalletPageModule {}
