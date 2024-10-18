import laika.ast.Path.Root
import laika.helium.Helium
import laika.helium.config.{HeliumIcon, IconLink}

ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "io.github.kory33"
ThisBuild / organizationName := "Ryosuke Kondo"
ThisBuild / startYear := Some(2024)
ThisBuild / licenses := List(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("kory33", "Ryosuke Kondo")
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

ThisBuild / scalaVersion := "3.6.0"

def withCommonSubprojectSettings(p: Project): Project = p.settings {
  headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax
}

lazy val root = tlCrossRootProject
  .aggregate(core, integration_otel4s)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules") / "core")
  .settings(
    name := "scala3-tracing-instrument-macro-core"
  )
  .configure(withCommonSubprojectSettings)

lazy val integration_otel4s = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(core)
  .in(file("modules") / "integrations" / "otel4s")
  .settings(
    name := "scala3-tracing-instrument-macro-otel4s",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "otel4s-core" % "0.10.0",
      "org.typelevel" %%% "otel4s-sdk-testkit" % "0.10.0" % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % "1.5.0" % Test
    )
  )
  .configure(withCommonSubprojectSettings)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    laikaTheme := Helium.defaults.site
      .topNavigationBar(
        homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home)
      )
      .build
  )
