# Diagrama entidad-relación

Entidades y relaciones persistidas en Oracle, esquema `APRENDIZAJE`. Los permisos se derivan de los roles en el código y no son tablas independientes.

```mermaid
erDiagram
    LAB_APLICACION {
        NUMBER ID PK
        VARCHAR2 CODIGO UK
        VARCHAR2 NOMBRE UK
        VARCHAR2 TECNOLOGIA_ACTUAL
        VARCHAR2 TECNOLOGIA_OBJETIVO
        VARCHAR2 DESCRIPCION
        NUMBER ACTIVO
    }

    LAB_USUARIO {
        NUMBER ID PK
        VARCHAR2 USERNAME UK
        VARCHAR2 PASSWORD_HASH
        VARCHAR2 ROL
        NUMBER ACTIVO
    }

    LAB_REQUERIMIENTO {
        NUMBER ID PK
        NUMBER APLICACION_ID FK
        VARCHAR2 TITULO
        VARCHAR2 DESCRIPCION
        VARCHAR2 PRIORIDAD
        VARCHAR2 ESTADO
        VARCHAR2 TIPO
        DATE FECHA
        VARCHAR2 DETALLE_TECNICO
        VARCHAR2 ARCHIVO_NOMBRE
        VARCHAR2 ARCHIVO_RUTA
        NUMBER CREADO_POR FK
        TIMESTAMP FECHA_CREACION
        TIMESTAMP FECHA_ACTUALIZACION
    }

    LAB_ESTIMACION {
        NUMBER ID PK
        NUMBER REQUERIMIENTO_ID FK, UK
        NUMBER HH_DESARROLLO
        NUMBER HH_ANALISIS
        NUMBER HH_PRUEBAS
        NUMBER HH_DOCUMENTACION
        NUMBER HH_TOTAL
        NUMBER ESTIMADO_POR FK
        TIMESTAMP FECHA_ESTIMACION
    }

    LAB_AUDITORIA {
        NUMBER ID PK
        NUMBER REQUERIMIENTO_ID FK
        NUMBER ACTOR_ID FK
        VARCHAR2 ACCION
        VARCHAR2 DETALLE
        TIMESTAMP FECHA
    }

    LAB_REFRESH_TOKEN {
        NUMBER ID PK
        NUMBER USUARIO_ID FK
        VARCHAR2 TOKEN_HASH UK
        TIMESTAMP FECHA_CREACION
        TIMESTAMP FECHA_EXPIRACION
        TIMESTAMP FECHA_USO
        NUMBER REVOCADO
    }

    LAB_APLICACION ||--o{ LAB_REQUERIMIENTO : "agrupa"
    LAB_USUARIO ||--o{ LAB_REQUERIMIENTO : "crea"
    LAB_REQUERIMIENTO ||--o| LAB_ESTIMACION : "tiene"
    LAB_USUARIO ||--o{ LAB_ESTIMACION : "estima"
    LAB_REQUERIMIENTO ||--o{ LAB_AUDITORIA : "registra eventos"
    LAB_USUARIO ||--o{ LAB_AUDITORIA : "actua"
    LAB_USUARIO ||--o{ LAB_REFRESH_TOKEN : "mantiene sesiones"
```

## Notas

- `PK` es clave primaria, `FK` es clave foránea y `UK` es una restricción única. `REQUERIMIENTO_ID` en `LAB_ESTIMACION` tiene ambas marcas: un requerimiento tiene cero o una estimación.
- `LAB_APLICACION.NOMBRE` tiene un índice único sobre `UPPER(NOMBRE)`, por lo que no se permiten nombres duplicados ignorando mayúsculas y minúsculas.
- Prioridad del requerimiento: `BAJA`, `MEDIA` o `ALTA`. Estado: `REGISTRADO`, `ESTIMADO`, `APROBADO` o `RECHAZADO`. Rol del usuario: `ANALISTA`, `COORDINADOR` o `APROBADOR`.
- `TOKEN_HASH` contiene el hash SHA-256 del refresh token, no el valor original entregado al navegador.
- Los tipos se muestran de forma resumida; consulta los scripts SQL para tamaños, precisión y restricciones completas.
