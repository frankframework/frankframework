import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationSummaryComponent } from './configuration-summary.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('ConfigurationSummaryComponent', () => {
  let component: ConfigurationSummaryComponent;
  let fixture: ComponentFixture<ConfigurationSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ConfigurationSummaryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigurationSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
