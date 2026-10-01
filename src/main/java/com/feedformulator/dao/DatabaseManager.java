package com.feedformulator.dao;

import java.io.File;
import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    // ============================================================
    //  CONFIGURACIÓN DE RUTA DE BASE DE DATOS
    //  El usuario puede editar este archivo para apuntar a la red
    // ============================================================
    private static final String CONFIG_FILE = System.getenv("APPDATA") + 
        File.separator + "FeedFormulatorPro" + File.separator + "dbconfig.properties";
    
    // Ruta por defecto (local)
    private static String DB_FOLDER = System.getenv("APPDATA") + 
        File.separator + "FeedFormulatorPro";
    
    private static String DB_NAME = "feedformulator.db";
    private static String DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + DB_NAME;
    
    // ============================================================
    //  CARGAR CONFIGURACIÓN DESDE ARCHIVO
    // ============================================================
    static {
        cargarConfiguracion();
    }
    
    private static void cargarConfiguracion() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                Properties props = new Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                }
                
                String rutaConfig = props.getProperty("db.path");
                if (rutaConfig != null && !rutaConfig.trim().isEmpty()) {
                    DB_FOLDER = rutaConfig.trim();
                    DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + DB_NAME;
                    System.out.println("📁 Base de datos en: " + DB_FOLDER);
                    return;
                }
            }
            
            // Si no hay configuración, crear archivo por defecto
            crearArchivoConfiguracion();
            
        } catch (Exception e) {
            System.err.println("⚠️ Error al cargar configuración: " + e.getMessage());
        }
    }
    
    private static void crearArchivoConfiguracion() {
        try {
            File configFile = new File(CONFIG_FILE);
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            Properties props = new Properties();
            props.setProperty("db.path", DB_FOLDER);
            props.setProperty("db.name", DB_NAME);
            props.setProperty("# INSTRUCCIONES", "============================================");
            props.setProperty("# Para usar en RED, cambie db.path a la ruta compartida", "");
            props.setProperty("# Ejemplo para red:", "db.path = \\\\PC-ADMIN\\FeedData");
            props.setProperty("# Ejemplo para local:", "db.path = C:\\Users\\TuUsuario\\AppData\\Roaming\\FeedFormulatorPro");
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(configFile)) {
                props.store(fos, "Feed Formulator Pro - Configuración de Base de Datos");
            }
            
            System.out.println("📄 Archivo de configuración creado: " + CONFIG_FILE);
            
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo crear archivo de configuración: " + e.getMessage());
        }
    }
    
    // ============================================================
    //  MÉTODOS PARA CONFIGURACIÓN DE RED
    // ============================================================
    
    public static void cambiarRutaBaseDatos(String nuevaRuta) throws Exception {
        if (nuevaRuta == null || nuevaRuta.trim().isEmpty()) {
            throw new Exception("La ruta no puede estar vacía");
        }
        
        File folder = new File(nuevaRuta);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new Exception("No se pudo crear la carpeta: " + nuevaRuta);
            }
        }
        
        Properties props = new Properties();
        props.setProperty("db.path", nuevaRuta);
        props.setProperty("db.name", DB_NAME);
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Feed Formulator Pro - Configuración de Base de Datos");
        }
        
        DB_FOLDER = nuevaRuta;
        DB_URL = "jdbc:sqlite:" + DB_FOLDER + File.separator + DB_NAME;
        
        System.out.println("✅ Ruta de base de datos actualizada a: " + nuevaRuta);
    }
    
    public static String getRutaActual() {
        return DB_FOLDER;
    }
    
    public static String getRutaCompleta() {
        return DB_FOLDER + File.separator + DB_NAME;
    }
    
    public static String getCarpetaImagenesMateriales() {
        String carpeta = DB_FOLDER + File.separator + "imagenes_materiales";
        File dir = new File(carpeta);
        if (!dir.exists()) dir.mkdirs();
        return carpeta;
    }

    public static void cerrarTodasLasConexiones() {
        // No-op
    }

    public static Connection getConnection() throws SQLException {
        long t0 = System.currentTimeMillis();
        cerrarTodasLasConexiones();
        
        File folder = new File(DB_FOLDER);
        if (!folder.exists()) {
            boolean creado = folder.mkdirs();
            System.out.println("📁 Creando carpeta: " + DB_FOLDER + " - " + (creado ? "OK" : "FALLÓ"));
        }
        
        Properties props = new Properties();
        props.setProperty("busy_timeout", "30000");
        
        long t1 = System.currentTimeMillis();
        Connection conn = DriverManager.getConnection(DB_URL, props);
        long t2 = System.currentTimeMillis();
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA busy_timeout = 30000");
            stmt.execute("PRAGMA temp_store = MEMORY");
        }
        conn.setAutoCommit(true);
        
        long t3 = System.currentTimeMillis();
        long total = t3 - t0;
        if (total > 100) {
            System.out.println("⏱️ getConnection() tardó " + total + "ms (folder:" + (t1 - t0) + "ms, driver:" + (t2 - t1) + "ms, pragmas:" + (t3 - t2) + "ms)");
        }
        return conn;
    }

    public static void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignorar */ }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignorar */ }
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException e) { /* ignorar */ }
    }

    public static void closeResources(Connection conn, Statement stmt) {
        closeResources(conn, stmt, null);
    }

    public static void closeResources(Connection conn) {
        closeResources(conn, null, null);
    }

    public static void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            System.out.println("=========================================");
            System.out.println("📁 RUTA DE LA BASE DE DATOS:");
            System.out.println("   " + DB_FOLDER + File.separator + DB_NAME);
            System.out.println("=========================================");
            
            File folder = new File(DB_FOLDER);
            if (!folder.exists()) {
                boolean creado = folder.mkdirs();
                System.out.println("📁 Creando carpeta: " + DB_FOLDER + " - " + (creado ? "OK" : "FALLÓ"));
            }
            
            conn = DriverManager.getConnection(DB_URL);
            stmt = conn.createStatement();
            
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA synchronous = OFF");
            stmt.execute("PRAGMA journal_mode = MEMORY");
            
            // ========== TABLAS EXISTENTES ==========
            crearTablasExistentes(stmt);
            
            // ========== TABLAS DE CLIENTES Y CRÉDITOS ==========
            crearTablasClientes(stmt);
            crearTablasCreditos(stmt);
            
            // ========== TABLAS DE PROVEEDORES ==========
            crearTablasProveedores(stmt);
            
            // ========== SISTEMA DE PEDIDOS ==========
            crearTablasPedidos(stmt);
            
            // ========== TABLA: AUDITORÍA ==========
            crearTablaAuditoria(stmt);
            
            // ========== TABLA: USUARIOS ==========
            crearTablaUsuarios(stmt);
            
            // ========== TABLA: HISTORIAL PRECIOS ==========
            crearTablaHistorialPrecios(stmt);
            
            // ========== MIGRACIONES ==========
            aplicarMigraciones(stmt);
            
            // ========== FORZAR MIGRACIÓN DE ORIGEN ==========
            forzarMigracionOrigen(stmt);
            
            // ========== CREAR ÍNDICES ==========
            crearIndices(stmt);
            
            System.out.println("✅ Base de datos inicializada correctamente en: " + DB_FOLDER);
            
        } catch (SQLException e) {
            System.err.println("❌ Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt);
        }
    }

    private static void forzarMigracionOrigen(Statement stmt) throws SQLException {
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(materias_primas)");
            boolean existeOrigen = false;
            while (rs.next()) {
                if ("origen".equals(rs.getString("name"))) {
                    existeOrigen = true;
                    break;
                }
            }
            rs.close();
            
            if (!existeOrigen) {
                System.out.println("⚠️ Forzando migración: agregando columna origen a materias_primas...");
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN origen TEXT DEFAULT 'INVENTARIO'");
                System.out.println("✅ Columna origen agregada a materias_primas");
                
                stmt.execute("UPDATE materias_primas SET origen = 'PEDIDO' " +
                             "WHERE id IN (SELECT DISTINCT materia_prima_id FROM detalle_pedido)");
                System.out.println("✅ Productos con pedidos asociados marcados como 'PEDIDO'");
            } else {
                System.out.println("✅ Columna origen ya existe en materias_primas");
            }
            
            rs = stmt.executeQuery("PRAGMA table_info(ingresos_historial)");
            boolean existeOrigenIngreso = false;
            while (rs.next()) {
                if ("origen".equals(rs.getString("name"))) {
                    existeOrigenIngreso = true;
                    break;
                }
            }
            rs.close();
            
            if (!existeOrigenIngreso) {
                System.out.println("⚠️ Forzando migración: agregando columna origen a ingresos_historial...");
                stmt.execute("ALTER TABLE ingresos_historial ADD COLUMN origen TEXT DEFAULT 'INVENTARIO'");
                System.out.println("✅ Columna origen agregada a ingresos_historial");
            } else {
                System.out.println("✅ Columna origen ya existe en ingresos_historial");
            }
            
        } catch (SQLException e) {
            System.err.println("⚠️ Error en forzarMigracionOrigen: " + e.getMessage());
            try {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN origen TEXT DEFAULT 'INVENTARIO'");
                System.out.println("✅ Columna origen agregada a materias_primas (segundo intento)");
            } catch (SQLException e2) {
                System.out.println("ℹ️ La columna origen ya existe o no se pudo agregar: " + e2.getMessage());
            }
        }
    }

    private static void crearIndices(Statement stmt) throws SQLException {
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_materias_primas_nombre ON materias_primas(nombre)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_materias_primas_codigo ON materias_primas(codigo)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_materias_primas_origen ON materias_primas(origen)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_materia_prima_id ON movimientos_inventario(materia_prima_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_inventario(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_inventario(tipo_movimiento)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_pedido_id ON movimientos_inventario(pedido_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_compra_id ON movimientos_inventario(compra_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_formulaciones_estado ON formulaciones(estado)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_formulaciones_fecha ON formulaciones(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_formulacion_detalle_formulacion_id ON formulacion_detalle(formulacion_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_formulacion_detalle_materia_prima_id ON formulacion_detalle(materia_prima_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_formulacion_id ON ventas(formulacion_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_codigo ON clientes(codigo)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_estado ON clientes(estado)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_credito_cliente_id ON ventas_credito(cliente_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_credito_estado ON ventas_credito(estado)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_credito_fecha_venta ON ventas_credito(fecha_venta)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_credito_fecha_limite ON ventas_credito(fecha_limite_pago)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_credito_venta_id ON ventas_credito(venta_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_abonos_venta_credito_id ON abonos(venta_credito_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_abonos_cliente_id ON abonos(cliente_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_abonos_fecha_abono ON abonos(fecha_abono)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_proveedores_nombre ON proveedores(nombre)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_proveedores_codigo ON proveedores(codigo)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_proveedores_estado ON proveedores(estado)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pedidos_proveedor_id ON pedidos(proveedor_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pedidos_estado_pago ON pedidos(estado_pago)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pedidos_estado_recepcion ON pedidos(estado_recepcion)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pedidos_fecha_pedido ON pedidos(fecha_pedido)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_detalle_pedido_pedido_id ON detalle_pedido(pedido_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_detalle_pedido_materia_prima_id ON detalle_pedido(materia_prima_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_pedido_pedido_id ON pagos_pedido(pedido_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_pedido_proveedor_id ON pagos_pedido(proveedor_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ingresos_historial_producto_id ON ingresos_historial(producto_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ingresos_historial_fecha_ingreso ON ingresos_historial(fecha_ingreso)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ingresos_historial_pedido_id ON ingresos_historial(pedido_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ingresos_historial_compra_id ON ingresos_historial(compra_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_compras_proveedores_proveedor_id ON compras_proveedores(proveedor_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_compras_proveedores_materia_prima_id ON compras_proveedores(materia_prima_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_compras_proveedores_estado_pago ON compras_proveedores(estado_pago)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_proveedores_compra_id ON pagos_proveedores(compra_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_proveedores_proveedor_id ON pagos_proveedores(proveedor_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auditoria_tabla ON auditoria(tabla)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auditoria_registro_id ON auditoria(registro_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_auditoria_accion ON auditoria(accion)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_historial_precios_producto_id ON historial_precios(producto_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_historial_precios_fecha ON historial_precios(fecha)");
        
        System.out.println("✅ Índices creados correctamente");
    }

    private static void crearTablasExistentes(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS materias_primas (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "codigo TEXT UNIQUE," +
            "nombre TEXT UNIQUE NOT NULL," +
            "unidad TEXT NOT NULL," +
            "cantidad_total REAL NOT NULL," +
            "cantidad_disponible REAL NOT NULL," +
            "precio_total REAL NOT NULL," +
            "precio_por_unidad REAL NOT NULL," +
            "fecha_ingreso TEXT NOT NULL," +
            "hora_ingreso TEXT NOT NULL," +
            "peso_saco REAL," +
            "numero_sacos INTEGER," +
            "libras_sueltas REAL DEFAULT 0," +
            "origen TEXT DEFAULT 'INVENTARIO'," +
            "imagen_path TEXT)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS movimientos_inventario (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "materia_prima_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "unidad TEXT NOT NULL," +
            "tipo_movimiento TEXT NOT NULL," +
            "cantidad REAL NOT NULL," +
            "precio_por_unidad REAL NOT NULL," +
            "subtotal REAL NOT NULL," +
            "sacos_afectados INTEGER," +
            "libras_sueltas_afectadas REAL," +
            "fecha TEXT NOT NULL," +
            "hora TEXT NOT NULL," +
            "referencia TEXT," +
            "pedido_id INTEGER," +
            "compra_id INTEGER," +
            "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS formulaciones (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "nombre TEXT NOT NULL," +
            "total_libras REAL NOT NULL," +
            "costo_total REAL NOT NULL," +
            "fecha TEXT NOT NULL," +
            "hora TEXT NOT NULL," +
            "estado TEXT DEFAULT 'Disponible')"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS formulacion_detalle (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "formulacion_id INTEGER NOT NULL," +
            "materia_prima_id INTEGER NOT NULL," +
            "nombre_ingrediente TEXT NOT NULL," +
            "unidad TEXT NOT NULL," +
            "cantidad_usada REAL NOT NULL," +
            "precio_unitario REAL NOT NULL," +
            "subtotal REAL NOT NULL," +
            "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE," +
            "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS ventas (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "formulacion_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "libras_vendidas REAL NOT NULL," +
            "costo_inversion REAL NOT NULL," +
            "precio_venta REAL NOT NULL," +
            "ganancia REAL NOT NULL," +
            "fecha TEXT NOT NULL," +
            "hora TEXT NOT NULL," +
            "forma_pago TEXT DEFAULT 'Efectivo'," +
            "cliente_id INTEGER DEFAULT 0," +
            "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS contabilidad_movimientos (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "tipo TEXT NOT NULL," +
            "categoria TEXT," +
            "descripcion TEXT," +
            "monto REAL NOT NULL," +
            "fecha TEXT NOT NULL," +
            "estado TEXT DEFAULT 'ACTIVO'," +
            "notas TEXT)"
        );

        // Espacio libre de notas/borrador para Contabilidad (una sola fila,
        // id=1). Totalmente aparte de todo lo demás: nunca se lee ni se
        // toma en cuenta para ningún cálculo del sistema.
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS contabilidad_notas (" +
            "id INTEGER PRIMARY KEY," +
            "texto TEXT)"
        );

        stmt.execute(
            "CREATE TABLE IF NOT EXISTS ingresos_historial (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "producto_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "unidad TEXT NOT NULL," +
            "cantidad REAL NOT NULL," +
            "precio_unitario REAL NOT NULL," +
            "precio_total REAL NOT NULL," +
            "numero_sacos INTEGER," +
            "peso_saco REAL," +
            "libras_sueltas REAL," +
            "fecha_ingreso TEXT NOT NULL," +
            "hora_ingreso TEXT NOT NULL," +
            "pedido_id INTEGER," +
            "compra_id INTEGER," +
            "cantidad_usada REAL DEFAULT 0," +
            "origen TEXT DEFAULT 'INVENTARIO'," +
            "FOREIGN KEY (producto_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );
    }

    private static void crearTablasClientes(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS clientes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "codigo TEXT UNIQUE," +
            "nombre TEXT NOT NULL," +
            "telefono TEXT," +
            "email TEXT," +
            "direccion TEXT," +
            "ruc_cedula TEXT," +
            "fecha_registro TEXT NOT NULL," +
            "estado TEXT DEFAULT 'ACTIVO')"
        );
    }

    private static void crearTablasCreditos(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS ventas_credito (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "venta_id INTEGER NOT NULL," +
            "cliente_id INTEGER NOT NULL," +
            "formulacion_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "total_libras REAL NOT NULL," +
            "precio_total REAL NOT NULL," +
            "abonado REAL DEFAULT 0," +
            "saldo_pendiente REAL NOT NULL," +
            "estado TEXT DEFAULT 'PENDIENTE'," +
            "fecha_venta TEXT NOT NULL," +
            "fecha_limite_pago TEXT," +
            "notas TEXT," +
            "ganancia_diferida REAL DEFAULT 0," +
            "ganancia_realizada REAL DEFAULT 0," +
            "FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE," +
            "FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE," +
            "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS abonos (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "venta_credito_id INTEGER NOT NULL," +
            "cliente_id INTEGER NOT NULL," +
            "monto REAL NOT NULL," +
            "forma_pago TEXT DEFAULT 'EFECTIVO'," +
            "fecha_abono TEXT NOT NULL," +
            "hora_abono TEXT NOT NULL," +
            "referencia TEXT," +
            "FOREIGN KEY (venta_credito_id) REFERENCES ventas_credito(id) ON DELETE CASCADE," +
            "FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE)"
        );
    }

    private static void crearTablasProveedores(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS proveedores (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "codigo TEXT UNIQUE," +
            "nombre TEXT NOT NULL," +
            "telefono TEXT," +
            "email TEXT," +
            "direccion TEXT," +
            "ruc_cedula TEXT," +
            "contacto TEXT," +
            "tipo_proveedor TEXT DEFAULT 'MATERIA_PRIMA'," +
            "fecha_registro TEXT NOT NULL," +
            "estado TEXT DEFAULT 'ACTIVO')"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS compras_proveedores (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "proveedor_id INTEGER NOT NULL," +
            "materia_prima_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "unidad TEXT NOT NULL," +
            "cantidad REAL NOT NULL," +
            "precio_unitario REAL NOT NULL," +
            "precio_total REAL NOT NULL," +
            "numero_sacos INTEGER," +
            "peso_saco REAL," +
            "libras_sueltas REAL," +
            "estado_pago TEXT DEFAULT 'PENDIENTE'," +
            "estado_recepcion TEXT DEFAULT 'PENDIENTE'," +
            "abonado REAL DEFAULT 0," +
            "saldo_pendiente REAL NOT NULL," +
            "fecha_compra TEXT NOT NULL," +
            "fecha_limite_pago TEXT," +
            "notas TEXT," +
            "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE," +
            "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );
        
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS pagos_proveedores (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "compra_id INTEGER NOT NULL," +
            "proveedor_id INTEGER NOT NULL," +
            "monto REAL NOT NULL," +
            "forma_pago TEXT DEFAULT 'EFECTIVO'," +
            "fecha_pago TEXT NOT NULL," +
            "hora_pago TEXT NOT NULL," +
            "referencia TEXT," +
            "FOREIGN KEY (compra_id) REFERENCES compras_proveedores(id) ON DELETE CASCADE," +
            "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE)"
        );
    }

    private static void crearTablasPedidos(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS pedidos (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "proveedor_id INTEGER NOT NULL," +
            "fecha_pedido TEXT NOT NULL," +
            "estado_pago TEXT DEFAULT 'PENDIENTE'," +
            "estado_recepcion TEXT DEFAULT 'PENDIENTE'," +
            "abonado REAL DEFAULT 0," +
            "saldo_pendiente REAL NOT NULL," +
            "notas TEXT," +
            "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE)"
        );

        stmt.execute(
            "CREATE TABLE IF NOT EXISTS detalle_pedido (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "pedido_id INTEGER NOT NULL," +
            "materia_prima_id INTEGER NOT NULL," +
            "nombre_producto TEXT NOT NULL," +
            "unidad TEXT NOT NULL," +
            "cantidad REAL NOT NULL," +
            "precio_unitario REAL NOT NULL," +
            "subtotal REAL NOT NULL," +
            "numero_sacos INTEGER," +
            "peso_saco REAL," +
            "libras_sueltas REAL," +
            "FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE," +
            "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );

        stmt.execute(
            "CREATE TABLE IF NOT EXISTS pagos_pedido (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "pedido_id INTEGER NOT NULL," +
            "proveedor_id INTEGER NOT NULL," +
            "monto REAL NOT NULL," +
            "forma_pago TEXT DEFAULT 'EFECTIVO'," +
            "fecha_pago TEXT NOT NULL," +
            "hora_pago TEXT NOT NULL," +
            "referencia TEXT," +
            "FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE," +
            "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE)"
        );
    }

    private static void crearTablaAuditoria(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS auditoria (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "usuario TEXT DEFAULT 'Sistema'," +
            "accion TEXT NOT NULL," +
            "tabla TEXT NOT NULL," +
            "registro_id INTEGER," +
            "datos_anteriores TEXT," +
            "datos_nuevos TEXT," +
            "fecha TEXT NOT NULL," +
            "hora TEXT NOT NULL," +
            "ip TEXT" +
            ")"
        );
    }

    private static void crearTablaUsuarios(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS usuarios (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "nombre TEXT NOT NULL," +
            "usuario TEXT UNIQUE NOT NULL," +
            "contrasena TEXT NOT NULL," +
            "rol TEXT DEFAULT 'USUARIO'," +
            "estado TEXT DEFAULT 'ACTIVO'," +
            "fecha_registro TEXT NOT NULL)"
        );

        try {
            stmt.execute(
                "INSERT OR IGNORE INTO usuarios (nombre, usuario, contrasena, rol, estado, fecha_registro) " +
                "VALUES ('Administrador', 'admin', 'admin123', 'ADMIN', 'ACTIVO', datetime('now'))"
            );
            System.out.println("✅ Usuario admin creado por defecto (admin/admin123)");
        } catch (SQLException e) {
            // Usuario ya existe
        }
    }

    private static void crearTablaHistorialPrecios(Statement stmt) throws SQLException {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS historial_precios (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "producto_id INTEGER NOT NULL," +
            "precio REAL NOT NULL," +
            "fecha TEXT NOT NULL," +
            "FOREIGN KEY (producto_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
        );
    }

    private static void aplicarMigraciones(Statement stmt) throws SQLException {
        // ===== MIGRACIÓN 1: formulaciones.estado =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(formulaciones)");
            boolean existeEstado = false;
            while (rs.next()) {
                if ("estado".equals(rs.getString("name"))) {
                    existeEstado = true;
                    break;
                }
            }
            rs.close();
            if (!existeEstado) {
                stmt.execute("ALTER TABLE formulaciones ADD COLUMN estado TEXT DEFAULT 'Disponible'");
                System.out.println("✅ Migración: estado agregado a formulaciones");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 2: materias_primas columnas =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(materias_primas)");
            boolean existePesoSaco = false, existeNumeroSacos = false, existeLibrasSueltas = false, existeCodigo = false;
            boolean existeOrigen = false;
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("peso_saco".equals(colName)) existePesoSaco = true;
                if ("numero_sacos".equals(colName)) existeNumeroSacos = true;
                if ("libras_sueltas".equals(colName)) existeLibrasSueltas = true;
                if ("codigo".equals(colName)) existeCodigo = true;
                if ("origen".equals(colName)) existeOrigen = true;
            }
            rs.close();
            
            if (!existeCodigo) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN codigo TEXT");
                stmt.execute("UPDATE materias_primas SET codigo = 'MAT-' || printf('%03d', id) WHERE codigo IS NULL");
                System.out.println("✅ Migración: codigo agregado a materias_primas");
            }
            if (!existePesoSaco) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN peso_saco REAL");
                System.out.println("✅ Migración: peso_saco agregado a materias_primas");
            }
            if (!existeNumeroSacos) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN numero_sacos INTEGER");
                System.out.println("✅ Migración: numero_sacos agregado a materias_primas");
            }
            if (!existeLibrasSueltas) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN libras_sueltas REAL DEFAULT 0");
                System.out.println("✅ Migración: libras_sueltas agregado a materias_primas");
            }
            
            if (!existeOrigen) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN origen TEXT DEFAULT 'INVENTARIO'");
                System.out.println("✅ Migración: origen agregado a materias_primas");
                
                stmt.execute("UPDATE materias_primas SET origen = 'PEDIDO' " +
                             "WHERE id IN (SELECT DISTINCT materia_prima_id FROM detalle_pedido)");
                System.out.println("✅ Productos con pedidos asociados marcados como 'PEDIDO'");
            }
        } catch (SQLException e) { 
            System.out.println("⚠️ No se pudo migrar materias_primas: " + e.getMessage());
        }

        // ===== MIGRACIÓN 3: ingresos_historial.cantidad_usada =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(ingresos_historial)");
            boolean existeCantidadUsada = false;
            while (rs.next()) {
                if ("cantidad_usada".equals(rs.getString("name"))) {
                    existeCantidadUsada = true;
                    break;
                }
            }
            rs.close();
            if (!existeCantidadUsada) {
                stmt.execute("ALTER TABLE ingresos_historial ADD COLUMN cantidad_usada REAL DEFAULT 0");
                System.out.println("✅ Migración: cantidad_usada agregado a ingresos_historial");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 4: compras_proveedores.estado_recepcion =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(compras_proveedores)");
            boolean existeEstadoRecepcion = false;
            while (rs.next()) {
                if ("estado_recepcion".equals(rs.getString("name"))) {
                    existeEstadoRecepcion = true;
                    break;
                }
            }
            rs.close();
            if (!existeEstadoRecepcion) {
                stmt.execute("ALTER TABLE compras_proveedores ADD COLUMN estado_recepcion TEXT DEFAULT 'PENDIENTE'");
                System.out.println("✅ Migración: estado_recepcion agregado a compras_proveedores");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 5: movimientos_inventario.pedido_id =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(movimientos_inventario)");
            boolean existePedidoId = false;
            while (rs.next()) {
                if ("pedido_id".equals(rs.getString("name"))) {
                    existePedidoId = true;
                    break;
                }
            }
            rs.close();
            if (!existePedidoId) {
                stmt.execute("ALTER TABLE movimientos_inventario ADD COLUMN pedido_id INTEGER");
                System.out.println("✅ Migración: pedido_id agregado a movimientos_inventario");
            }
        } catch (SQLException e) { 
            System.out.println("⚠️ No se pudo migrar movimientos_inventario.pedido_id: " + e.getMessage());
        }

        // ===== MIGRACIÓN 6: movimientos_inventario.compra_id =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(movimientos_inventario)");
            boolean existeCompraId = false;
            while (rs.next()) {
                if ("compra_id".equals(rs.getString("name"))) {
                    existeCompraId = true;
                    break;
                }
            }
            rs.close();
            if (!existeCompraId) {
                stmt.execute("ALTER TABLE movimientos_inventario ADD COLUMN compra_id INTEGER");
                System.out.println("✅ Migración: compra_id agregado a movimientos_inventario");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 7: ingresos_historial.pedido_id =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(ingresos_historial)");
            boolean existePedidoId = false;
            while (rs.next()) {
                if ("pedido_id".equals(rs.getString("name"))) {
                    existePedidoId = true;
                    break;
                }
            }
            rs.close();
            if (!existePedidoId) {
                stmt.execute("ALTER TABLE ingresos_historial ADD COLUMN pedido_id INTEGER");
                System.out.println("✅ Migración: pedido_id agregado a ingresos_historial");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 8: ingresos_historial.compra_id =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(ingresos_historial)");
            boolean existeCompraId = false;
            while (rs.next()) {
                if ("compra_id".equals(rs.getString("name"))) {
                    existeCompraId = true;
                    break;
                }
            }
            rs.close();
            if (!existeCompraId) {
                stmt.execute("ALTER TABLE ingresos_historial ADD COLUMN compra_id INTEGER");
                System.out.println("✅ Migración: compra_id agregado a ingresos_historial");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 9: ventas_credito - ganancia_diferida y ganancia_realizada =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(ventas_credito)");
            boolean existeGananciaDiferida = false;
            boolean existeGananciaRealizada = false;
            while (rs.next()) {
                String colName = rs.getString("name");
                if ("ganancia_diferida".equals(colName)) existeGananciaDiferida = true;
                if ("ganancia_realizada".equals(colName)) existeGananciaRealizada = true;
            }
            rs.close();
            
            if (!existeGananciaDiferida) {
                stmt.execute("ALTER TABLE ventas_credito ADD COLUMN ganancia_diferida REAL DEFAULT 0");
                System.out.println("✅ Migración: ganancia_diferida agregado a ventas_credito");
            }
            if (!existeGananciaRealizada) {
                stmt.execute("ALTER TABLE ventas_credito ADD COLUMN ganancia_realizada REAL DEFAULT 0");
                System.out.println("✅ Migración: ganancia_realizada agregado a ventas_credito");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 12: ventas - forma_pago =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(ventas)");
            boolean existeFormaPago = false;
            while (rs.next()) {
                if ("forma_pago".equals(rs.getString("name"))) {
                    existeFormaPago = true;
                    break;
                }
            }
            rs.close();
            if (!existeFormaPago) {
                stmt.execute("ALTER TABLE ventas ADD COLUMN forma_pago TEXT DEFAULT 'Efectivo'");
                System.out.println("✅ Migración: forma_pago agregado a ventas");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 13: ventas - cliente_id =====
        try {
            ResultSet rs2 = stmt.executeQuery("PRAGMA table_info(ventas)");
            boolean existeClienteId = false;
            while (rs2.next()) {
                if ("cliente_id".equals(rs2.getString("name"))) {
                    existeClienteId = true;
                    break;
                }
            }
            rs2.close();
            if (!existeClienteId) {
                stmt.execute("ALTER TABLE ventas ADD COLUMN cliente_id INTEGER DEFAULT 0");
                System.out.println("✅ Migración: cliente_id agregado a ventas");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 14: materias_primas - imagen_path =====
        try {
            ResultSet rs3 = stmt.executeQuery("PRAGMA table_info(materias_primas)");
            boolean existeImagenPath = false;
            while (rs3.next()) {
                if ("imagen_path".equals(rs3.getString("name"))) {
                    existeImagenPath = true;
                    break;
                }
            }
            rs3.close();
            if (!existeImagenPath) {
                stmt.execute("ALTER TABLE materias_primas ADD COLUMN imagen_path TEXT");
                System.out.println("✅ Migración: imagen_path agregado a materias_primas");
            }
        } catch (SQLException e) { /* ignorar */ }

        // ===== MIGRACIÓN 10: clientes - eliminar tipo_cliente =====
        try {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(clientes)");
            boolean existeTipoCliente = false;
            while (rs.next()) {
                if ("tipo_cliente".equals(rs.getString("name"))) {
                    existeTipoCliente = true;
                    break;
                }
            }
            rs.close();
            if (existeTipoCliente) {
                System.out.println("⚠️ Migrando tabla clientes (eliminando tipo_cliente)...");
                stmt.execute(
                    "CREATE TABLE clientes_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "codigo TEXT UNIQUE," +
                    "nombre TEXT NOT NULL," +
                    "telefono TEXT," +
                    "email TEXT," +
                    "direccion TEXT," +
                    "ruc_cedula TEXT," +
                    "fecha_registro TEXT NOT NULL," +
                    "estado TEXT DEFAULT 'ACTIVO')"
                );
                stmt.execute(
                    "INSERT INTO clientes_temp (id, codigo, nombre, telefono, email, direccion, ruc_cedula, fecha_registro, estado) " +
                    "SELECT id, codigo, nombre, telefono, email, direccion, ruc_cedula, fecha_registro, estado FROM clientes"
                );
                stmt.execute("DROP TABLE clientes");
                stmt.execute("ALTER TABLE clientes_temp RENAME TO clientes");
                System.out.println("✅ Migración: tipo_cliente eliminado de clientes");
            }
        } catch (SQLException e) { 
            System.out.println("⚠️ No se pudo migrar clientes: " + e.getMessage());
        }

        // ===== MIGRACIÓN 11: Recrear tablas con ON DELETE CASCADE =====
        int versionActual = 0;
        try {
            ResultSet rsVer = stmt.executeQuery("PRAGMA user_version");
            if (rsVer.next()) versionActual = rsVer.getInt(1);
            rsVer.close();
        } catch (SQLException e) { /* ignorar, se asume 0 */ }

        if (versionActual < 12) {
            boolean legacyOriginal = false;
            try (ResultSet rsLegacy = stmt.executeQuery("PRAGMA legacy_alter_table")) {
                if (rsLegacy.next()) legacyOriginal = rsLegacy.getInt(1) != 0;
            } catch (SQLException e) { /* ignorar */ }

            try {
                stmt.execute("PRAGMA legacy_alter_table = ON");
                recuperarTablasHuerfanas(stmt);
                aplicarMigracion11Cascade(stmt);
            } finally {
                try {
                    stmt.execute("PRAGMA legacy_alter_table = " + (legacyOriginal ? "ON" : "OFF"));
                } catch (SQLException e) {
                    System.out.println("⚠️ No se pudo restaurar legacy_alter_table: " + e.getMessage());
                }
            }
            try {
                stmt.execute("PRAGMA user_version = 12");
            } catch (SQLException e) {
                System.out.println("⚠️ No se pudo fijar PRAGMA user_version: " + e.getMessage());
            }
        } else {
            System.out.println("✅ Migración 12 (ON DELETE CASCADE + legacy_alter_table fix) ya aplicada anteriormente, se omite.");
        }

        // ===== SINCRONIZACIÓN: nombre_producto en ventas/ventas_credito =====
        // Corrige de forma retroactiva las ventas cuyo nombre_producto quedó
        // desactualizado por renombrados de formulaciones hechos ANTES de que
        // FormulacionDAO.renombrar() propagara el cambio también a ventas.
        // Es seguro ejecutarla en cada arranque: si ya están sincronizados,
        // no cambia nada.
        try {
            stmt.execute(
                "UPDATE ventas SET nombre_producto = (" +
                "  SELECT f.nombre FROM formulaciones f WHERE f.id = ventas.formulacion_id" +
                ") WHERE formulacion_id IN (SELECT id FROM formulaciones) " +
                "AND nombre_producto <> (SELECT f.nombre FROM formulaciones f WHERE f.id = ventas.formulacion_id)"
            );
            System.out.println("✅ Sincronización: nombre_producto actualizado en ventas");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo sincronizar nombre_producto en ventas: " + e.getMessage());
        }
        try {
            stmt.execute(
                "UPDATE ventas_credito SET nombre_producto = (" +
                "  SELECT f.nombre FROM formulaciones f WHERE f.id = ventas_credito.formulacion_id" +
                ") WHERE formulacion_id IN (SELECT id FROM formulaciones) " +
                "AND nombre_producto <> (SELECT f.nombre FROM formulaciones f WHERE f.id = ventas_credito.formulacion_id)"
            );
            System.out.println("✅ Sincronización: nombre_producto actualizado en ventas_credito");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo sincronizar nombre_producto en ventas_credito: " + e.getMessage());
        }
    }

    private static void recuperarTablasHuerfanas(Statement stmt) {
        String[] tablas = {
            "movimientos_inventario", "formulacion_detalle", "ingresos_historial",
            "ventas_credito", "abonos", "detalle_pedido", "pagos_pedido",
            "compras_proveedores", "pagos_proveedores", "ventas"
        };
        for (String tabla : tablas) {
            try {
                boolean existePrincipal = existeTabla(stmt, tabla);
                boolean existeOld = existeTabla(stmt, tabla + "_old");
                if (!existePrincipal && existeOld) {
                    stmt.execute("ALTER TABLE " + tabla + "_old RENAME TO " + tabla);
                    System.out.println("🛠️ Recuperada tabla huérfana: " + tabla + "_old -> " + tabla);
                }
            } catch (SQLException e) {
                System.out.println("⚠️ No se pudo recuperar " + tabla + ": " + e.getMessage());
            }
        }
    }

    private static boolean existeTabla(Statement stmt, String nombreTabla) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='" + nombreTabla + "'")) {
            return rs.next();
        }
    }

    private static void aplicarMigracion11Cascade(Statement stmt) throws SQLException {
        try {
            stmt.execute("PRAGMA legacy_alter_table = ON");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo activar legacy_alter_table: " + e.getMessage());
        }

        try {
            aplicarSubmigraciones11(stmt);
        } finally {
            try {
                stmt.execute("PRAGMA legacy_alter_table = OFF");
            } catch (SQLException e) {
                System.out.println("⚠️ No se pudo restaurar legacy_alter_table: " + e.getMessage());
            }
        }
    }

    private static void aplicarSubmigraciones11(Statement stmt) throws SQLException {
        // 11.0 ventas
        try {
            stmt.execute("DROP TABLE IF EXISTS ventas_old");
            stmt.execute("ALTER TABLE ventas RENAME TO ventas_old");
            stmt.execute(
                "CREATE TABLE ventas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "formulacion_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "libras_vendidas REAL NOT NULL," +
                "costo_inversion REAL NOT NULL," +
                "precio_venta REAL NOT NULL," +
                "ganancia REAL NOT NULL," +
                "fecha TEXT NOT NULL," +
                "hora TEXT NOT NULL," +
                "forma_pago TEXT DEFAULT 'Efectivo'," +
                "cliente_id INTEGER DEFAULT 0," +
                "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO ventas (id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora, forma_pago, cliente_id) " +
                    "SELECT id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora, forma_pago, cliente_id FROM ventas_old"
                );
            } catch (SQLException eClienteId) {
                try {
                    stmt.execute(
                        "INSERT INTO ventas (id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora, forma_pago) " +
                        "SELECT id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora, forma_pago FROM ventas_old"
                    );
                } catch (SQLException e) {
                    try {
                        stmt.execute(
                            "INSERT INTO ventas (id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora) " +
                            "SELECT id, formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora FROM ventas_old"
                        );
                    } catch (SQLException e2) { /* ignorar */ }
                }
            }
            stmt.execute("DROP TABLE ventas_old");
            System.out.println("✅ Migración 11.0: ventas recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar ventas: " + e.getMessage());
        }

        // 11.1 movimientos_inventario
        try {
            stmt.execute("DROP TABLE IF EXISTS movimientos_inventario_old");
            stmt.execute("ALTER TABLE movimientos_inventario RENAME TO movimientos_inventario_old");
            stmt.execute(
                "CREATE TABLE movimientos_inventario (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "materia_prima_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "unidad TEXT NOT NULL," +
                "tipo_movimiento TEXT NOT NULL," +
                "cantidad REAL NOT NULL," +
                "precio_por_unidad REAL NOT NULL," +
                "subtotal REAL NOT NULL," +
                "sacos_afectados INTEGER," +
                "libras_sueltas_afectadas REAL," +
                "fecha TEXT NOT NULL," +
                "hora TEXT NOT NULL," +
                "referencia TEXT," +
                "pedido_id INTEGER," +
                "compra_id INTEGER," +
                "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO movimientos_inventario SELECT * FROM movimientos_inventario_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE movimientos_inventario_old");
            System.out.println("✅ Migración 11.1: movimientos_inventario recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar movimientos_inventario: " + e.getMessage());
        }

        // 11.2 formulacion_detalle
        try {
            stmt.execute("DROP TABLE IF EXISTS formulacion_detalle_old");
            stmt.execute("ALTER TABLE formulacion_detalle RENAME TO formulacion_detalle_old");
            stmt.execute(
                "CREATE TABLE formulacion_detalle (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "formulacion_id INTEGER NOT NULL," +
                "materia_prima_id INTEGER NOT NULL," +
                "nombre_ingrediente TEXT NOT NULL," +
                "unidad TEXT NOT NULL," +
                "cantidad_usada REAL NOT NULL," +
                "precio_unitario REAL NOT NULL," +
                "subtotal REAL NOT NULL," +
                "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE," +
                "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO formulacion_detalle SELECT * FROM formulacion_detalle_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE formulacion_detalle_old");
            System.out.println("✅ Migración 11.2: formulacion_detalle recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar formulacion_detalle: " + e.getMessage());
        }

        // 11.3 ingresos_historial
        try {
            stmt.execute("DROP TABLE IF EXISTS ingresos_historial_old");
            stmt.execute("ALTER TABLE ingresos_historial RENAME TO ingresos_historial_old");
            stmt.execute(
                "CREATE TABLE ingresos_historial (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "producto_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "unidad TEXT NOT NULL," +
                "cantidad REAL NOT NULL," +
                "precio_unitario REAL NOT NULL," +
                "precio_total REAL NOT NULL," +
                "numero_sacos INTEGER," +
                "peso_saco REAL," +
                "libras_sueltas REAL," +
                "fecha_ingreso TEXT NOT NULL," +
                "hora_ingreso TEXT NOT NULL," +
                "pedido_id INTEGER," +
                "compra_id INTEGER," +
                "cantidad_usada REAL DEFAULT 0," +
                "origen TEXT DEFAULT 'INVENTARIO'," +
                "FOREIGN KEY (producto_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO ingresos_historial SELECT * FROM ingresos_historial_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE ingresos_historial_old");
            System.out.println("✅ Migración 11.3: ingresos_historial recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar ingresos_historial: " + e.getMessage());
        }

        // 11.4 ventas_credito
        try {
            stmt.execute("DROP TABLE IF EXISTS ventas_credito_old");
            stmt.execute("ALTER TABLE ventas_credito RENAME TO ventas_credito_old");
            stmt.execute(
                "CREATE TABLE ventas_credito (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "venta_id INTEGER NOT NULL," +
                "cliente_id INTEGER NOT NULL," +
                "formulacion_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "total_libras REAL NOT NULL," +
                "precio_total REAL NOT NULL," +
                "abonado REAL DEFAULT 0," +
                "saldo_pendiente REAL NOT NULL," +
                "estado TEXT DEFAULT 'PENDIENTE'," +
                "fecha_venta TEXT NOT NULL," +
                "fecha_limite_pago TEXT," +
                "notas TEXT," +
                "ganancia_diferida REAL DEFAULT 0," +
                "ganancia_realizada REAL DEFAULT 0," +
                "FOREIGN KEY (venta_id) REFERENCES ventas(id) ON DELETE CASCADE," +
                "FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE," +
                "FOREIGN KEY (formulacion_id) REFERENCES formulaciones(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO ventas_credito SELECT * FROM ventas_credito_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE ventas_credito_old");
            System.out.println("✅ Migración 11.4: ventas_credito recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar ventas_credito: " + e.getMessage());
        }

        // 11.5 abonos
        try {
            stmt.execute("DROP TABLE IF EXISTS abonos_old");
            stmt.execute("ALTER TABLE abonos RENAME TO abonos_old");
            stmt.execute(
                "CREATE TABLE abonos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "venta_credito_id INTEGER NOT NULL," +
                "cliente_id INTEGER NOT NULL," +
                "monto REAL NOT NULL," +
                "forma_pago TEXT DEFAULT 'EFECTIVO'," +
                "fecha_abono TEXT NOT NULL," +
                "hora_abono TEXT NOT NULL," +
                "referencia TEXT," +
                "FOREIGN KEY (venta_credito_id) REFERENCES ventas_credito(id) ON DELETE CASCADE," +
                "FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO abonos SELECT * FROM abonos_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE abonos_old");
            System.out.println("✅ Migración 11.5: abonos recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar abonos: " + e.getMessage());
        }

        // 11.6 detalle_pedido
        try {
            stmt.execute("DROP TABLE IF EXISTS detalle_pedido_old");
            stmt.execute("ALTER TABLE detalle_pedido RENAME TO detalle_pedido_old");
            stmt.execute(
                "CREATE TABLE detalle_pedido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pedido_id INTEGER NOT NULL," +
                "materia_prima_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "unidad TEXT NOT NULL," +
                "cantidad REAL NOT NULL," +
                "precio_unitario REAL NOT NULL," +
                "subtotal REAL NOT NULL," +
                "numero_sacos INTEGER," +
                "peso_saco REAL," +
                "libras_sueltas REAL," +
                "FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE," +
                "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO detalle_pedido SELECT * FROM detalle_pedido_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE detalle_pedido_old");
            System.out.println("✅ Migración 11.6: detalle_pedido recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar detalle_pedido: " + e.getMessage());
        }

        // 11.7 pagos_pedido
        try {
            stmt.execute("DROP TABLE IF EXISTS pagos_pedido_old");
            stmt.execute("ALTER TABLE pagos_pedido RENAME TO pagos_pedido_old");
            stmt.execute(
                "CREATE TABLE pagos_pedido (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pedido_id INTEGER NOT NULL," +
                "proveedor_id INTEGER NOT NULL," +
                "monto REAL NOT NULL," +
                "forma_pago TEXT DEFAULT 'EFECTIVO'," +
                "fecha_pago TEXT NOT NULL," +
                "hora_pago TEXT NOT NULL," +
                "referencia TEXT," +
                "FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO pagos_pedido SELECT * FROM pagos_pedido_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE pagos_pedido_old");
            System.out.println("✅ Migración 11.7: pagos_pedido recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar pagos_pedido: " + e.getMessage());
        }

        // 11.8 compras_proveedores
        try {
            stmt.execute("DROP TABLE IF EXISTS compras_proveedores_old");
            stmt.execute("ALTER TABLE compras_proveedores RENAME TO compras_proveedores_old");
            stmt.execute(
                "CREATE TABLE compras_proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "proveedor_id INTEGER NOT NULL," +
                "materia_prima_id INTEGER NOT NULL," +
                "nombre_producto TEXT NOT NULL," +
                "unidad TEXT NOT NULL," +
                "cantidad REAL NOT NULL," +
                "precio_unitario REAL NOT NULL," +
                "precio_total REAL NOT NULL," +
                "numero_sacos INTEGER," +
                "peso_saco REAL," +
                "libras_sueltas REAL," +
                "estado_pago TEXT DEFAULT 'PENDIENTE'," +
                "estado_recepcion TEXT DEFAULT 'PENDIENTE'," +
                "abonado REAL DEFAULT 0," +
                "saldo_pendiente REAL NOT NULL," +
                "fecha_compra TEXT NOT NULL," +
                "fecha_limite_pago TEXT," +
                "notas TEXT," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE," +
                "FOREIGN KEY (materia_prima_id) REFERENCES materias_primas(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO compras_proveedores SELECT * FROM compras_proveedores_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE compras_proveedores_old");
            System.out.println("✅ Migración 11.8: compras_proveedores recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar compras_proveedores: " + e.getMessage());
        }

        // 11.9 pagos_proveedores
        try {
            stmt.execute("DROP TABLE IF EXISTS pagos_proveedores_old");
            stmt.execute("ALTER TABLE pagos_proveedores RENAME TO pagos_proveedores_old");
            stmt.execute(
                "CREATE TABLE pagos_proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "compra_id INTEGER NOT NULL," +
                "proveedor_id INTEGER NOT NULL," +
                "monto REAL NOT NULL," +
                "forma_pago TEXT DEFAULT 'EFECTIVO'," +
                "fecha_pago TEXT NOT NULL," +
                "hora_pago TEXT NOT NULL," +
                "referencia TEXT," +
                "FOREIGN KEY (compra_id) REFERENCES compras_proveedores(id) ON DELETE CASCADE," +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE CASCADE)"
            );
            try {
                stmt.execute(
                    "INSERT INTO pagos_proveedores SELECT * FROM pagos_proveedores_old"
                );
            } catch (SQLException e) { /* ignorar */ }
            stmt.execute("DROP TABLE pagos_proveedores_old");
            System.out.println("✅ Migración 11.9: pagos_proveedores recreada con ON DELETE CASCADE");
        } catch (SQLException e) {
            System.out.println("⚠️ No se pudo migrar pagos_proveedores: " + e.getMessage());
        }
    }
}