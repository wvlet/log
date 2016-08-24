# wvlet  [![Gitter Chat][gitter-badge]][gitter-link] [![CircleCI](https://circleci.com/gh/wvlet/wvlet.svg?style=svg)](https://circleci.com/gh/wvlet/wvlet) [![Coverage Status][coverall-badge]][coverall-link]

[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/wvlet?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/wvlet/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/wvlet?branch=master

wvlet (weavelet) is a framework for weaving objects in Scala.

With wvlet, you can:
 - Build a new object from its dependencies (as in Google Guice)
 - Build objects from various data sources (e.g., JSON, JDBC, etc.)
 - Send object data through JMX
 - etc.


## projects

| project      |                                         | version |
| -------------- | --------------------------------------- | -------- |
| wvlet-log      | Logging utility                          | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-log_2.11) |
| wvlet-obj   |  Object schema inspection library   | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-obj_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-obj_2.11) |
| wvlet-inject      | Dependency injection library     | Moved to [Airframe](https://github.com/wvlet/airframe) |
| wvlet-jmx   | JMX utility| [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-jmx_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-jmx_2.11) |
| wvlet-config    | Configuration loader from Yaml          | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-config_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-config_2.11) |
| wvlet-opts    | Command line option parser          | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-opts_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-opts_2.11) |
| wvlet-core     | Object data processing operators     | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.wvlet/wvlet-core_2.11) |
