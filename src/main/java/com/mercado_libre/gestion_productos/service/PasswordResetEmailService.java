package com.mercado_libre.gestion_productos.service;

import com.mercado_libre.gestion_productos.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String smtpLogin;
    private final String smtpKey;
    private final String smtpHost;
    private final long expirationMinutes;

    public PasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${spring.mail.username:}") String smtpLogin,
            @Value("${spring.mail.password:}") String smtpKey,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.password-reset.expiration-minutes:60}") long expirationMinutes
    ) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.smtpLogin = smtpLogin == null ? "" : smtpLogin.trim();
        this.smtpKey = smtpKey == null ? "" : smtpKey.replaceAll("\\s+", "");
        this.smtpHost = smtpHost == null ? "" : smtpHost;
        this.fromAddress = StringUtils.hasText(fromAddress)
                ? fromAddress
                : "Mercado Libre Inventario <" + smtpLogin + ">";
        this.expirationMinutes = expirationMinutes;
    }

    @PostConstruct
    void validateMailConfiguration() {
        if (!mailEnabled) {
            return;
        }
        if (!StringUtils.hasText(smtpLogin) || !StringUtils.hasText(smtpKey)) {
            throw new IllegalStateException(
                    "Correo habilitado (MAIL_ENABLED=true) pero faltan MAIL_USERNAME o MAIL_APP_PASSWORD"
            );
        }
        log.info("Correo SMTP configurado. Remitente: {}", fromAddress);
    }

    public void sendPasswordResetEmail(User user, String resetUrl) {
        if (!mailEnabled) {
            log.info("Correo deshabilitado (app.mail.enabled=false). Enlace para {}: {}", user.getEmail(), resetUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(user.getEmail());
            helper.setSubject("Restablece tu contrasena - Mercado Libre Inventario");
            helper.setText(buildHtmlBody(user.getFirstName(), resetUrl), true);
            mailSender.send(message);
            log.info("Correo de restablecimiento enviado a {}", user.getEmail());
        } catch (MessagingException | MailException ex) {
            if (ex instanceof MailAuthenticationException) {
                log.error("Autenticacion SMTP fallida para usuario '{}'. {}", smtpLogin, buildAuthHelpMessage());
            } else {
                log.error("No se pudo enviar el correo a {}.", user.getEmail(), ex);
            }
        }
    }

    private String buildAuthHelpMessage() {
        if (smtpHost.contains("gmail")) {
            return """
                    Usa MAIL_USERNAME = correo exacto de Google (ej. tu@gmail.com o tu@ucp.edu.co si es Google Workspace). \
                    MAIL_APP_PASSWORD = contrasena de aplicacion de 16 caracteres (NO tu contrasena normal). \
                    Requisitos: verificacion en 2 pasos activa y clave creada en https://myaccount.google.com/apppasswords
                    """;
        }
        return "Revisa MAIL_USERNAME, MAIL_APP_PASSWORD y MAIL_HOST en tu archivo .env.";
    }

    private String buildHtmlBody(String firstName, String resetUrl) {
        String safeName = firstName == null || firstName.isBlank() ? "usuario" : firstName.trim();
        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="font-family: Arial, sans-serif; color: #1f2a37; line-height: 1.5;">
                  <p>Hola %s,</p>
                  <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en Mercado Libre Inventario.</p>
                  <p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 20px; background: #3483fa; color: #ffffff;
                              text-decoration: none; border-radius: 6px; font-weight: bold;">
                      Restablecer contraseña
                    </a>
                  </p>
                  <p>Si el boton no funciona, copia y pega este enlace en tu navegador:</p>
                  <p><a href="%s">%s</a></p>
                  <p>Este enlace expira en %d minutos. Si no solicitaste este cambio, ignora este correo.</p>
                  <p style="color: #6b7280; font-size: 12px;">Mercado Libre Inventario</p>
                </body>
                </html>
                """.formatted(safeName, resetUrl, resetUrl, resetUrl, expirationMinutes);
    }
}
