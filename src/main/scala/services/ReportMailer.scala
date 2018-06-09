package services

import java.io.File

import courier.{Envelope, Mailer, Multipart}
import interfaces.{ErrorPayload, SuccessResult}
import javax.mail.internet.InternetAddress
import monix.eval.Task

trait ReportMailer {

  def sendReport(sender: String, subject: String, report: File, body: String)(implicit mailer: Mailer): Task[Unit] = {

    val address: InternetAddress = new InternetAddress(sender)
    var attempts: Int = 0

    Task.eval{
      mailer(Envelope.from(address)
        .to(address)
        .subject(subject)
        .content(Multipart()
          .attach(report)
          .html(body))).onComplete(_ => {attempts + 1; println("Report Delivered")})
    }.onErrorRestartIf(_ => attempts < 3)

  }
}

object ReportMailer extends ReportMailer
