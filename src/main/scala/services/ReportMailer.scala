package services

import better.files._
import courier.Defaults._
import courier._
import interfaces.ReportMail
import monix.eval.Task

trait ReportMailer {

  final def sendReport(mail: ReportMail)(implicit mailer: Mailer): Task[Unit] = { //TODO Refactor and minimize side effects, add file SALT and hashing SHA-256

    Task.eval {
      for {
        tempFile <- file"/User/johndoe/Documents/report.pdf".toTemporary
      } mail.toEnvelope(tempFile)
        .map(env =>
          mailer(env)
          .onComplete(_ => auditMessage(mail, true))
        ).onErrorRestart(3L)
        .onErrorHandle(_ => auditMessage(mail, false))
    }
  }

  def auditMessage(mail: ReportMail, delivered: Boolean): ReportMail = { //TODO separate into different service for auditing mails to mongodb
    if (delivered) {
      mail.copy(delivered = true)
    } else {
      mail
    }
  }
}

object ReportMailer extends ReportMailer
