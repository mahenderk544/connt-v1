import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { TransactionsPage } from './transactions.page';
import { TransactionsPageRoutingModule } from './transactions-routing.module';

@NgModule({
  imports: [CommonModule, IonicModule, TransactionsPageRoutingModule],
  declarations: [TransactionsPage],
})
export class TransactionsPageModule {}
