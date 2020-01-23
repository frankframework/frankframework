var app = angular.module("myApp", ['ngSanitize']);

// Get the json file with all the data in the form of an array of folders
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

// Getter and setter for classes
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

// Getter and setter for methods  (attributes)
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
        },
    };
});

// The folders div
app.controller("foldersCtrl", function($scope, dataService, classesService, methodsService, $rootScope) {
    dataService.getData().then(function(response) {
        $scope.folders = response;
        $scope.methods = [];
        $scope.allFolder = {};
        var index = 0;

        // Put all the methods in a methods array (is put here so it will only be done once)
        angular.forEach($scope.folders, function(folder) {
            if (folder.name === "All") {
                index = $scope.folders.indexOf(folder);
                $scope.allFolder = folder;
            }

            angular.forEach(folder.classes, function(clas) {
                angular.forEach(clas.methods, function(method) {
                    method.className = clas.name;
                    method.folderName = folder.name;
                    $scope.methods.push(method);
                });
            })
        });

        $scope.folders.splice(index, 1);

        angular.forEach($scope.folders,  function (folder) {
            angular.forEach(folder.classes, function (clas) {
                $scope.allFolder.classes.push(clas);
            });
        });

        $scope.folders.push($scope.allFolder);

        // The searchbar for all methods (attributes)
        $scope.onKey = function($event) {
            $rootScope.$broadcast('givingAllMethhods', $scope.methods.filter(function (method) {
                return (method.name.toLowerCase() === $event.target.value.toLowerCase());
            }));

            // Notify the methodsCrtl that we are searching accross all methods (attributes)
            $rootScope.$broadcast('searching');
        }
    });

    // Show all the classes that fall under the folderName that is clicked
    $scope.showClasses = function(folderName) {
        angular.forEach($scope.folders, function(folder) {

            // Give all the folders a black color (so that blue folders are reset to black)
            var resetFolders = angular.element(document.querySelector('#' + folder.name));
            resetFolders[0].style.color = "black";

        });

        // Give the selected folder a blue color
        var oneFolder = angular.element( document.querySelector('#' + folderName));
        oneFolder[0].style.color = "blue";

        // Get all the classes of the folder
        angular.forEach($scope.folders, function(folder) {
            if (angular.equals(folder.name, folderName)) {
                classesService.setClasses(folder.classes);
            }
        });

        // Notify the classesCtrl in which folder we currently are
        $rootScope.$broadcast('folderNameBar', folderName);
    };
});

// The classes div
app.controller("classesCtrl", function($scope, $rootScope, classesService, methodsService) {
    // When we change the list of classes, change to the new list
    $scope.$watch(classesService.getClasses, function(change) {
        $scope.classes = change;
    }.bind(this));


    // Receive the notification of the foldersCtrl in which folder we currently are
    $scope.$on('folderNameBar', function(event, folderName) {
        $scope.folderName = folderName;
    });

    // When clicking a class show the methods (attributes) page
    $scope.showMethods = function(className) {
        angular.forEach($scope.classes, function(clas) {
            // Get the right class
            if (angular.equals(clas.name, className)) {
            	var parentIndex = 0;
                $scope.potentialParents = [];
                $scope.parentClasses = [];
                angular.forEach(clas.superClasses, function(superClass) {
                	$scope.potentialParents.push({
                		name : superClass,
                		attributes : [],
                		parentIndex : parentIndex
                	});
                	
                	parentIndex = parentIndex + 1
                });
                // Notify the methodsCtrl in which package we currently are
                $rootScope.$broadcast('packageName', clas.packageName);
                $rootScope.$broadcast('javadocLink', clas.javadocLink);
                
                // Check for each method what its original class is
                angular.forEach(clas.methods, function(method) {

                	// If it is not part of the current class (meaning part of a parent class)
                    if (method.className !== method.originalClassName) {
                        let index = $scope.potentialParents.findIndex(parent => parent.name === method.originalClassName);
                        if (index !== -1) {
                            $scope.potentialParents[index].attributes.push(method);
                        } else {
                            $scope.potentialParents.push({
                                name : method.originalClassName,
                                attributes : [method],
                                parentIndex : parentIndex
                            });

                            parentIndex = parentIndex + 1
                        }
                    }
                });

                // Set the methods (attributes) and its order of parent classes
                methodsService.setMethods(clas.methods);
                
                // Check if the potential parents have attributes, if not delete them
                angular.forEach($scope.potentialParents, function(parent) {
                	if (parent.attributes.length > 0 ) {
                		$scope.parentClasses.push(parent);
                	}
                });
                
                methodsService.setParents($scope.parentClasses);
            }
        });
    }
});

// The methods (attribute) div
app.controller("methodsCtrl", function($scope, methodsService) {
    // If there is a change in class, then change the current page
    $scope.$watch(methodsService.getMethods, function(change) {
        $scope.methods = change;
        $scope.searching = false;
    }.bind(this));

    // Get the parents in the order of abstraction
    $scope.$watch(methodsService.getParents, function(change) {
        $scope.parents = change;
    }.bind(this));

    // Receive the notification that we are currently searching accross all methods (attributes)
    $scope.$on('givingAllMethhods', function(event, allMethods) {
        $scope.searching = true;
        $scope.allMethods = allMethods;
    });

    // The current class' package name in which the methods (attributes) reside
    $scope.$on('packageName', function(event, packageName) {
        $scope.packageName = packageName;
    });
    
    $scope.$on('javadocLink', function(event, javadocLink) {
    	$scope.javadocLink = javadocLink;
    })

    // When searching within the class, keep note
    $scope.onKey = function($event) {
        $scope.searching = true;
    };
})
// The directive for sorting each table according to alphabet of the name property or ordernumber (property of a method)
    .directive("sort", function() {
        return {
            restrict: 'A',
            transclude: true,
            template :
                '<p ng-click="onClick()">'+
                '<span ng-transclude></span>'+
                '<a class=\'orderIcon\' ng-if=\'by === "order"\'><img src="images/alphabet.png" alt=""></a>'+
                '<a class=\'orderIcon\' ng-if=\'by === "alphabetical"\'><img src="images/numeric.png" alt=""></a>'+
                '</p>',
            scope: {
                by: '='
            },
            link: function(scope, element, attrs) {
                scope.by = 'order';
                scope.onClick = function () {
                    if(scope.by ===  'alphabetical') {
                        scope.by = 'order';
                    } else {
                        scope.by = 'alphabetical' ;
                    }
                }
            }
        }
    })
    // The directive for sorting each table according to whether the default value property is empty or not (non-empty first)
    .directive("filled", function() {
        return {
            restrict: 'A',
            transclude: true,
            template :
                '<p ng-click="onClick()">'+
                '<span ng-transclude></span>'+
                '<a class=\'orderIcon\'><img src="images/alphabet.png" alt=""></a>'+
                '</p>',
            scope: {
                fill: '='
            },
            link: function(scope, element, attrs) {
                scope.fill = 'order';
                scope.onClick = function() {
                    if (scope.fill === 'order') {
                        scope.fill = '-defaultValue';
                    } else {
                        scope.fill = 'order';
                    }
                }
            }
        }
    });
