var exec = require('cordova/exec');

var KakaoCordovaSDK = {
  login: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'login', []);
	},

  logout: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'logout', []);
  },

  unlinkApp: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'unlinkApp', []);
	},

  getAccessToken: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'getAccessToken', []);
  },

  sendLinkFeed: function(template, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'sendLinkFeed', [template]);
  },

  sendLinkCustom: function(template, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'KakaoCordovaSDK', 'sendLinkCustom', [template]);
  },
};

module.exports = KakaoCordovaSDK;
