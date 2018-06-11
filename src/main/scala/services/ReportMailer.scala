package services

import java.io.{File => JFile}
import better.files._
import courier.Defaults._
import courier._
import interfaces.ReportMail
import javax.mail.internet.InternetAddress
import monix.eval.Task

trait ReportMailer {

  def sendReport(mail: ReportMail)(implicit mailer: Mailer): Unit = { //TODO Refactor and make asynchronous

    val from: InternetAddress = new InternetAddress(mail.from)
    val to: InternetAddress = new InternetAddress(mail.to)
    val file: File = file"/User/johndoe/Documents/report.pdf"

    for {
      tempFile <- file.toTemporary
    } bytesToFile(mail.attachment, tempFile)
      .map(report =>
        mailer(Envelope.from(from)
        .to(to)
        .subject(mail.subject)
        .content(Multipart()
          .attach(report)
          .html(mail.body))).onComplete(_ => auditMessage(mail, true))
      ).onErrorRestart(3L)
        .onErrorHandle(_ => auditMessage(mail, false))
  }

  def bytesToFile(bytes : Array[Byte], report: File): Task[JFile] = {
    Task(report.writeByteArray(bytes).toJava)
  }

  def auditMessage(mail: ReportMail, delivered: Boolean): ReportMail = {
    if (delivered) {
      mail.copy(delivered = true)
    } else {
      mail
    }
  }
}

object ReportMailer extends ReportMailer
