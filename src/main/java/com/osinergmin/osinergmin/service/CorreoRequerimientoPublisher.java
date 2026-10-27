package com.osinergmin.osinergmin.service;

import com.osinergmin.osinergmin.model.Estimacion;
import com.osinergmin.osinergmin.model.Requerimiento;
import com.osinergmin.osinergmin.model.Usuario;
import com.osinergmin.osinergmin.util.CodigoRequerimiento;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Crea snapshots de notificación dentro de la transacción que modificó el requerimiento. */
@Component
public class CorreoRequerimientoPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final String frontUrl;

    public CorreoRequerimientoPublisher(
            ApplicationEventPublisher eventPublisher,
            @Value("${app.mail.front-url}") String frontUrl) {
        this.eventPublisher = eventPublisher;
        this.frontUrl = frontUrl;
    }

    public void registrado(Requerimiento requerimiento) {
        publicar(CorreoRequerimientoEvent.Tipo.REGISTRADO, requerimiento, null,
                "El requerimiento fue registrado correctamente.");
    }

    public void estimado(Requerimiento requerimiento, Estimacion estimacion) {
        String detalle = "Análisis: " + estimacion.getHhAnalisis()
                + " HH; desarrollo: " + estimacion.getHhDesarrollo()
                + " HH; pruebas: " + estimacion.getHhPruebas()
                + " HH; documentación: " + estimacion.getHhDocumentacion()
                + " HH; total: " + estimacion.getHhTotal() + " HH.";
        publicar(CorreoRequerimientoEvent.Tipo.ESTIMADO, requerimiento, estimacion.getEstimadoPor(), detalle);
    }

    public void decidido(
            Requerimiento requerimiento, Usuario aprobador, Estimacion estimacion, boolean rechazado, String motivo) {
        publicar(
                rechazado ? CorreoRequerimientoEvent.Tipo.RECHAZADO : CorreoRequerimientoEvent.Tipo.APROBADO,
                requerimiento,
                aprobador,
                rechazado ? motivo : "Estimación aprobada: " + estimacion.getHhTotal() + " HH.");
    }

    private void publicar(
            CorreoRequerimientoEvent.Tipo tipo,
            Requerimiento requerimiento,
            Usuario actor,
            String detalleAccion) {
        Usuario creador = requerimiento.getCreadoPor();
        String destinatario = creador.getCorreo();
        eventPublisher.publishEvent(new CorreoRequerimientoEvent(
                tipo,
                destinatario == null ? null : destinatario.trim().toLowerCase(Locale.ROOT),
                CodigoRequerimiento.formatear(requerimiento.getId()),
                requerimiento.getAplicacion().getNombre(),
                requerimiento.getTitulo(),
                requerimiento.getDescripcion(),
                requerimiento.getPrioridad().name(),
                requerimiento.getFechaRequerida() == null ? null : requerimiento.getFechaRequerida().toString(),
                requerimiento.getDetalleTecnico(),
                tipo.name(),
                creador.getUsername(),
                actor == null ? null : actor.getUsername(),
                detalleAccion,
                frontUrl.replaceAll("/+$", "") + "/requerimientos/" + requerimiento.getId()));
    }
}
