package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.security.MailGraphProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

/** Envía las notificaciones fuera de la petición, solo después de confirmar la transacción. */
@Component
public class CorreoRequerimientoListener {

    private static final Logger log = LoggerFactory.getLogger(CorreoRequerimientoListener.class);

    private final MicrosoftGraphMailSender sender;
    private final MailGraphProperties properties;

    public CorreoRequerimientoListener(MicrosoftGraphMailSender sender, MailGraphProperties properties) {
        this.sender = sender;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(CorreoRequerimientoEvent evento) {
        if (!properties.enabled()) {
            log.info("Notificación {} de {} omitida: app.mail.enabled=false (defina MAIL_ENABLED=true)",
                    evento.tipo(), evento.codigo());
            return;
        }
        if (evento.destinatario() == null || evento.destinatario().isBlank()) {
            log.warn("No se envió correo para {}: el creador de {} no tiene correo registrado",
                    evento.tipo(), evento.codigo());
            return;
        }
        try {
            sender.enviar(evento.destinatario(), asunto(evento), renderizar(evento));
            log.info("Notificación {} enviada a {} para {}",
                    evento.tipo(), evento.destinatario(), evento.codigo());
        } catch (RuntimeException | IOException e) {
            log.error("No se pudo enviar la notificación {} para {} a {}",
                    evento.tipo(), evento.codigo(), evento.destinatario(), e);
        }
    }

    private static String asunto(CorreoRequerimientoEvent evento) {
        return switch (evento.tipo()) {
            case REGISTRADO -> "Requerimiento registrado: " + evento.codigo();
            case ESTIMADO -> "Requerimiento estimado: " + evento.codigo();
            case APROBADO -> "Requerimiento aprobado: " + evento.codigo();
            case RECHAZADO -> "Requerimiento rechazado: " + evento.codigo();
        };
    }

    private static String renderizar(CorreoRequerimientoEvent evento) throws IOException {
        String nombrePlantilla = evento.tipo().name().toLowerCase(java.util.Locale.ROOT);
        ClassPathResource resource = new ClassPathResource("templates/mail/" + nombrePlantilla + ".html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> valores = Map.ofEntries(
                Map.entry("codigo", escapar(evento.codigo())),
                Map.entry("aplicacion", escapar(evento.aplicacion())),
                Map.entry("titulo", escapar(evento.titulo())),
                Map.entry("descripcion", escapar(evento.descripcion())),
                Map.entry("prioridad", escapar(evento.prioridad())),
                Map.entry("fechaRequerida", escapar(evento.fechaRequerida() == null ? "No especificada" : evento.fechaRequerida())),
                Map.entry("detalleTecnico", escapar(evento.detalleTecnico() == null ? "No especificado" : evento.detalleTecnico())),
                Map.entry("estado", escapar(evento.estado())),
                Map.entry("creador", escapar(evento.creador())),
                Map.entry("actor", escapar(evento.actor() == null ? "No aplica" : evento.actor())),
                Map.entry("detalleAccion", escapar(evento.detalleAccion()).replace("\n", "<br>")),
                Map.entry("enlace", escapar(evento.enlace())));
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            html = html.replace("{{" + entrada.getKey() + "}}", entrada.getValue());
        }
        return html;
    }

    private static String escapar(String valor) {
        return HtmlUtils.htmlEscape(valor == null ? "" : valor);
    }
}
