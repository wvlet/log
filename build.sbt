import ReleaseTransformations._


val buildSettings = Seq[Setting[_]](
  scalaVersion := "2.11.8",
  organization := "org.wvlet",
  description := "A framework for structured data mapping",
  crossPaths := true,
  publishMavenStyle := true,
  // For performance testing, ensure each test run one-by-one
  concurrentRestrictions in Global := Seq(Tags.limit(Tags.Test, 1)),
  incOptions := incOptions.value.withNameHashing(true),
  logBuffered in Test := false,
  updateOptions := updateOptions.value.withCachedResolution(true),
  sonatypeProfileName := "org.wvlet",
  pomExtra := {
  <url>https://github.com/xerial/wvlet</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/wvlet/wvlet.git</connection>
      <developerConnection>scm:git:git@github.com:wvlet/wvlet.git</developerConnection>
      <url>github.com/wvlet/wvlet.git</url>
    </scm>
    <developers>
      <developer>
        <id>leo</id>
        <name>Taro L. Saito</name>
        <url>http://xerial.org/leo</url>
      </developer>
    </developers>
  },
  // Release settings
  releaseTagName := { (version in ThisBuild).value },
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

lazy val wvlet =
  Project(id = "wvlet", base = file(".")).settings(
    buildSettings,
    // For creaating target/pack with sbt-pack
    packSettings,
    packMain := Map("wv" -> "wvlet.cui.WvletMain"),
    publishArtifact := false,
    publish := {},
    publishLocal := {},
    packExclude := Seq("wvlet")
  ).aggregate(wvletCore, wvletObj, wvletOpts, wvletTest, wvletLog, wvletConfig, wvletJmx) //, wvletLens, wvletJdbc, wvletDataframe, wvletRest, wvletTest, wvletCui)

lazy val wvletLog =
  Project(id = "wvlet-log", base = file("wvlet-log")).settings(
    buildSettings,
    description := "Handy logging wrapper for java.util.logging",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "ch.qos.logback" % "logback-core" % "1.1.7",
      "org.scalatest" %% "scalatest" % "2.2.+" % "test"
    )
  )

lazy val wvletJmx =
  Project(id = "wvlet-jmx", base = file("wvlet-jmx")).settings(
    buildSettings,
    description := "wvlet handy logging wrapper for java.util.logging",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  ).dependsOn(wvletLog, wvletCore, wvletTest % "test->compile")

lazy val wvletConfig =
  Project(id = "wvlet-config", base = file("wvlet-config")).settings(
    buildSettings,
    description := "wvlet configuration module",
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % "1.14",
      "org.wvlet" %% "airframe" % "0.6" % "test"
    )
  ).dependsOn(wvletObj, wvletTest % "test->compile")

lazy val wvletCore =
  Project(id = "wvlet-core", base = file("wvlet-core")).settings(
    buildSettings,
    description := "wvlet core module",
    libraryDependencies ++= Seq(
      "org.msgpack" % "msgpack-core" % "0.8.7"
    )
  ).dependsOn(wvletLog, wvletObj, wvletTest % "test->compile")

lazy val wvletObj =
  Project(id = "wvlet-obj", base = file("wvlet-obj")).settings(
    buildSettings,
    description := "wvlet object schema inspector",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  ).dependsOn(wvletLog, wvletTest % "test->compile")

lazy val wvletRest =
  Project(id = "wvlet-rest", base = file("wvlet-rest")).settings(
    buildSettings,
    description := "wvlet for REST applications",
    libraryDependencies ++= Seq(
      "javax.ws.rs" % "javax.ws.rs-api" % "2.0.1",
      "org.xerial" %% "xerial-lens" % "3.5.0",
      "javax.servlet" % "javax.servlet-api" % "3.1.0",
      "com.twitter" %% "finagle-http" % "6.34.0"
    )
  ).dependsOn(wvletCore, wvletTest % "test->compile")

lazy val wvletOpts =
  Project(id = "wvlet-opts", base = file("wvlet-opts")).settings(
    buildSettings,
    description := "wvlet command-line option parser",
    libraryDependencies ++= Seq(
    )
  ) dependsOn(wvletObj, wvletTest % "test->compile")

lazy val wvletCui =
  Project(id = "wvlet-cui", base = file("wvlet-cui")).settings(
    buildSettings,
    description := "wvlet commandline tools",
    libraryDependencies ++= Seq(
      "org.xerial" %% "xerial-lens" % "3.5.0"    
    )
  ) dependsOn(wvletCore, wvletTest % "test->compile")

lazy val wvletTest =
  Project(id = "wvlet-test", base = file("wvlet-test")).settings(
    buildSettings,
    description := "wvlet testing module",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.+",
      "org.scalacheck" %% "scalacheck" % "1.11.4"
    )
  ) dependsOn(wvletLog)