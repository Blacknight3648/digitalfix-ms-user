-- ========================================================
-- V2: Catálogo de roles del caso DigitalFix y alta de usuarios
-- ========================================================
-- Roles del enunciado: Admin, Supervisor (Operador), Cliente y Auditor.
-- TECNICO se mantiene porque ya existía en V1 (ejecutor en terreno).
-- MERGE deja la migración idempotente: actualiza los roles sembrados en V1 y
-- agrega los que faltan, sin duplicar ni romper las FK de USUARIOS.
MERGE INTO ROLES r
USING (
    SELECT 'ADMIN' AS CODIGO, 'Administrador' AS NOMBRE,
           'Gestión total de la plataforma, usuarios y roles' AS DESCRIPCION FROM DUAL
    UNION ALL
    SELECT 'SUPERVISOR', 'Supervisor (Operador)',
           'Asigna y gestiona órdenes de trabajo y cuadrillas' FROM DUAL
    UNION ALL
    SELECT 'TECNICO', 'Técnico Electromecánico',
           'Ejecuta las órdenes de trabajo en terreno' FROM DUAL
    UNION ALL
    SELECT 'CLIENTE', 'Cliente',
           'Crea y consulta sus solicitudes y reportes' FROM DUAL
    UNION ALL
    SELECT 'AUDITOR', 'Auditor',
           'Consulta de solo lectura de auditoría y timeline de órdenes' FROM DUAL
) s
ON (r.CODIGO = s.CODIGO)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE = s.NOMBRE, r.DESCRIPCION = s.DESCRIPCION
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, DESCRIPCION) VALUES (s.CODIGO, s.NOMBRE, s.DESCRIPCION);

-- Empresa interna: a ella quedan asociados los usuarios que se dan de alta
-- solos en su primer login, hasta que un ADMIN los asigne a su PyME.
MERGE INTO EMPRESAS e
USING (SELECT 'DIGITALFIX' AS RUT FROM DUAL) s
ON (e.RUT = s.RUT)
WHEN NOT MATCHED THEN
    INSERT (RAZON_SOCIAL, RUT, TIPO_EMPRESA, ESTADO)
    VALUES ('DigitalFix (interna)', 'DIGITALFIX', 'INTERNA', 'ACTIVO');

-- Permite pre-asignar roles por correo antes del primer login: el usuario se
-- crea sin OID y, cuando entra por primera vez, ms-user lo vincula por email.
-- En Oracle un UNIQUE admite varios NULL, así que UK_USUARIO_ENTRA_ID se mantiene.
ALTER TABLE USUARIOS MODIFY (USER_ENTRA_ID NULL);
