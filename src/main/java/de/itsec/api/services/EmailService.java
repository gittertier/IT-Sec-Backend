package de.itsec.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private MailSender emailSender;
  private String fromAddress;

  public void sendSimpleMessage(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(this.fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    emailSender.send(message);
  }

  @Autowired
  public EmailService(MailSender eMailSender, @Value("${mail.from}") String fromAddress) {
    this.emailSender = eMailSender;
    this.fromAddress = fromAddress;
  }
}
