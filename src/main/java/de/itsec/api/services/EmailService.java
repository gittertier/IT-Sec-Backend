package de.itsec.api.services;

import de.itsec.api.data.Address;
import de.itsec.api.data.authentication.User;
import de.itsec.api.data.termin.Praxis;
import de.itsec.api.data.termin.Termin;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends the transactional emails: confirm your address, appointment booked, appointment cancelled.
 * The look matches the frontend - black and white, no blue - with the portal logo on top. Styling
 * is inline because email clients drop style blocks and external CSS.
 */
@Service
public class EmailService {

  // Inline Tabler SVGs - the same icon set (react-icons/tb) the frontend uses - so
  // the appointment rows show a calendar, clock, map pin and syringe.
  private static final String ICON_DATE =
      svgIcon(
          "M4 7a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12z",
          "M16 3v4", "M8 3v4", "M4 11h16", "M11 15h1", "M12 15v3");
  private static final String ICON_TIME =
      svgIcon("M3 12a9 9 0 1 0 18 0a9 9 0 0 0 -18 0", "M12 7v5l3 3");
  private static final String ICON_PLACE =
      svgIcon(
          "M9 11a3 3 0 1 0 6 0a3 3 0 0 0 -6 0",
          "M17.657 16.657l-4.243 4.243a2 2 0 0 1 -2.827 0l-4.244 -4.243a8 8 0 1 1 11.314 0z");
  private static final String ICON_VACCINE =
      svgIcon(
          "M17 3l4 4", "M19 5l-4.5 4.5", "M11.5 6.5l6 6", "M16.5 11.5l-6.5 6.5h-4v-4l6.5 -6.5",
          "M7.5 12.5l1.5 1.5", "M10.5 9.5l1.5 1.5", "M3 21l3 -3");

