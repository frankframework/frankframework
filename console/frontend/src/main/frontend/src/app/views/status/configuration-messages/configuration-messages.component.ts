import { Component, Input } from '@angular/core';
import { AdapterMessage, MessageLog } from '../../../app.service';

@Component({
  selector: 'app-configuration-messages',
  templateUrl: './configuration-messages.component.html',
  styleUrl: './configuration-messages.component.scss',
})
export class ConfigurationMessagesComponent {
  @Input({ required: true }) messageLog: Record<string, MessageLog> = {};
  @Input({ required: true }) selectedConfiguration: string = '';
  protected msgBoxExpanded = false;

  protected getMessageLog(selectedConfiguration: string): AdapterMessage[] {
    return this.messageLog[selectedConfiguration]?.messages ?? [];
  }
}
