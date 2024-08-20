import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationSummaryComponent } from './configuration-summary.component';

describe('ConfigurationSummaryComponent', () => {
  let component: ConfigurationSummaryComponent;
  let fixture: ComponentFixture<ConfigurationSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ConfigurationSummaryComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ConfigurationSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