  private static String svgIcon(String... paths) {
    StringBuilder sb =
        new StringBuilder(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"18\" height=\"18\""
                + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#444444\" stroke-width=\"2\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\">");
    for (String d : paths) {
      sb.append("<path d=\"").append(d).append("\"/>");
    }
    return sb.append("</svg>").toString();
  }

  private static final String LOGO_DATA_URI = loadLogoDataUri();

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN);
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);

  private final JavaMailSender emailSender;
  private final String fromAddress;
  private final String verificationBaseUrl;

  public void sendVerificationEmail(User user) throws MessagingException {
    String link = this.verificationBaseUrl + user.getVerificationToken();
    String inner =
        paragraph(
                "willkommen beim Impfterminportal Rheinland-Pfalz. Bitte bestaetigen Sie Ihre"
                    + " E-Mail-Adresse, damit Sie Impftermine buchen koennen.")
            + button(link, "E-Mail-Adresse bestaetigen")
            + smallParagraph(
                "Falls der Button nicht funktioniert, kopieren Sie diesen Link in Ihren Browser:")
            + smallParagraph(rawLink(link))
            + smallParagraph(
                "Der Link ist drei Stunden gueltig. Wenn Sie sich nicht registriert haben, koennen"
                    + " Sie diese E-Mail einfach ignorieren.");
    String plain =
        salutationText(user)
            + ",\n\nwillkommen beim Impfterminportal Rheinland-Pfalz. Bitte bestaetigen Sie Ihre\n"
            + "E-Mail-Adresse ueber diesen Link:\n\n"
            + link
            + "\n\nDer Link ist drei Stunden gueltig.\n\nViele Gruesse\n"
            + "Ihr Impfterminportal Rheinland-Pfalz";
    send(user.getUsername(), "Bitte bestaetigen Sie Ihre E-Mail-Adresse", user, inner, plain);
  }

  public void sendBookingConfirmation(User user, Termin termin) throws MessagingException {
    String inner =
        heading("Termin gebucht")
            + paragraph("Ihr Impftermin ist gebucht. Hier sind Ihre Termindaten:")
            + appointmentTable(termin)
            + smallParagraph(
                "Bitte zeigen Sie beim Termin Ihre Terminuebersicht im Portal vor, damit die Praxis"
                    + " Ihren Termin verifizieren kann.");
    send(
        user.getUsername(),
        "Terminbestaetigung - Ihr Impftermin",
        user,
        inner,
        appointmentText(user, "Ihr Impftermin ist gebucht.", termin));
  }

  public void sendCancellationConfirmation(User user, Termin termin) throws MessagingException {
    String inner =
        heading("Termin storniert")
            + paragraph("Ihr folgender Impftermin wurde storniert:")
            + appointmentTable(termin)
            + smallParagraph("Sie koennen jederzeit einen neuen Termin im Portal buchen.");
    send(
        user.getUsername(),
        "Termin storniert",
        user,
        inner,
        appointmentText(user, "Ihr Impftermin wurde storniert.", termin));
  }

  private void send(String to, String subject, User user, String innerHtml, String plainText)
      throws MessagingException {
    MimeMessage message = emailSender.createMimeMessage();
    // multipart=true lets us send a plain-text and an HTML body as alternatives.
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    helper.setFrom(this.fromAddress);
    helper.setTo(to);
    helper.setSubject(subject);
    helper.setText(plainText, shell(user, innerHtml));
    emailSender.send(message);
  }

  // --- HTML building blocks (inline styles, black and white) ---------------------

  private static String shell(User user, String innerHtml) {
    String template =
        """
        <div style="background:#f5f5f5;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#111111;">
          <div style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e5e5;border-radius:12px;overflow:hidden;">
            <div style="padding:24px 28px;border-bottom:1px solid #ededed;text-align:center;">
              <img src="{{LOGO}}" alt="Impfterminportal Rheinland-Pfalz" style="display:inline-block;max-width:280px;width:100%;height:auto;" />
            </div>
            <div style="padding:28px;">
              <p style="margin:0 0 16px;font-size:16px;">{{SALUTATION}},</p>
              {{INNER}}
            </div>
            <div style="padding:16px 28px;background:#fafafa;border-top:1px solid #ededed;font-size:12px;color:#888888;">
              Diese E-Mail wurde automatisch versendet. Bitte antworten Sie nicht darauf.
            </div>
          </div>
        </div>
        """;
    return template
        .replace("{{LOGO}}", LOGO_DATA_URI)
        .replace("{{SALUTATION}}", salutationHtml(user))
        .replace("{{INNER}}", innerHtml);
  }

  private static String appointmentTable(Termin termin) {
    Praxis praxis = termin.getPraxis();
    Address address = praxis.getAddress();
    String date = termin.getStartTime().format(DATE_FORMAT);
    String time =
        termin.getStartTime().format(TIME_FORMAT)
            + " - "
            + termin.getEndTime().format(TIME_FORMAT)
            + " Uhr";
    String place =
        escape(praxis.getName())
            + "<br>"
            + escape(
                address.getStreet()
                    + " "
                    + address.getHouseNumber()
                    + ", "
                    + address.getAreaCode()
                    + " "
                    + address.getCity());
    String vaccine =
        termin.getVaccine() != null ? escape(termin.getVaccine()) : "Wird vor Ort festgelegt";

    return "<table style=\"width:100%;border-collapse:collapse;margin:8px 0 20px;\">"
        + detailRow(ICON_DATE, "Datum", escape(date))
        + detailRow(ICON_TIME, "Uhrzeit", escape(time))
        + detailRow(ICON_PLACE, "Praxis", place)
        + detailRow(ICON_VACCINE, "Impfstoff", vaccine)
        + "</table>";
  }

  private static String detailRow(String icon, String label, String value) {
    return "<tr>"
        + "<td style=\"padding:10px 12px;border-bottom:1px solid #ededed;width:24px;"
        + "line-height:0;vertical-align:middle;\">"
        + icon
        + "</td>"
        + "<td style=\"padding:10px 12px 10px 0;border-bottom:1px solid #ededed;font-size:13px;"
        + "color:#666666;width:90px;vertical-align:middle;\">"
        + label
        + "</td>"
        + "<td style=\"padding:10px 12px 10px 0;border-bottom:1px solid #ededed;font-size:14px;"
        + "color:#111111;vertical-align:middle;\">"
        + value
        + "</td>"
        + "</tr>";
  }

  private static String heading(String text) {
    return "<p style=\"margin:0 0 16px;font-size:18px;font-weight:bold;color:#111111;\">"
        + text
        + "</p>";
  }

  private static String paragraph(String text) {
    return "<p style=\"margin:0 0 16px;font-size:15px;line-height:1.6;\">" + text + "</p>";
  }

  private static String smallParagraph(String text) {
    return "<p style=\"margin:0 0 12px;font-size:13px;color:#666666;line-height:1.5;\">"
        + text
        + "</p>";
  }

  private static String button(String href, String label) {
    return "<p style=\"text-align:center;margin:24px 0;\">"
        + "<a href=\""
        + href
        + "\" style=\"display:inline-block;background:#111111;color:#ffffff;text-decoration:none;"
        + "padding:12px 28px;border-radius:8px;font-size:15px;font-weight:bold;\">"
        + label
        + "</a></p>";
  }

  private static String rawLink(String href) {
    return "<a href=\"" + href + "\" style=\"color:#111111;word-break:break-all;\">" + href + "</a>";
  }

  // --- Text and shared helpers ---------------------------------------------------

  private static String salutationHtml(User user) {
    return escape(salutationText(user));
  }

  private static String salutationText(User user) {
    String first = user.getFirstName();
    String last = user.getLastName();
    boolean hasName =
        first != null && !first.isBlank() && last != null && !last.isBlank();
    return hasName ? "Sehr geehrte/r " + first + " " + last : "Sehr geehrte Damen und Herren";
  }

  private static String appointmentText(User user, String intro, Termin termin) {
    Praxis praxis = termin.getPraxis();
    Address address = praxis.getAddress();
    String vaccine = termin.getVaccine() != null ? termin.getVaccine() : "Wird vor Ort festgelegt";
    return salutationText(user)
        + ",\n\n"
        + intro
        + "\n\n"
        + "Datum:    "
        + termin.getStartTime().format(DATE_FORMAT)
        + "\n"
        + "Uhrzeit:  "
        + termin.getStartTime().format(TIME_FORMAT)
        + " - "
        + termin.getEndTime().format(TIME_FORMAT)
        + " Uhr\n"
        + "Praxis:   "
        + praxis.getName()
        + ", "
        + address.getStreet()
        + " "
        + address.getHouseNumber()
        + ", "
        + address.getAreaCode()
        + " "
        + address.getCity()
        + "\n"
        + "Impfstoff: "
        + vaccine
        + "\n\nViele Gruesse\nIhr Impfterminportal Rheinland-Pfalz";
  }

  // Minimal HTML escaping for the few values that flow into the markup. Most are
  // controlled (seed/enum), but the first name comes from the user, so escape it.
  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  // Loads the logo once and inlines it as a base64 data URI, so the mail carries
  // its own image and needs no external host. The logo is decorative: if it cannot
  // be read, the empty src just falls back to the alt text.
  private static String loadLogoDataUri() {
    try (InputStream in = new ClassPathResource("email/logo.svg").getInputStream()) {
      String base64 = Base64.getEncoder().encodeToString(in.readAllBytes());
      return "data:image/svg+xml;base64," + base64;
    } catch (Exception e) {
      return "";
    }
  }

  @Autowired
  public EmailService(
      JavaMailSender emailSender,
      @Value("${mail.from}") String fromAddress,
      @Value("${mail.verification-base-url}") String verificationBaseUrl) {
    this.emailSender = emailSender;
    this.fromAddress = fromAddress;
    this.verificationBaseUrl = verificationBaseUrl;
  }
}
