-- ========================================================
-- V3: Rol SUPERADMIN y asignación inicial de roles
-- ========================================================
-- SUPERADMIN: equipo a cargo de la plataforma. Puede ver todas las funciones y
-- administrar todo; en los servicios hereda los permisos de todos los demás
-- roles (jerarquía de roles en SecurityConfig de ms-user y ms-login).

-- Oracle Autonomous ejecuta los MERGE con DML en paralelo; en esa modalidad la
-- misma transacción no puede volver a leer ROLES después de modificarla
-- (ORA-12839), y el segundo MERGE la consulta para obtener el ID del rol.
ALTER SESSION DISABLE PARALLEL DML;

MERGE INTO ROLES r
USING (
    SELECT 'SUPERADMIN' AS CODIGO, 'Superadministrador' AS NOMBRE,
           'Acceso total: todas las funciones de todos los roles y gestión de usuarios y roles' AS DESCRIPCION
    FROM DUAL
) s
ON (r.CODIGO = s.CODIGO)
WHEN MATCHED THEN
    UPDATE SET r.NOMBRE = s.NOMBRE, r.DESCRIPCION = s.DESCRIPCION
WHEN NOT MATCHED THEN
    INSERT (CODIGO, NOMBRE, DESCRIPCION) VALUES (s.CODIGO, s.NOMBRE, s.DESCRIPCION);

-- Asignación por correo. Si el usuario ya inició sesión (alta automática como
-- CLIENTE), se le cambia el rol; si aún no existe, se pre-crea sin OID y ms-user
-- lo vincula por correo en su primer login (conservando este rol).
MERGE INTO USUARIOS u
USING (
    SELECT 'sol.medina@duocuc.cl' AS EMAIL, 'SOLGREY' AS NOMBRE, 'NATASHA MARIA MEDINA LIRA' AS APELLIDO,
           'SUPERADMIN' AS CODIGO_ROL FROM DUAL
    UNION ALL
    SELECT 'cr.ossesg@duocuc.cl', 'CRISTOPHER', 'OSSES GUERRA', 'SUPERADMIN' FROM DUAL
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
