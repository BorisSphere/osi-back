-- LAB_APLICACION ya existe (ID, CODIGO, NOMBRE, TECNOLOGIA_ACTUAL, TECNOLOGIA_OBJETIVO).
-- La pantalla "Aplicaciones" necesita además descripción y estado.
-- Ejecutar en el esquema APRENDIZAJE: spring.jpa.hibernate.ddl-auto=none, Hibernate no modifica tablas.

-- Nullable porque las filas existentes no tienen descripción; el backend la exige al crear o editar.
ALTER TABLE LAB_APLICACION ADD (DESCRIPCION VARCHAR2(500 CHAR));

-- Las filas existentes quedan activas.
ALTER TABLE LAB_APLICACION ADD (
    ACTIVO NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT CK_LAB_APLICACION_ACTIVO CHECK (ACTIVO IN (0, 1))
);

-- Nombre único sin distinguir mayúsculas (respaldo de la validación del backend).
CREATE UNIQUE INDEX UQ_LAB_APLICACION_NOMBRE ON LAB_APLICACION (UPPER(NOMBRE));
