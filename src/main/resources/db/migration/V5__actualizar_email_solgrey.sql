-- ========================================================
-- V5: Corrección de email de Solgrey Medina (SUPERADMIN)
-- ========================================================
-- La versión V3 registró el email institucional 'sol.medina@duocuc.cl'.
-- La cuenta activa en Microsoft Entra ID es 'sol.medina@gmail.com'.
-- Este script actualiza el registro existente o lo inserta como SUPERADMIN
-- para que el matching en getOrProvisionCurrentUser reconozca a Solgrey.

ALTER SESSION DISABLE PARALLEL DML;

-- 1. Si existe con el correo duocuc.cl, actualizamos su email a gmail.com
UPDATE USUARIOS
SET    EMAIL               = 'sol.medina@gmail.com',
       ID_ROL              = (SELECT ID FROM ROLES WHERE CODIGO = 'SUPERADMIN'),
       FECHA_ACTUALIZACION = CURRENT_TIMESTAMP
WHERE  LOWER(EMAIL) = 'sol.medina@duocuc.cl';

-- 2. Asegurar que sol.medina@gmail.com exista con rol SUPERADMIN
MERGE INTO USUARIOS u
USING (
    SELECT 'sol.medina@gmail.com' AS EMAIL, 'SOLGREY' AS NOMBRE, 'NATASHA MARIA MEDINA LIRA' AS APELLIDO,
           'SUPERADMIN' AS CODIGO_ROL FROM DUAL
) s
ON (LOWER(u.EMAIL) = s.EMAIL)
WHEN MATCHED THEN
    UPDATE SET u.ID_ROL = (SELECT ID FROM ROLES WHERE CODIGO = s.CODIGO_ROL),
               u.FECHA_ACTUALIZACION = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (EMAIL, NOMBRE, APELLIDO, ID_EMPRESA, ID_ROL, ESTADO)
    VALUES (s.EMAIL, s.NOMBRE, s.APELLIDO,
            (SELECT ID FROM EMPRESAS WHERE RUT = 'DIGITALFIX'),
            (SELECT ID FROM ROLES WHERE CODIGO = s.CODIGO_ROL),
            'ACTIVO');
