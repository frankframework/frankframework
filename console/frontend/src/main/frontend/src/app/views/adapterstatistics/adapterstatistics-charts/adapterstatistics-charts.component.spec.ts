import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdapterstatisticsChartsComponent } from './adapterstatistics-charts.component';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

describe('AdapterstatisticsChartsComponent', () => {
  let component: AdapterstatisticsChartsComponent;
  let fixture: ComponentFixture<AdapterstatisticsChartsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdapterstatisticsChartsComponent],
      providers: [provideCharts(withDefaultRegisterables())],
    }).compileComponents();

    fixture = TestBed.createComponent(AdapterstatisticsChartsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
