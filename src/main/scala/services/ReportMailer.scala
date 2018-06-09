package services

import java.io.File
import javax.mail.internet.InternetAddress
import monix.eval.Task
import courier._, Defaults._

trait ReportMailer {

  def sendReport(sender: String, subject: String, report: File, body: String)(implicit mailer: Mailer): Task[Unit] = {

    val address: InternetAddress = new InternetAddress(sender)

    Task.eval{
      mailer(Envelope.from(address)
        .to(address)
        .subject(subject)
        .content(Multipart()
          .attach(report)
          .html(body))).onComplete(_ => println("Report Delivered"))
    }.onErrorRestart(3L)

  }
}

object ReportMailer extends ReportMailer
