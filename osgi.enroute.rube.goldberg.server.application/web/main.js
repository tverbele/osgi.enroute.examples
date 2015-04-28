'use strict';

(function() {

	var MODULE = angular.module('osgi.enroute.rube.goldberg.server',
			[ 'ngRoute', 'ngResource', 'enEasse' ]);

	MODULE.config( function($routeProvider) {
		$routeProvider.when('/', { controller: mainProvider, templateUrl: '/osgi.enroute.rube.goldberg.server/main/htm/home.htm'});
		$routeProvider.when('/about', { templateUrl: '/osgi.enroute.rube.goldberg.server/main/htm/about.htm'});
		$routeProvider.otherwise('/');
	});
	
	MODULE.run( function($rootScope, $location
			, en$easse
			) {
		$rootScope.page = function() {
			return $location.path();
		}
	});
	
	
	var mainProvider = function($scope, $http, en$easse) {
		$scope.contraptions = [];

	    // start contraption on click
		$scope.running = false;
		$scope.start = function(contraption) {
			$http.get('/rest/start/'+contraption).success(
					function(response) {
						$scope.running = true;
					}
			);
		};	
		
		
		// listeners for events
		$scope.online = function(event) {
			$scope.contraptions.push(event.id);
			$scope.$apply();
		};	
		
		$scope.offline = function(event) {
			var index = $scope.contraptions.indexOf(event.id);
			$scope.contraptions.splice(index, 1);
			$scope.$apply();
		};
		
		$scope.error = function(e){
			console.log("Error... "+e);
		};
		
		en$easse.handle("osgi/enroute/rube/goldberg/online", $scope.online, $scope.error);
		en$easse.handle("osgi/enroute/rube/goldberg/offline", $scope.offline, $scope.error);

		
		// initialize with available contraptions
		$http.get('/rest/contraptions').success(
			function(response) {
					$scope.contraptions = angular.copy(JSON.parse(response));
			}
		);
	}
	
})();
