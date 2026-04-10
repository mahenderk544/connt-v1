import { IonicModule } from '@ionic/angular';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatsPage } from './chats.page';
import { ChatsPageRoutingModule } from './chats-routing.module';

@NgModule({
  imports: [IonicModule, CommonModule, FormsModule, ChatsPageRoutingModule],
  declarations: [ChatsPage],
})
export class ChatsPageModule {}
