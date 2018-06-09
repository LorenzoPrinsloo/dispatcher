package services

import java.io.File

import courier.{Envelope, Mailer, Multipart}
import interfaces.{ErrorPayload, SuccessResult}
import javax.mail.internet.InternetAddress

trait ReportMailer {

  def sendReport(sender: String, subject: String, report: File, body: String)(implicit mailer: Mailer): Either[ErrorPayload, SuccessResult] = {

    val address: InternetAddress = new InternetAddress(sender)
    Either.cond(template.isSuccess, template.get, ErrorPayload(404, s"Cannot find template $templateName"))

    val mailAction = mailer(Envelope.from(address)
          .to(address)
          .subject(subject)
          .content(Multipart()
            .attach(report)
            .html(body))).onComplete(_ => Right9(SuccessResult("Report Delivered"))

  }
}

object ReportMailer extends ReportMailer
