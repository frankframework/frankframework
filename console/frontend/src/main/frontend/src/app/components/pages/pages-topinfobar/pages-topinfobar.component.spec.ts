import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PagesTopinfobarComponent } from './pages-topinfobar.component';

describe('PagesTopinfobarComponent', () => {
  let component: PagesTopinfobarComponent;
  let fixture: ComponentFixture<PagesTopinfobarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, PagesTopinfobarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PagesTopinfobarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
