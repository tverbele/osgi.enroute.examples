'use strict';

(function() {

	var MODULE = angular.module('osgi.enroute.rube.goldberg.server',
			[ 'ngRoute', 'ngResource' ]);

	MODULE.config( function($routeProvider) {
		$routeProvider.when('/', { controller: mainProvider, templateUrl: '/osgi.enroute.rube.goldberg.server/main/htm/home.htm'});
		$routeProvider.when('/about', { templateUrl: '/osgi.enroute.rube.goldberg.server/main/htm/about.htm'});
		$routeProvider.otherwise('/');
	});
	
	MODULE.run( function($rootScope, $location) {
		$rootScope.alerts = [];
		$rootScope.closeAlert = function(index) {
			$rootScope.alerts.splice(index, 1);
		};
		$rootScope.page = function() {
			return $location.path();
		}
	});
	
	
	
	var mainProvider = function($scope, $http) {
		$http.get('/rest/contraptions').success(
			function(response) {
					$scope.contraptions = angular.copy(JSON.parse(response));
					console.log($scope.contraptions);
				}
		);
		$scope.contraptions = [];
		$scope.running = false;
		$scope.start = function(contraption) {
			$http.get('/rest/start/'+contraption).success(
					function(response) {
						$scope.running = true;
					}
			);
		};	
	}
	
})();
