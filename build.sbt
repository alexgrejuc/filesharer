ThisBuild / version := "0"

ThisBuild / scalaVersion := "3.1.1"

lazy val root = (project in file("."))
  .settings(
    name := "FileSharer",
    idePackagePrefix := Some("filesharer")
  )
