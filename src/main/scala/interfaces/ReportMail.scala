package interfaces

import courier.{Envelope, Multipart}
import javax.mail.internet.InternetAddress
import java.io.{File => JFile}

import better.files.File
import monix.eval.Task

case class ReportMail(from: String,
                      to: String,
                      subject: String,
                      attachment: Array[Byte], //change to PDF model when complete
                      body: String,
                      delivered: Boolean = false) {

  final def toEnvelope(file: File): Task[Envelope] = {
    val f: InternetAddress = new InternetAddress(from)
    val t: InternetAddress = new InternetAddress(to)

    deserializeAttachment(file)
      .map(report =>
        Envelope.from(f)
          .to(t)
          .subject(subject)
          .content(Multipart()
            .attach(report)
            .html(body)))
  }


  private def deserializeAttachment(report: File): Task[JFile] = {
    Task(report.writeByteArray(attachment).toJava)
  }
}
