import { appModule } from "../../app.module";

const ConnectionsController = function ($scope, Api) {
    const ctrl = this;

    ctrl.dtOptions = {
        processing: true,
        lengthMenu: [50, 100, 250, 500],
        columns: [
            { "data": "adapterName", bSortable: false },
            { "data": "componentName", bSortable: false },
            { "data": "domain", bSortable: false },
            { "data": "destination", bSortable: false },
            { "data": "direction", bSortable: false }
        ],
        sAjaxDataProp: 'data',
        ajax: function (data, callback, settings) {
            Api.Get("connections", function (response) {
                response.draw = data.draw;
                response.recordsTotal = response.data.length;
                response.recordsFiltered = response.data.length;
                callback(response);
            });
        },
        initComplete: function () {
            this.api().columns([2, 4]).every(function () {
                var column = this;
                var select = $('<select><option value=""></option></select>')
                    .appendTo($(column.header()))
                    .on('change', function () {
                        var val = $.fn.dataTable.util.escapeRegex(
                            $(this).val()
                        );
                        column.search(val ? '^' + val + '$' : '', true, false).draw();
                    });

                column.data().unique().sort().each(function (d, j) {
                    select.append('<option value="' + d + '">' + d + '</option>')
                });
            });
            this.api().columns([0, 1, 3]).every(function () {
                var column = this;
                $('<input type="text" style="display:block; font-size:12px" placeholder="Search..." />')
                    .appendTo($(column.header()))
                    .on('keyup change clear', function () {
                        if (column.search() !== this.value) {
                            column.search(this.value).draw();
                        }
                    });
            });
        }
    };
};

appModule.component('connections', {
    controller: ['$scope', 'Api', ConnectionsController],
    templateUrl: 'js/app/views/connections/connections.component.html'
});
