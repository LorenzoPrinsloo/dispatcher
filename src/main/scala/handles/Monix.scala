package handles

import monix.execution.Scheduler

object Monix {

  //Schedulers
  implicit val io: Scheduler = Scheduler.fixedPool("io", 200)
  implicit val aggregation: Scheduler = Scheduler.fixedPool("aggregation", 200)
}
