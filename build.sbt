name := "di-exp"

version := "1.0"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.bintrayRepo("stanch", "maven"),
  Resolver.bintrayRepo("drdozer", "maven")
)

libraryDependencies += "org.typelevel" %% "cats" % "0.8.0"
libraryDependencies += "org.stanch" %% "reftree" % "0.7.2"
