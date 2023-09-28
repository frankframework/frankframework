import { ApiService } from "src/angularjs/app/services/api.service";
import { CookiesService } from "src/angularjs/app/services/cookies.service";

class JmsBrowseQueueController {
    destinationTypes = ["QUEUE", "TOPIC"];
    form: Record<string, any> = {};
    messages = [];
    numberOfMessages = -1;
    processing = false;
    error = "";
    connectionFactories = [];

    constructor(
        private Api: ApiService,
        private Cookies: CookiesService
    ) { };

    $onInit() {
        var browseJmsQueue = this.Cookies.get("browseJmsQueue");
        if (browseJmsQueue) this.form = browseJmsQueue;

        this.Api.Get("jms", (data) => {
            $.extend(this, data);
            angular.element("select[name='type']").val(this.destinationTypes[0]);
        });
    };

    submit(formData: any) {
        this.processing = true;

        if (!formData || !formData.destination) {
            this.error = "Please specify a connection factory and destination!";
            return;
        };

        this.Cookies.set("browseJmsQueue", formData);
        if (!formData.connectionFactory) formData.connectionFactory = this.connectionFactories[0] || false;
        if (!formData.type) formData.type = this.destinationTypes[0] || false;

        this.Api.Post("jms/browse", JSON.stringify(formData), (data) => {
            $.extend(this, data);
            if (!data.messages) this.messages = [];
            this.error = "";
            this.processing = false;
        }, (errorData, status, errorMsg) => {
            this.error = (errorData && errorData.error) ? errorData.error : errorMsg;
            this.processing = false;
        });
    };

    reset() {
        console.log(this.form)
        this.error = "";
        if (!this.form) return;
        if (this.form["destination"]) this.form["destination"] = "";
        if (this.form["rowNumbersOnly"]) this.form["rowNumbersOnly"] = "";
        if (this.form["payload"]) this.form["payload"] = "";
        if (this.form["lookupDestination"]) this.form["lookupDestination"] = "";
        if (this.form["type"]) this.form["type"] = this.destinationTypes[0];

        this.messages = [];
        this.numberOfMessages = -1;
        this.processing = false;
    };
}