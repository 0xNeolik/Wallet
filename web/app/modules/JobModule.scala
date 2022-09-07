package modules

//import com.clluc.stockmind.core.actor.Scheduler.SchedulingConfig
//import com.clluc.stockmind.core.actor.Scheduler.SchedulingConfig
import com.clluc.stockmind.core.actor.{LicenseActor}
//import com.google.inject.Provides
import play.api.libs.concurrent.AkkaGuiceSupport
//import akka.actor.{ActorRef, ActorSystem}
import net.codingwell.scalaguice.ScalaModule

/**
  * The job module.
  */
class JobModule extends ScalaModule with AkkaGuiceSupport {

  /**
    * Configures the module.
    */
  def configure() = {
    bind[LicenseActor].asEagerSingleton()
  }
  val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  /* @Provides
  def scheduler(
      system: ActorSystem,
      licenseActor: ActorRef,
      schedulingConfig: SchedulingConfig
  ) =
    Scheduler(system, licenseActor, schedulingConfig)

  import scala.concurrent.duration._
   */
  /* @Provides
  def schedulingConfig() =
    SchedulingConfig(1.minute)*/

  /*@Provides
  def authTokenCleaner(system: ActorSystem,
                       authTokenPort: AuthTokenPort,
                       executionContext: ExecutionContext) =
    system.actorOf(
      AuthTokenCleaner.props(
        authTokenPort,
        () => DateTime.now(DateTimeZone.UTC)
      )(executionContext)
    )*/
  /* @Provides
  def licenseActor(system: ActorSystem, licensePort: LicensePort) =
    system.actorOf(
      LicenseActor.props(
        licensePort
      )()
    )*/
}
