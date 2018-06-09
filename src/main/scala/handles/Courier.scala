package handles

import courier._, Defaults._

object Courier {
  implicit val mailer = Mailer("smtp.gmail.com", 587)
    .auth(true)
    .as("cmlprinsloo@gmail.com", "Spdf1357")
    .startTtls(true)()
}
