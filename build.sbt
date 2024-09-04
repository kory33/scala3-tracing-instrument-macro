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

ThisBuild / scalaVersion := "3.5.0"

def withCommonSubprojectSettings(p: Project): Project = p.settings {
  headerLicenseStyle := HeaderLicenseStyle.SpdxSyntax
}

lazy val root = tlCrossRootProject
  .aggregate(core)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "scala3-tracing-instrument-macro",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.12.0",
      "org.typelevel" %%% "cats-effect" % "3.5.4",
      "org.scalameta" %%% "munit" % "1.0.1" % Test,
      "org.typelevel" %%% "munit-cats-effect" % "2.0.0" % Test
    )
  )
  .configure(withCommonSubprojectSettings)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    laikaTheme := Helium.defaults.site
      .topNavigationBar(
        homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home),
      )
      .build
  )
