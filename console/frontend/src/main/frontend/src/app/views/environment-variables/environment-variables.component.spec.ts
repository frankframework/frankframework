import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvironmentVariablesComponent } from './environment-variables.component';

describe('EnvironmentVariablesComponent', () => {
  let component: EnvironmentVariablesComponent;
  let fixture: ComponentFixture<EnvironmentVariablesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvironmentVariablesComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EnvironmentVariablesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
