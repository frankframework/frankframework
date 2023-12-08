import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AppConstants, AppService } from 'src/app/app.service';
import { SweetalertService } from 'src/app/services/sweetalert.service';
import { ToastService } from 'src/app/services/toast.service';

type FeedbackForm = {
  rating: number,
  name: string,
  feedback: string
};

@Component({
  selector: 'app-feedback-modal',
  templateUrl: './feedback-modal.component.html',
  styleUrls: ['./feedback-modal.component.scss']
})
export class FeedbackModalComponent implements OnInit {
  @Input() rating: number = 0;

  form: FeedbackForm = { rating: this.rating, name: "", feedback: "" };

  private appConstants: AppConstants;

  constructor(
    private http: HttpClient,
    private activeModal: NgbActiveModal,
    private appService: AppService,
    private sweetalertService: SweetalertService,
    private toastService: ToastService
  ){
    this.appConstants = this.appService.APP_CONSTANTS;
    this.appService.appConstants$.subscribe(() => {
      this.appConstants = this.appService.APP_CONSTANTS;
    })
  }

  ngOnInit() {
    window.setTimeout(() => {
      while (this.rating >= 0) {
        this.setRate(this.rating);
        this.rating--;
      }
    }, 150);
  }

  setRating(ev: MouseEvent, i: number) {
    this.resetRating();
    this.form.rating = i;
    let j = i;
    while (j >= 0) {
      this.setRate(j);
      j--;
    }
  }

  submit(form: FeedbackForm) {
    form.rating++;
    this.http.post<{result: string}>(this.appConstants["console.feedbackURL"], form).subscribe({ next: (response) => {
      if (response['result'] == "ok")
        this.toastService.success("Thank you for sending us feedback!");
      else
        this.sweetalertService.Error("Oops, something went wrong...", "Please try again later!");
    }, error: () => {
      this.sweetalertService.Error("Oops, something went wrong...", "Please try again later!");
    }});
    this.activeModal.close();
  }

  close() {
    this.activeModal.close();
  }

  private setRate(i: number) {
    $(".rating i.rating" + i).removeClass("fa-star-o");
    $(".rating i.rating" + i).addClass("fa-star");
  }
  private resetRating() {
    $(".rating i").each((i, e) => {
      $(e).addClass("fa-star-o").removeClass("fa-star");
    });
  }
}
