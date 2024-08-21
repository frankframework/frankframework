import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationMessagesComponent } from './configuration-messages.component';

describe('ConfigurationMessagesComponent', () => {
  let component: ConfigurationMessagesComponent;
  let fixture: ComponentFixture<ConfigurationMessagesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfigurationMessagesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigurationMessagesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
