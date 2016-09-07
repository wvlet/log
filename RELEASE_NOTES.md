Release Notes
====
0.23
 * wvlet-log
    - Terminate log scanner thread automatically
    - Suppress sbt and scalatest related stack trace messages
 * wvlet-config 
    - Improved the configuration flow [#14](https://github.com/wvlet/wvlet/pull/14)
       - Config is now immutable. Deprecated ConfigBuilder.
       - Use prefix@tag.param format for properties file
       - Allow providing default config instances
       - Throw FileNotFoundException when YAML file is not found
       - Use Map for the internal config holder to avoid duplicates
       - Add method for returning default and overwritten configurations
       - Allow checking unused config properties for validation

0.22
 * Add Logger.scheduleLogLevelScan

0.21
 * Removed wvlet-inject in favor of [Airframe](https://github.com/wvlet/airframe)
 * Removed Config.bindConfigs(Inject) since it should be done outside wvlet-config 

2016-08-19 0.20
 * Support building abstract types (if possible)

2016-08-19 0.19
 * Show error message when an object cannot build 
 * Allow building concreate classes that have inject[X] parameter

2016-08-18 0.18
 * Fix eager singleton initialization

2016-08-16 0.17
 * Allow overriding Config using java.util.Properties
 * Improved test coverage of wvlet-log

2016-08-09 0.16
 * Avoid using auto-generated annonymous trait name for logger name of LogSupport trait
 * Exclude $ from Scala object logger name

2016-08-05 0.15 
 * Add wvlet-opts for parsing command line options
 * Fix tagged type Config @@ Scope binding in wvlet-config

2016-08-02 0.14
 * Fix SessionListner to track all injected objects
 * Suppress wvlet.inject logs

2016-08-02 0.13
 * Rename wvlet.inject.Context -> Session
 * Suppress log messages

2016-08-02 0.12
 * Enable injection of wvlet-config
 * Add trait instantiation support
 * Nested trait injection support
 * Fix binding override

2016-08-01 0.11
 * Add wvlet-inject for dependency injection
 * Reorganized modules into wvlet-core, wvlet-obj, wvlet-injext

2016-07 0.10
 * Add wvlet-jmx module

2016-06-03 0.8
 * Fix logger methods

2016-05-23 0.7
 * Add wvlet-config 

2016-05-16 0.4
 * Add LogRotationHandler
 * (since 0.1) Add various ANSI color logging LogFormatter

2016-05-04 0.1
 * Added wvlet-log, a handly logging library
