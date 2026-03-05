package com.gestion.salles.services;

/******************************************************************************
 * EmailService.java
 *
 * Singleton email service for the Gestion Salles application.
 * Sends verification codes, new user credentials, and password reset
 * confirmations via Gmail SMTP (TLS on port 587). Credentials are loaded
 * exclusively from AppConfig — no fallbacks, no hardcoded values.
 *
 * All three send methods follow the same pattern: validate the address,
 * catch AddressException separately to avoid masking SMTP errors, and
 * return false on any failure without throwing to callers.
 ******************************************************************************/

import com.gestion.salles.utils.AppConfig;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {

    private static final Logger LOGGER    = Logger.getLogger(EmailService.class.getName());
    private static final String APP_NAME  = "Gestion Salles";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static volatile EmailService instance;

    private final Session mailSession;

    private EmailService() {
        this.mailSession = createMailSession();
    }

    public static EmailService getInstance() {
        if (instance == null) {
            synchronized (EmailService.class) {
                if (instance == null) {
                    instance = new EmailService();
                }
            }
        }
        return instance;
    }

    public boolean isConfigured() {
        String email    = AppConfig.getSenderEmail();
        String password = AppConfig.getSenderAppPassword();
        return email != null && !email.trim().isEmpty()
            && password != null && !password.trim().isEmpty();
    }

    public boolean sendVerificationCode(String toEmail, String firstName, String lastName, String verificationCode) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            LOGGER.warning("Attempted to send verification email to null or empty address.");
            return false;
        }
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(AppConfig.getSenderEmail(), APP_NAME));
            message.setSubject("Code de Vérification de Compte - " + APP_NAME);
            try {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            } catch (AddressException ae) {
                LOGGER.log(Level.SEVERE, "Invalid recipient email address for verification: " + toEmail, ae);
                return false;
            }
            message.setContent(createVerificationCodeEmailHTML(firstName, lastName, verificationCode), "text/html; charset=utf-8");
            Transport.send(message);
            LOGGER.info("Verification email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending verification email to: " + toEmail, e);
            return false;
        }
    }

    public boolean sendNewUserCredentials(String toEmail, String firstName, String lastName, String userEmail, String temporaryPassword) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            LOGGER.warning("Attempted to send new user credentials email to null or empty address.");
            return false;
        }
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(AppConfig.getSenderEmail(), APP_NAME));
            message.setSubject("Nouveau Compte Utilisateur - " + APP_NAME);
            try {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            } catch (AddressException ae) {
                LOGGER.log(Level.SEVERE, "Invalid recipient email address for new user credentials: " + toEmail, ae);
                return false;
            }
            message.setContent(createNewUserCredentialsEmailHTML(firstName, lastName, userEmail, temporaryPassword), "text/html; charset=utf-8");
            Transport.send(message);
            LOGGER.info("New user credentials email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending new user credentials email to: " + toEmail, e);
            return false;
        }
    }

    public boolean sendPasswordResetConfirmation(String toEmail, String firstName, String lastName, String newPassword) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            LOGGER.warning("Attempted to send password reset confirmation email to null or empty address.");
            return false;
        }
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(AppConfig.getSenderEmail(), APP_NAME));
            message.setSubject("Réinitialisation de votre mot de passe - " + APP_NAME);
            try {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            } catch (AddressException ae) {
                LOGGER.log(Level.SEVERE, "Invalid recipient email address for password reset confirmation: " + toEmail, ae);
                return false;
            }
            message.setContent(createPasswordResetConfirmationEmailHTML(firstName, lastName, newPassword), "text/html; charset=utf-8");
            Transport.send(message);
            LOGGER.info("Password reset confirmation email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending password reset confirmation email to: " + toEmail, e);
            return false;
        }
    }

    private Session createMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",             SMTP_HOST);
        props.put("mail.smtp.port",             SMTP_PORT);
        props.put("mail.smtp.auth",             "true");
        props.put("mail.smtp.starttls.enable",  "true");
        props.put("mail.smtp.ssl.protocols",    "TLSv1.2");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(AppConfig.getSenderEmail(), AppConfig.getSenderAppPassword());
            }
        });
    }

    private String sharedCss() {
        return "    * { margin: 0; padding: 0; box-sizing: border-box; }" +
               "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; line-height: 1.6; color: #1e1e1e; background-color: #f7f9f8; padding: 20px; }" +
               "    .email-container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07); }" +
               "    .header { background: linear-gradient(135deg, #0F6B3F 0%, #13854d 100%); padding: 40px 30px; text-align: center; }" +
               "    .header h1 { color: #ffffff; font-size: 24px; font-weight: 600; margin-bottom: 8px; letter-spacing: 0.3px; }" +
               "    .header p { color: rgba(255, 255, 255, 0.9); font-size: 15px; font-weight: 400; }" +
               "    .content { padding: 40px 30px; }" +
               "    .greeting { font-size: 16px; color: #1e1e1e; margin-bottom: 20px; }" +
               "    .greeting strong { color: #0F6B3F; font-weight: 600; }" +
               "    .message { font-size: 15px; color: #4b5563; margin-bottom: 30px; line-height: 1.7; }" +
               "    .credentials-box { background: #f9fafb; border: 2px solid #e5e7eb; border-radius: 10px; padding: 30px; margin: 30px 0; text-align: center; }" +
               "    .credentials-box p { margin-bottom: 10px; font-size: 15px; color: #1e1e1e; }" +
               "    .credentials-box strong { font-weight: 600; color: #0F6B3F; }" +
               "    .info-box { background: #fef3c7; border-left: 4px solid #f59e0b; border-radius: 6px; padding: 20px; margin: 25px 0; }" +
               "    .info-box p { font-size: 14px; color: #78350f; margin-bottom: 12px; }" +
               "    .info-box p:last-child { margin-bottom: 0; }" +
               "    .info-box strong { font-weight: 600; color: #78350f; }" +
               "    .info-box ul { margin: 10px 0 0 20px; padding: 0; }" +
               "    .info-box li { font-size: 14px; color: #78350f; margin-bottom: 6px; }" +
               "    .closing { font-size: 15px; color: #4b5563; margin-top: 30px; line-height: 1.7; }" +
               "    .signature { margin-top: 15px; color: #1e1e1e; font-weight: 500; }" +
               "    .footer { background: #f9fafb; padding: 25px 30px; text-align: center; border-top: 1px solid #e5e7eb; }" +
               "    .footer p { font-size: 13px; color: #6b7280; margin: 8px 0; }" +
               "    .footer-divider { width: 50px; height: 2px; background: #0F6B3F; margin: 15px auto; }" +
               "    @media only screen and (max-width: 600px) {" +
               "      body { padding: 10px; }" +
               "      .email-container { border-radius: 8px; }" +
               "      .header { padding: 30px 20px; }" +
               "      .header h1 { font-size: 20px; }" +
               "      .content { padding: 30px 20px; }" +
               "      .credentials-box { padding: 25px 15px; }" +
               "    }";
    }

    private String sharedFooter() {
        return "    <div class='footer'>" +
               "      <div class='footer-divider'></div>" +
               "      <p>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>" +
               "      <p>&copy; 2025 " + APP_NAME + ". Tous droits réservés.</p>" +
               "    </div>";
    }

    private String sharedClosing() {
        return "      <div class='closing'>" +
               "        <p>Si vous rencontrez des difficultés ou avez des questions, n'hésitez pas à contacter l'université.</p>" +
               "        <div style='margin-top: 20px; font-size: 12px; color: #4b5563;'>" +
               "          <p>Contacter l'université:</p>" +
               "          <p>Email: <a href='mailto:" + AppConfig.getSenderEmail() + "' style='color:#0F6B3F; text-decoration: none;'>" + AppConfig.getSenderEmail() + "</a></p>" +
               "        </div>" +
               "        <p class='signature'>Cordialement,<br>L'équipe " + APP_NAME + "</p>" +
               "      </div>";
    }

    private String htmlOpen(String headerSubtitle) {
        return "<!DOCTYPE html>" +
               "<html lang='fr'>" +
               "<head>" +
               "  <meta charset='UTF-8'>" +
               "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "  <style>" + sharedCss() + "  </style>" +
               "</head>" +
               "<body>" +
               "  <div class='email-container'>" +
               "    <div class='header'>" +
               "      <h1>" + APP_NAME + "</h1>" +
               "      <p>" + headerSubtitle + "</p>" +
               "    </div>" +
               "    <div class='content'>";
    }

    private String htmlClose() {
        return "    </div>" +
               sharedFooter() +
               "  </div>" +
               "</body>" +
               "</html>";
    }

    private String createVerificationCodeEmailHTML(String firstName, String lastName, String code) {
        String fullName = firstName + " " + lastName;
        String verificationCss =
               "    .verification-code-box { background: rgba(0,0,0,0.05); border: 1px solid rgba(0,0,0,0.1); border-radius: 10px; padding: 25px; margin: 30px 0; text-align: center; }" +
               "    .verification-code-box p { font-size: 18px; color: #1e1e1e; margin-bottom: 15px; }" +
               "    .verification-code { display: inline-block; background: #ffffff; color: #0F6B3F; font-size: 32px; font-weight: 700; padding: 10px 20px; border-radius: 8px; letter-spacing: 3px; border: 1px solid transparent; }" +
               "    @media only screen and (max-width: 600px) {" +
               "      .verification-code-box { padding: 20px 15px; }" +
               "      .verification-code { font-size: 28px; }" +
               "    }";

        return "<!DOCTYPE html>" +
               "<html lang='fr'>" +
               "<head>" +
               "  <meta charset='UTF-8'>" +
               "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "  <style>" + sharedCss() + verificationCss + "  </style>" +
               "</head>" +
               "<body>" +
               "  <div class='email-container'>" +
               "    <div class='header'>" +
               "      <h1>" + APP_NAME + "</h1>" +
               "      <p>Code de vérification de votre compte</p>" +
               "    </div>" +
               "    <div class='content'>" +
               "      <p class='greeting'>Bonjour <strong>" + fullName + "</strong>,</p>" +
               "      <p class='message'>Vous avez demandé à réinitialiser votre mot de passe pour <strong>" + APP_NAME + "</strong>. Utilisez le code de vérification suivant :</p>" +
               "      <div class='verification-code-box'>" +
               "        <p>Votre code de vérification est :</p>" +
               "        <span class='verification-code'>" + code + "</span>" +
               "      </div>" +
               "      <div class='info-box'>" +
               "        <p><strong>⚠️ Important :</strong></p>" +
               "        <ul>" +
               "          <li>Ce code est valide pendant une courte période.</li>" +
               "          <li>Ne partagez jamais ce code avec qui que ce soit.</li>" +
               "          <li>Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet e-mail.</li>" +
               "        </ul>" +
               "      </div>" +
               sharedClosing() +
               "    </div>" +
               sharedFooter() +
               "  </div>" +
               "</body>" +
               "</html>";
    }

    private String createNewUserCredentialsEmailHTML(String firstName, String lastName, String userEmail, String temporaryPassword) {
        String fullName = firstName + " " + lastName;
        return htmlOpen("Création de votre compte utilisateur") +
               "      <p class='greeting'>Bonjour <strong>" + fullName + "</strong>,</p>" +
               "      <p class='message'>Votre compte a été créé avec succès pour la plateforme <strong>" + APP_NAME + "</strong>. Voici vos identifiants de connexion :</p>" +
               "      <div style='background: rgba(0,0,0,0.05); border: 1px solid rgba(0,0,0,0.1); border-radius: 10px; padding: 25px; margin: 30px 0; text-align: center;'>" +
               "        <p style='font-size: 18px; color: #1e1e1e; margin-bottom: 10px;'>Email: <strong>" + userEmail + "</strong></p>" +
               "        <p style='font-size: 18px; color: #1e1e1e; margin-bottom: 0;'>Mot de passe temporaire: <strong>" + temporaryPassword + "</strong></p>" +
               "      </div>" +
               "      <div class='info-box'>" +
               "        <p><strong>⚠️ Recommandation de sécurité :</strong></p>" +
               "        <ul>" +
               "          <li>Pour votre sécurité, nous vous recommandons fortement de changer ce mot de passe temporaire dès votre première connexion.</li>" +
               "          <li>Vous pouvez le faire via les paramètres de votre profil dans l'application.</li>" +
               "          <li>Ne partagez jamais votre mot de passe avec qui que ce soit.</li>" +
               "        </ul>" +
               "      </div>" +
               sharedClosing() +
               htmlClose();
    }

    private String createPasswordResetConfirmationEmailHTML(String firstName, String lastName, String newPassword) {
        String fullName = firstName + " " + lastName;
        return htmlOpen("Réinitialisation de votre mot de passe") +
               "      <p class='greeting'>Bonjour <strong>" + fullName + "</strong>,</p>" +
               "      <p class='message'>Votre mot de passe pour la plateforme <strong>" + APP_NAME + "</strong> a été réinitialisé par un administrateur. Voici votre nouveau mot de passe temporaire :</p>" +
               "      <div class='credentials-box'>" +
               "        <p style='font-size: 18px; color: #1e1e1e; margin-bottom: 0;'>Votre nouveau mot de passe:<br><strong style='color: #1e1e1e !important;'>" + newPassword + "</strong></p>" +
               "      </div>" +
               "      <div class='info-box'>" +
               "        <p><strong>⚠️ Recommandation de sécurité :</strong></p>" +
               "        <ul>" +
               "          <li>Pour votre sécurité, nous vous recommandons fortement de changer ce mot de passe temporaire dès votre première connexion.</li>" +
               "          <li>Vous pouvez le faire via les paramètres de votre profil dans l'application.</li>" +
               "          <li>Ne partagez jamais votre mot de passe avec qui que ce soit.</li>" +
               "        </ul>" +
               "      </div>" +
               sharedClosing() +
               htmlClose();
    }
}
