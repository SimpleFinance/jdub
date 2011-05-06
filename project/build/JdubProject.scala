import sbt._
import maven._

class JdubProject(info: ProjectInfo) extends DefaultProject(info) with MavenDependencies with IdeaProject {
  lazy val publishTo = Resolver.sftp("repo.codahale.com", "codahale.com", "/home/codahale/repo.codahale.com/")

  /**
   * Publish the source as well as the class files.
   */
  override def packageSrcJar = defaultJarPath("-sources.jar")
  lazy val sourceArtifact = Artifact.sources(artifactID)
  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageSrc)

  /**
   * Always check for new versions of snapshot dependencies.
   */
  override def snapshotUpdatePolicy = SnapshotUpdatePolicy.Always

  /**
   * Repositories
   */
  val codasRepo = "Coda's Repo" at "http://repo.codahale.com"
  
  /**
   * Dependencies
   */
  val dbcp = "org.apache.tomcat" % "tomcat-dbcp" % "7.0.8"
  val metrics = "com.yammer.metrics" %% "metrics-core" % "2.0.0-BETA12-SNAPSHOT"
  val logula = "com.codahale" %% "logula" % "2.1.1"
  
  /**
   * Test Dependencies
   */
  def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
  override def testFrameworks = super.testFrameworks ++ Seq(specs2Framework)
  val specs2 = "org.specs2" %% "specs2" % "1.2" % "test"
  val hsqldb = "org.hsqldb" % "hsqldb" % "1.8.0.10" % "test"
}
