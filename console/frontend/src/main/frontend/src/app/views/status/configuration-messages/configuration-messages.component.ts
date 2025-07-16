import { Component, Input } from '@angular/core';
import { ConfigurationMessage, MessageLog } from '../../../app.service';
import { NgClass } from '@angular/common';
import { ToDateDirective } from '../../../components/to-date.directive';

@Component({
  selector: 'app-configuration-messages',
  imports: [NgClass, ToDateDirective],
  templateUrl: './configuration-messages.component.html',
  styleUrl: './configuration-messages.component.scss',
})
export class ConfigurationMessagesComponent {
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration: string = '';
  protected msgBoxExpanded = false;

  protected getMessageLog(selectedConfiguration: string): ConfigurationMessage[] {
    return this.messageLog[selectedConfiguration]?.messages ?? [];
  }
}
