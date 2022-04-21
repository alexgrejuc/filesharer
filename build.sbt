ThisBuild / version := "0"

ThisBuild / scalaVersion := "3.1.1"

Global / excludeLintKeys += idePackagePrefix

lazy val root = (project in file("."))
  .settings(
    name := "FileSharer",
    idePackagePrefix := Some("filesharer")
  )
