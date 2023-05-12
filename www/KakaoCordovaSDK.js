
var exec = require("cordova/exec");

var KakaoCordovaSDK = {
  login: function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, "KakaoCordovaSDK", "login", []);
  },

  sendLinkFeed: function (template, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "KakaoCordovaSDK", "sendLinkFeed", [template]);
  },
};

module.exports = KakaoCordovaSDK;
