var app = angular.module("myApp", ['ngSanitize']);

app.factory('dataService', ['$http', function($http) {
  function parseJson() {
    return $http.get('../../rest/ibisdoc/ibisdoc.json').then(function(data) {
      return data.data;
    })
  }

  return {
    getData : parseJson
  };
}]);

app.factory('classesService', function() {
  var classes = [];

  return  {
    getClasses: function () {
      return classes;
    },
    setClasses: function(classArray) {
      classes = classArray;
    }
  };
});

app.factory('methodsService', function() {
  var methods = [];

  return {
    getMethods: function() {
      return methods;
    },
    setMethods: function(methodArray) {
      methods = methodArray;
    }
  };
});

app.controller("foldersCtrl", function($scope, dataService, classesService, methodsService, $rootScope, $filter) {
  dataService.getData().then(function(response) {
    $scope.folders = response;
    $scope.methods = [];

    angular.forEach($scope.folders, function(folder) {
      angular.forEach(folder.classes, function(clas) {
        angular.forEach(clas.methods, function(method) {
          $scope.methods.push(method);
        })
      })
    });

    $rootScope.$broadcast('givingAllMethhods', $scope.methods);
  });

  $scope.showClasses = function(folderName) {
    angular.forEach($scope.folders, function(folder) {
      if (angular.equals(folder.name, folderName)) {
        classesService.setClasses(folder.classes);
      }
    });
  }

  $scope.onKey = function($event) {
    $rootScope.$broadcast('givingAllMethhods', $scope.methods.filter(function (method) {
      return (method.name.toLowerCase() == $event.target.value.toLowerCase());
    }));
    $rootScope.$broadcast('searching');
  }
});

app.controller("classesCtrl", function($scope, classesService, methodsService) {
  $scope.$watch(classesService.getClasses, function(change) {
    $scope.classes = change;
  }.bind(this));

  $scope.showMethods = function(className) {
    angular.forEach($scope.classes, function(clas) {
      if (angular.equals(clas.name, className)) {
        methodsService.setMethods(clas.methods);
      }
    });
  }
});

app.controller("methodsCtrl", function($scope, methodsService) {
  $scope.$watch(methodsService.getMethods, function(change) {
    $scope.methods = change;
    $scope.searching = false;
  }.bind(this));


  $scope.$on('givingAllMethhods', function(event, allMethods) {
    console.log(allMethods.length);
    $scope.allMethods = allMethods;
  })

  $scope.$on('searching', function() {
    $scope.searching = true;
  })
});
