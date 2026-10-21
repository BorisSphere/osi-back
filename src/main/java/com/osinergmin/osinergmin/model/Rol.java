package com.osinergmin.osinergmin.model;

import static com.osinergmin.osinergmin.model.Permiso.AUDITORIA_VER;
import static com.osinergmin.osinergmin.model.Permiso.CATALOGO_GESTIONAR;
import static com.osinergmin.osinergmin.model.Permiso.CATALOGO_VER;
import static com.osinergmin.osinergmin.model.Permiso.ESTIMACION_GUARDAR;
import static com.osinergmin.osinergmin.model.Permiso.REQUERIMIENTO_APROBAR;
import static com.osinergmin.osinergmin.model.Permiso.REQUERIMIENTO_CREAR;
import static com.osinergmin.osinergmin.model.Permiso.REQUERIMIENTO_ELIMINAR;
import static com.osinergmin.osinergmin.model.Permiso.REQUERIMIENTO_MODIFICAR;
import static com.osinergmin.osinergmin.model.Permiso.USUARIO_ADMINISTRAR;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Roles del sistema y sus permisos (matriz de permisos). Se guardan como texto en
 * LAB_USUARIO.ROL y deben coincidir con la restricción CK_LAB_USUARIO_ROL.
 */
public enum Rol {
    ANALISTA(CATALOGO_VER, REQUERIMIENTO_CREAR, REQUERIMIENTO_MODIFICAR, REQUERIMIENTO_ELIMINAR,
            ESTIMACION_GUARDAR, AUDITORIA_VER),
    COORDINADOR(CATALOGO_VER, CATALOGO_GESTIONAR, REQUERIMIENTO_CREAR, REQUERIMIENTO_MODIFICAR,
            REQUERIMIENTO_ELIMINAR, AUDITORIA_VER, USUARIO_ADMINISTRAR),
    APROBADOR(CATALOGO_VER, REQUERIMIENTO_APROBAR, AUDITORIA_VER);

    private final Set<Permiso> permisos;

    Rol(Permiso... permisos) {
        this.permisos = Collections.unmodifiableSet(EnumSet.of(permisos[0], permisos));
    }

    public Set<Permiso> permisos() {
        return permisos;
    }
}
