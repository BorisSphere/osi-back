package com.osinergmin.osinergmin.model;

import static com.osinergmin.osinergmin.model.Permiso.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/** Fija la matriz de permisos acordada: si alguien la cambia, este test lo hace explícito. */
class RolTest {

    @Test
    void analista() {
        assertEquals(EnumSet.of(CATALOGO_VER, REQUERIMIENTO_CREAR, REQUERIMIENTO_MODIFICAR, REQUERIMIENTO_ELIMINAR,
                ESTIMACION_GUARDAR, AUDITORIA_VER), Rol.ANALISTA.permisos());
    }

    @Test
    void coordinador() {
        assertEquals(EnumSet.of(CATALOGO_VER, CATALOGO_GESTIONAR, REQUERIMIENTO_CREAR, REQUERIMIENTO_MODIFICAR,
                REQUERIMIENTO_ELIMINAR, AUDITORIA_VER, USUARIO_ADMINISTRAR), Rol.COORDINADOR.permisos());
    }

    @Test
    void aprobador() {
        assertEquals(EnumSet.of(CATALOGO_VER, REQUERIMIENTO_APROBAR, AUDITORIA_VER), Rol.APROBADOR.permisos());
    }
}
