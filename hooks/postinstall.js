
const fs = require("fs");
const path = require('path');
const xcode = require('xcode');
const semver = require('semver');

module.exports = context => {
  const projectRoot = context.opts.projectRoot;

  if (
    (context.hook === 'after_prepare' && context.cmdLine.includes('prepare')) ||
    (context.hook === 'after_plugin_add' && context.cmdLine.includes('plugin add')) ||
    (context.hook === 'after_platform_add' && context.cmdLine.includes('platform add'))
  ) {
    // patch xcode
    patchXcode(projectRoot);

    // patch android build.gradle
    patchGradle(projectRoot);
  }
};

function patchGradle(projectRoot) {
  const platformPath = path.join(projectRoot, 'platforms', 'android');
  const appGradlePath = path.join(platformPath, 'app','build.gradle');

  try {
    fs.accessSync(appGradlePath);

    const buildGradle = fs.readFileSync(appGradlePath, "utf8");

    fs.writeFileSync(appGradlePath,buildGradle.replace(/apply plugin: 'kotlin-android-extensions'/g,""));
  } catch (e) {
    console.error(e);
  }
}

function patchXcode(projectRoot) {
const platformPath = path.join(projectRoot, 'platforms', 'ios');
const pbxprojPath = path.join(platformPath,'Pods', 'Pods.xcodeproj', 'project.pbxproj');

  try {
    fs.accessSync(pbxprojPath);
    const COMMENT_KEY = /_comment$/;
    const xcodeProject = xcode.project(pbxprojPath);

    xcodeProject.parseSync();
    buildConfigs = xcodeProject.pbxXCBuildConfigurationSection();

    for (configName in buildConfigs) {
      if (!COMMENT_KEY.test(configName)) {
        buildConfig = buildConfigs[configName];
        xcodeProject.updateBuildProperty('BUILD_LIBRARY_FOR_DISTRIBUTION', "YES", buildConfig.name);
      }
    }

    fs.writeFileSync(pbxprojPath, xcodeProject.writeSync());
  } catch (e) {
    console.error(e);
  }
}
