package de.itsec.api.services;

import de.itsec.api.data.authentication.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final String VERFICATION_CONTENT =
      "Dear [[name]], "
          + "Please click the link below to verify your registration: [[URL]] "
          + "Thank you, Your company name.";
  private static final String VERFICATION_SUBJECT = "Please verify your registration";
  private MailSender emailSender;
  private String fromAddress;
  private String url;

  public void sendVerificationEmail(User user) {

    sendSimpleMessage(
        user.getUsername(),
        VERFICATION_SUBJECT,
        VERFICATION_CONTENT
            .replace("[[name]]", user.getFullName())
            .replace("[[URL]]", this.url + user.getVerificationToken()));
  }

  public void sendSimpleMessage(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(this.fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    emailSender.send(message);
  }

  @Autowired
  public EmailService(
      MailSender eMailSender,
      @Value("${mail.from}") String fromAddress,
      @Value("${mail.verification-base-url}") String url) {
    this.emailSender = eMailSender;
    this.fromAddress = fromAddress;
    this.url = url;
  }
}
