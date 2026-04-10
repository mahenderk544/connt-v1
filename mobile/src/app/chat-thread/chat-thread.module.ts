import { IonicModule } from '@ionic/angular';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatThreadPage } from './chat-thread.page';
import { ChatThreadPageRoutingModule } from './chat-thread-routing.module';

@NgModule({
  imports: [IonicModule, CommonModule, FormsModule, ChatThreadPageRoutingModule],
  declarations: [ChatThreadPage],
})
export class ChatThreadPageModule {}
