package com.mercado_libre.gestion_productos.config;

import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
@Configuration
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    @Primary
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username}") String username,
            @Value("${spring.mail.password}") String password
    ) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.replaceAll("\\s+", "");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(normalizedUsername);
        sender.setPassword(normalizedPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (host != null && host.contains("gmail") && normalizedUsername.endsWith("@ucp.edu.co")) {
            log.warn(
                    "MAIL_USERNAME es @ucp.edu.co con servidor Gmail. Solo funciona si la universidad usa Google Workspace "
                            + "y generaste una contrasena de aplicacion en ESA cuenta (no la contrasena del portal UCP)."
            );
        }

        if (normalizedPassword.length() > 0 && normalizedPassword.length() != 16 && host != null && host.contains("gmail")) {
            log.warn(
                    "MAIL_APP_PASSWORD tiene {} caracteres. Las contrasenas de aplicacion de Google suelen tener exactamente 16.",
                    normalizedPassword.length()
            );
        }

        log.info("SMTP listo: host={}, usuario={}", host, normalizedUsername);
        return sender;
    }
}
