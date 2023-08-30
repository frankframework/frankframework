import { appModule } from "../app.module";
import { Adapter } from "../app.service";

const configurationFilter = function () {
  return function (adapters: Record<string, Adapter>, context: any) {
    if (!adapters || Object.keys(adapters).length < 1) return {};
    var r: Record<string, Adapter> = {};
    for (const adapterName in adapters) {
      var adapter = adapters[adapterName];

      if ((adapter.configuration == context.selectedConfiguration || context.selectedConfiguration == "All") && (context.filter == undefined || context.filter[adapter.status!]))
        r[adapterName] = adapter;
    }
    return r;
  };
}

export type ConfigurationFilter = ReturnType<typeof configurationFilter>;

appModule.filter('configurationFilter', configurationFilter);
