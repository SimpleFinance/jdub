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
  val dbcp = "org.apache.tomcat" % "tomcat-dbcp" % "7.0.12"
  val metrics = "com.yammer.metrics" %% "metrics-core" % "2.0.0-BETA13-SNAPSHOT"
  val logula = "com.codahale" %% "logula" % "2.1.2"
  
  /**
   * Test Dependencies
   */
  val simplespec = "com.codahale" %% "simplespec" % "0.3.2" % "test"
  def specs2Framework = new TestFramework("org.specs2.runner.SpecsFramework")
  override def testFrameworks = super.testFrameworks ++ Seq(specs2Framework)
  val hsqldb = "org.hsqldb" % "hsqldb" % "2.0.0" % "test"
}
