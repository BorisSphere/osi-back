package com.osinergmin.osinergmin.model;

/**
 * Acciones del sistema. Los endpoints exigen permisos (no roles) con
 * {@code @PreAuthorize("hasAuthority('...')")}; qué rol tiene cada permiso se define en {@link Rol}.
 */
public enum Permiso {
    /** Ver catálogo de aplicaciones, requerimientos y tablero. */
    CATALOGO_VER,
    /** Crear, editar y eliminar aplicaciones del catálogo. */
    CATALOGO_GESTIONAR,
    REQUERIMIENTO_CREAR,
    /** Modificar una solicitud abierta (el servicio valida el estado). */
    REQUERIMIENTO_MODIFICAR,
    REQUERIMIENTO_ELIMINAR,
    ESTIMACION_GUARDAR,
    /** Aprobar o rechazar un requerimiento. */
    REQUERIMIENTO_APROBAR,
    AUDITORIA_VER,
    /** Registrar y listar usuarios. */
    USUARIO_ADMINISTRAR
}
