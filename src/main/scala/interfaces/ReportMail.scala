package interfaces

case class ReportMail(from: String,
                      to: String,
                      subject: String,
                      attachment: Array[Byte], //change to PDF model when complete
                      body: String,
                      delivered: Boolean = false)
