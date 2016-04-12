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
  sonatypeProfileName := "org.xerial",
  pomExtra := {
  <url>https://github.com/xerial/wvlet</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/xerial/wvlet.git</connection>
      <developerConnection>scm:git:git@github.com:xerial/wvlet.git</developerConnection>
      <url>github.com/xerial/wvlet.git</url>
    </scm>
    <developers>
      <developer>
        <id>leo</id>
        <name>Taro L. Saito</name>
        <url>http://xerial.org/leo</url>
      </developer>
    </developers>
  }
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
  ).aggregate(wvletCore, wvletTest, wvletCui)

lazy val wvletCore =
  Project(id = "wvlet-core", base = file("wvlet-core")).settings(
    buildSettings,
    description := "wvlet core module",
    libraryDependencies ++= Seq(
      "org.xerial" %% "xerial-core" % "3.5.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
    )
  ).dependsOn(wvletTest % "test->compile")

lazy val wvletLens =
  Project(id = "wvlet-lens", base = file("wvlet-lens")).settings(
    buildSettings,
    description := "wvlet bi-directional lens module",
    libraryDependencies ++= Seq(
      "org.xerial" %% "xerial-lens" % "3.5.0"
    )
  ).dependsOn(wvletCore, wvletTest % "test->compile")

lazy val wvletCui =
  Project(id = "wvlet-cui", base = file("wvlet-cui")).settings(
    buildSettings,
    description := "wvlet commandline tools",
    libraryDependencies ++= Seq(
      "org.xerial" %% "xerial-lens" % "3.5.0"    
    )
  ) dependsOn(wvletCore, wvletLens, wvletTest % "test->compile")

lazy val wvletTest =
  Project(id = "wvlet-test", base = file("wvlet-test")).settings(
    buildSettings,
    description := "wvlet testing module",
    libraryDependencies ++= Seq(
      "org.xerial" %% "xerial-core" % "3.5.0",
      "org.scalatest" %% "scalatest" % "2.2.+",
      "org.scalacheck" %% "scalacheck" % "1.11.4",
      "ch.qos.logback" % "logback-classic" % "1.1.2"
    )
  )