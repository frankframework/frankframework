import { Component, OnInit } from '@angular/core';
import { File } from 'src/angularjs/app/app.service';
import { ApiService } from 'src/angularjs/app/services/api.service';

interface Form {
    destination: string
    replyTo: string
    message: string
    persistent: boolean
    propertyValue: string
    propertyKey: string
    type: string
    connectionFactory: string
    synchronous: boolean
    lookupDestination: boolean
    encoding: string
}

@Component({
    selector: 'app-jms-send-message',
    templateUrl: './jms-send-message.component.html',
    styleUrls: ['./jms-send-message.component.scss']
})
export class JmsSendMessageComponent implements OnInit {
    destinationTypes: string[] = ["QUEUE", "TOPIC"];
    processing: boolean = false;
    file: File = {
        name: ""
    };
    connectionFactories: string[] = [];
    error: string = "";
    form: Form = {
        destination: "",
        replyTo: "",
        message: "",
        persistent: false,
        propertyValue: "",
        propertyKey: "",
        type: "",
        connectionFactory: "",
        synchronous: false,
        lookupDestination: false,
        encoding: ""
    };

    constructor(
        private apiService: ApiService
    ) { };

    ngOnInit(): void {
        this.apiService.Get("jms", (data) => {
            this.connectionFactories = data["connectionFactories"];
            angular.element("select[name='type']").val(this.destinationTypes[0]);
        });
    };

    submit(formData: Form) {
        this.processing = true;
        if (!formData) return;

        var fd = new FormData();
        if (formData.connectionFactory && formData.connectionFactory != "")
            fd.append("connectionFactory", formData.connectionFactory);
        else
            fd.append("connectionFactory", this.connectionFactories[0]);
        if (formData.destination && formData.destination != "")
            fd.append("destination", formData.destination);
        if (formData.type && formData.type != "")
            fd.append("type", formData.type);
        else
            fd.append("type", this.destinationTypes[0]);
        if (formData.replyTo && formData.replyTo != "")
            fd.append("replyTo", formData.replyTo);
        if (formData.persistent)
            fd.append("persistent", formData.persistent.toString());
        if (formData.synchronous)
            fd.append("synchronous", formData.synchronous.toString());
        if (formData.lookupDestination)
            fd.append("lookupDestination", formData.lookupDestination.toString());

        if (formData.propertyKey && formData.propertyKey != "" && formData.propertyValue && formData.propertyValue != "")
            fd.append("property", formData.propertyKey + "," + formData.propertyValue);
        if (formData.message && formData.message != "") {
            var encoding = (formData.encoding && formData.encoding != "") ? ";charset=" + formData.encoding : "";
            fd.append("message", new Blob([formData.message], { type: "text/plain" + encoding }), 'message');
        }
        if (this.file)
            fd.append("file", this.file as any, this.file['name']);
        if (formData.encoding && formData.encoding != "")
            fd.append("encoding", formData.encoding);

        if (!formData.message && !this.file) {
            this.error = "Please specify a file or message!";
            this.processing = false;
            return;
        }

        this.apiService.Post("jms/message", fd, (returnData) => {
            this.error = "";
            this.processing = false;
        }, (errorData, status, errorMsg) => {
            this.processing = false;
            errorMsg = (errorMsg) ? errorMsg : "An unknown error occured, check the logs for more info.";
            this.error = (errorData.error) ? errorData.error : errorMsg;
        });
    };

    reset() {
        this.error = "";
        if (!this.form) return;
        if (this.form["destination"])
            this.form["destination"] = "";
        if (this.form["replyTo"])
            this.form["replyTo"] = "";
        if (this.form["message"])
            this.form["message"] = "";
        if (this.form["persistent"])
            this.form["persistent"] = false;
        if (this.form["propertyValue"])
            this.form["propertyValue"] = "";
        if (this.form["propertyKey"])
            this.form["propertyKey"] = "";
        if (this.form["type"])
            this.form["type"] = this.destinationTypes[0];
    };
}
