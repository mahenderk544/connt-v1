import { IonicModule } from '@ionic/angular';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ConnectPage } from './connect.page';
import { ConnectPageRoutingModule } from './connect-routing.module';

@NgModule({
  imports: [IonicModule, CommonModule, FormsModule, RouterModule, ConnectPageRoutingModule],
  declarations: [ConnectPage],
})
export class ConnectPageModule {}
