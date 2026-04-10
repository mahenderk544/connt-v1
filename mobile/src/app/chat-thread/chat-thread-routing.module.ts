import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ChatThreadPage } from './chat-thread.page';

const routes: Routes = [
  {
    path: '',
    component: ChatThreadPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ChatThreadPageRoutingModule {}
