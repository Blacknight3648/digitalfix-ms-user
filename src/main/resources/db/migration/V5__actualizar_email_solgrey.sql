-- ========================================================
-- V5: Corrección de email de Solgrey Medina (SUPERADMIN)
-- ========================================================
-- La versión V3 registró el email institucional 'sol.medina@duocuc.cl'.
-- La cuenta activa en Microsoft Entra ID es 'sol.medina@gmail.com'.
-- Este script corrige el email para que el matching en getOrProvisionCurrentUser
-- reconozca a Solgrey como SUPERADMIN desde su primer login.
UPDATE USUARIOS
SET    EMAIL              = 'sol.medina@gmail.com',
       FECHA_ACTUALIZACION = CURRENT_TIMESTAMP
WHERE  LOWER(EMAIL) = 'sol.medina@duocuc.cl';

