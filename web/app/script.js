/*
 * Template for the web app script generated by JMRI.
 * 
 * This template will be completed with dependences (item 1), routes (2),
 * navigation menu items (3), and additional providers (4)
 */

// create the jmri.app module
angular.module('jmri.app', [
  // dependencies %1$s
]);

// configure the jmri.app module
angular.module('jmri.app').config(
  function ($routeProvider, $logProvider) {
    'use strict';

    $routeProvider
    // routes %2$s
    // Default
    .otherwise({redirectTo: '/'});
    
    $logProvider.debugEnabled = false;
  }
);

// add the navigation menu controller to the jmri.app module
angular.module('jmri.app').controller('NavigationCtrl',
  function ($scope, $http, jmriWebSocket, $log, $interval, $rootScope, Notifications) {
    // navigation items %3$s

    // notification service
    $rootScope.toastNotifications = Notifications.data;
    
    // about display
    $http.get('/app/about').then(function (response) {
      $scope.additionalInfo = response.data.additionalInfo;
      $scope.copyright = response.data.copyright;
      $scope.imgAlt = response.data.imgAlt;
      $scope.imgSrc = response.data.imgSrc;
      $scope.title = response.data.title;
      $scope.productInfo = response.data.productInfo;
    });
    $scope.openAboutModal = function () {
      $scope.isAboutModalOpen = true;
    };
    $scope.onAboutModalClose = function() {
      $scope.isAboutModalOpen = false;
    };
    
    // core socket services (power and keep alive)
    $scope.heartbeat;
    $scope.startHeartbeat = function(heartbeat) {
      if (angular.isDefined($scope.heartbeat)) {
        return;
      }
      $log.info("Starting heartbeat every " + heartbeat + " ms.");
      $scope.heartbeat = $interval(function() {
        jmriWebSocket.ping();
      }, heartbeat);
    };
    $scope.stopHeartbeat = function() {
      if (angular.isDefined(heartbeat)) {
        $interval.cancel(heartbeat);
        heartbeat = undefined;
      }
    };
    $scope.power = [];
    $scope.defaultPowerState = jmriWebSocket.UNKNOWN;
    $scope.setPower = function(name, state) {
      jmriWebSocket.setPower(name, (state === jmriWebSocket.POWER_ON) ? jmriWebSocket.POWER_OFF : jmriWebSocket.POWER_ON);
    };
    jmriWebSocket.register({
      open: function() {
        jmriWebSocket.list('power');
      },
      power: function(data, method) {
        $log.debug('got ' + (data.default ? 'default ' : '') + 'power ' + data.name + " (" + data.state + ")");
        if (method === 'delete') {
          for (var i = 0; i < $scope.power.length; i++) {
            if (data.name === $scope.power[i].name) {
              $scope.power.splice(i, 1);
              break;
            }
          }
        }
        if (!jmriWebSocket.mergePush($scope.power, data)) {
          // express a desire to listen for changes in the object state
          jmriWebSocket.getPower(data.name);
        }
        if (data.default) {
          $scope.defaultPowerState = data.state;
        }
      },
      hello: function(data, method) {
        $scope.startHeartbeat(data.heartbeat);
      },
      pong: function(data, method) {
        $log.debug("Received pong");
      },
      close: function() {
        $scope.stopHeartbeat();
      }
      
    });
    
    // set the browser locale
    // TODO: read cookie with locale if one exists
    // TODO: move this into the factory
    if (!jmriWebSocket.socket.readyState) {
      var locale = (navigator.languages && navigator.languages.length) ? navigator.languages[0] : navigator.language;
      jmriWebSocket.get('locale', {locale: locale});
    }
    jmriWebSocket.list('power');

  }
);

angular.module('jmri.app').filter('powerTriState', function(jmriWebSocket) {
  return function(input) {
    switch(input) {
      case jmriWebSocket.POWER_OFF:
        return 'fa-circle-o';
        break;
      case jmriWebSocket.POWER_ON:
        return 'fa-bolt';
        break;
      default: // unknown
        return 'fa-question';
    }
  };
});

// additional providers %4$s