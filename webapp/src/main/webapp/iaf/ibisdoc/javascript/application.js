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
    var parents = [];

    return {
        getMethods: function() {
            return methods;
        },
        setMethods: function(methodArray) {
            methods = methodArray;
        },
        getParents: function () {
            return parents;
        },
        setParents: function(parentArray) {
            parents = parentArray;
        }
    };
});

app.controller("foldersCtrl", function($scope, dataService, classesService, methodsService, $rootScope) {
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
            var resetFolders = angular.element(document.querySelector('#' + folder.name));
            resetFolders[0].style.color = "black";

        });
        var oneFolder = angular.element( document.querySelector('#' + folderName));
        oneFolder[0].style.color = "blue";

        angular.forEach($scope.folders, function(folder) {
            if (angular.equals(folder.name, folderName)) {
                classesService.setClasses(folder.classes);
            }
        });

        $rootScope.$broadcast('folderNameBar', folderName);
    };
});

app.controller("classesCtrl", function($scope, classesService, methodsService) {
    $scope.$watch(classesService.getClasses, function(change) {
        $scope.classes = change;
        $scope.searching = false;
    }.bind(this));

    $scope.onKey = function($event) {
        $scope.searching = true;
    };

    $scope.$on('folderNameBar', function(event, folderName) {
        $scope.folderName = folderName;
    });

    $scope.showMethods = function(className) {
        angular.forEach($scope.classes, function(clas) {
            if (angular.equals(clas.name, className)) {
                $scope.parentClasses = [];

                angular.forEach(clas.methods, function(method) {

                    if (method.className !== method.originalClassName) {
                        let index = $scope.parentClasses.findIndex(parent => parent.name === method.originalClassName);
                        var parentIndex = 0;

                        // For each of the parentClasses
                        angular.forEach(method.superClasses, function(parentClass) {

                            // Check if we are dealing with the right parentClass
                            if (parentClass.substring(0, parentClass.length - 1) === method.originalClassName) {

                                // Get the priority index
                                var str = parentClass.substring(parentClass.length - 1, parentClass.length);
                                parentIndex = parseInt(str, 10);
                            }
                        });

                        if (index === -1) {
                            $scope.parentClasses.push({
                                name : method.originalClassName,
                                attributes : [method],
                                parentIndex : parentIndex
                            });
                        } else {
                            $scope.parentClasses[index].attributes.push(method);
                        }
                    }
                });
                methodsService.setMethods(clas.methods);
                methodsService.setParents($scope.parentClasses);
            }
        });
    }
});

app.controller("methodsCtrl", function($scope, methodsService) {
    $scope.$watch(methodsService.getMethods, function(change) {
        $scope.methods = change;
        $scope.searching = false;
    }.bind(this));

    $scope.$watch(methodsService.getParents, function(change) {
        $scope.parents = change;
    }.bind(this));


    $scope.$on('givingAllMethhods', function(event, allMethods) {
        $scope.allMethods = allMethods;
    });

    $scope.onKey = function($event) {
        $scope.searching = true;
    };
});
