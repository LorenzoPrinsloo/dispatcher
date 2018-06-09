package handles.rabbit.implicits

import monix.eval.Task
import org.slf4j.Logger

import scala.util.control.NonFatal

object TaskImplicits {

  implicit class TaskImpl[A](t: Task[A]) {

    def onErrorHandleWithLogging[U >: A](f: Throwable => U)(implicit logger: Logger): Task[U] = {
      t.onErrorHandle { throwable =>
        logger.error("Error while executing task.", throwable)
        f(throwable)
      }
    }

    def onErrorRetryIfSync(predicate: Throwable => Boolean)(f: => Unit): Task[A] = {
      t.onErrorHandleWith {
        case NonFatal(ex) if predicate(ex) => Task.eval(f).flatMap(_ => t)
      }
    }
  }

}
