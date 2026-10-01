package com.feedformulator.dao;

import com.feedformulator.model.DetalleFormulacion;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.util.DensidadManager;
import com.feedformulator.util.LoggerUtil;
import com.feedformulator.util.MathUtils;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FormulacionDAO {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();

   public void guardar(Formulacion f) throws SQLException {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);

        // ============================================================
        //  VALIDAR STOCK - CON UNIDAD CORRECTA PARA LÍQUIDOS
        // ============================================================
        StringBuilder errores = new StringBuilder();
        boolean hayStockSuficiente = true;

        for (DetalleFormulacion d : f.getDetalles()) {
            MateriaPrima mp = materiaDAO.buscarPorId(d.getMateriaPrimaId());
            if (mp == null) {
                errores.append("❌ Producto '").append(d.getNombreIngrediente()).append("' NO ENCONTRADO.<br>");
                hayStockSuficiente = false;
            } else {
                // ============================================================
                //  DETERMINAR LA CANTIDAD A VALIDAR Y SU UNIDAD
                // ============================================================
                double cantidadNecesaria;
                double cantidadDisponible = mp.getCantidadDisponible();
                String unidadMostrar = mp.getUnidad();
                
                if ("litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad())) {
                    // Para líquidos: validar en litros (cantidad original)
                    cantidadNecesaria = d.getCantidadOriginal() > 0 ? d.getCantidadOriginal() : d.getCantidadUsada();
                    unidadMostrar = "litro";
                } else {
                    // Para sólidos: validar en libras
                    cantidadNecesaria = d.getCantidadUsada();
                }
                
                if (cantidadDisponible < cantidadNecesaria) {
                    errores.append("❌ ").append(d.getNombreIngrediente())
                           .append(": Disponible <b>").append(String.format("%.2f", cantidadDisponible))
                           .append("</b> ").append(unidadMostrar)
                           .append(" | Necesario <b>").append(String.format("%.2f", cantidadNecesaria))
                           .append("</b> ").append(unidadMostrar)
                           .append(" (Faltan <b>").append(String.format("%.2f", cantidadNecesaria - cantidadDisponible))
                           .append("</b> ").append(unidadMostrar).append(")<br>");
                    hayStockSuficiente = false;
                }
            }
        }

        // ============================================================
        //  SI NO HAY STOCK SUFICIENTE, LANZAR EXCEPCIÓN Y NO GUARDAR
        // ============================================================
        if (!hayStockSuficiente) {
            String mensajeError = "⚠️ STOCK INSUFICIENTE PARA CREAR ESTA FORMULACIÓN<br><br>" + 
                                  errores.toString() +
                                  "<br>💡 <b>Agregue stock en la pestaña 'Inventario' antes de guardar la fórmula.</b>";
            
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
                try { conn.close(); } catch (SQLException ex) { /* ignorar */ }
                conn = null;
            }
            
            throw new SQLException(mensajeError);
        }

        // ============================================================
        //  INSERTAR FORMULACIÓN (SOLO SI HAY STOCK SUFICIENTE)
        // ============================================================
        f.setFecha(LocalDate.now().toString());
        f.setHora(LocalTime.now().format(TIME_FMT));
        f.setEstado("Disponible");

        String sql = "INSERT INTO formulaciones (nombre, total_libras, costo_total, fecha, hora, estado) VALUES (?,?,?,?,?,?)";
        ps = conn.prepareStatement(sql);
        ps.setString(1, f.getNombre());
        ps.setDouble(2, f.getTotalLibras());
        ps.setDouble(3, f.getCostoTotal());
        ps.setString(4, f.getFecha());
        ps.setString(5, f.getHora());
        ps.setString(6, f.getEstado());
        ps.executeUpdate();
        ps.close();
        
        try (Statement stmt = conn.createStatement()) {
            rs = stmt.executeQuery("SELECT last_insert_rowid()");
            if (rs.next()) {
                f.setId(rs.getInt(1));
            }
            rs.close();
        }

        // ============================================================
        //  INSERTAR DETALLES (SIN cantidad_original)
        // ============================================================
        String sqlD = "INSERT INTO formulacion_detalle (formulacion_id, materia_prima_id, nombre_ingrediente, unidad, cantidad_usada, precio_unitario, subtotal) VALUES (?,?,?,?,?,?,?)";

        ps = conn.prepareStatement(sqlD);
        for (DetalleFormulacion d : f.getDetalles()) {
            d.setFormulacionId(f.getId());
            ps.setInt(1, f.getId());
            ps.setInt(2, d.getMateriaPrimaId());
            ps.setString(3, d.getNombreIngrediente());
            ps.setString(4, "lb");  // SIEMPRE "lb" para el peso del saco
            ps.setDouble(5, d.getCantidadUsada());
            ps.setDouble(6, d.getPrecioUnitario());
            ps.setDouble(7, d.getSubtotal());
            ps.executeUpdate();
        }
        ps.close();

        // ============================================================
        //  DESCONTAR STOCK - CORREGIDO PARA LÍQUIDOS
        // ============================================================
        for (DetalleFormulacion d : f.getDetalles()) {
            try {
                // Obtener el producto
                String sqlMp = "SELECT * FROM materias_primas WHERE id = ?";
                ps = conn.prepareStatement(sqlMp);
                ps.setInt(1, d.getMateriaPrimaId());
                rs = ps.executeQuery();
                
                if (!rs.next()) {
                    throw new SQLException("Producto no encontrado: ID " + d.getMateriaPrimaId());
                }
                
                double stockActual = rs.getDouble("cantidad_disponible");
                String nombreProducto = rs.getString("nombre");
                String unidad = rs.getString("unidad");
                rs.close();
                ps.close();
                ps = null;
                
                // ============================================================
                //  DETERMINAR LA CANTIDAD A DESCONTAR
                //  - Si el producto es líquido (litro), descontar en litros (cantidad original)
                //  - Si es sólido (lb), descontar en libras (cantidad usada)
                // ============================================================
                double cantidadADescontar;
                String unidadDescuento;
                double precioUnitarioParaMovimiento;
                
                if ("litro".equals(unidad) || "litros".equals(unidad)) {
                    // Para líquidos: usar la cantidad original (litros)
                    cantidadADescontar = d.getCantidadOriginal() > 0 ? d.getCantidadOriginal() : d.getCantidadUsada();
                    unidadDescuento = "litro";
                    // El precio unitario en litros = precio por libra * densidad
                    double densidad = DensidadManager.getDensidad(d.getNombreIngrediente());
                    precioUnitarioParaMovimiento = d.getPrecioUnitario() * densidad;
                } else {
                    // Para sólidos: usar la cantidad en libras
                    cantidadADescontar = d.getCantidadUsada();
                    unidadDescuento = unidad;
                    precioUnitarioParaMovimiento = d.getPrecioUnitario();
                }
                
                // Validar stock usando la cantidad correcta
                if (stockActual < cantidadADescontar) {
                    throw new SQLException("Stock insuficiente para " + nombreProducto + 
                        ". Disponible: " + String.format("%.2f", stockActual) + 
                        " " + unidadDescuento + 
                        ", Necesario: " + String.format("%.2f", cantidadADescontar) + 
                        " " + unidadDescuento);
                }
                
                // Actualizar stock disponible (usando la cantidad correcta)
                double nuevoStock = stockActual - cantidadADescontar;
                String sqlUpdate = "UPDATE materias_primas SET cantidad_disponible = ? WHERE id = ?";
                ps = conn.prepareStatement(sqlUpdate);
                ps.setDouble(1, nuevoStock);
                ps.setInt(2, d.getMateriaPrimaId());
                ps.executeUpdate();
                ps.close();
                ps = null;
                
                // ============================================================
                //  REGISTRAR MOVIMIENTO DE INVENTARIO
                //  - Para líquidos: mostrar en litros
                //  - Para sólidos: mostrar en libras
                // ============================================================
                String sqlMov = "INSERT INTO movimientos_inventario (" +
                    "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                    "cantidad, precio_por_unidad, subtotal, fecha, hora, referencia) " +
                    "VALUES (?, ?, ?, 'USO', ?, ?, ?, date('now'), time('now'), ?)";
                
                ps = conn.prepareStatement(sqlMov);
                ps.setInt(1, d.getMateriaPrimaId());
                ps.setString(2, nombreProducto);
                ps.setString(3, unidadDescuento);
                ps.setDouble(4, cantidadADescontar);
                ps.setDouble(5, precioUnitarioParaMovimiento);
                // El subtotal se calcula con la cantidad en la unidad del movimiento
                double subtotalMovimiento = cantidadADescontar * precioUnitarioParaMovimiento;
                ps.setDouble(6, subtotalMovimiento);
                ps.setString(7, "Formulación: " + f.getNombre());
                ps.executeUpdate();
                ps.close();
                ps = null;
                
                System.out.println("📦 Stock consumido: " + nombreProducto + 
                    " -" + String.format("%.2f", cantidadADescontar) + " " + unidadDescuento);
                
            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
                    try { conn.close(); } catch (SQLException ex) { /* ignorar */ }
                    conn = null;
                }
                throw new SQLException("❌ Error al consumir stock de " + d.getNombreIngrediente() + ": " + e.getMessage());
            }
        }

        conn.commit();
        System.out.println("✅ Formulación guardada: " + f.getNombre());
        
        LoggerUtil.logInfo("Formulación guardada: " + f.getNombre() + " (ID: " + f.getId() + ")");
        
    } catch (SQLException e) {
        if (conn != null) { 
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } 
        }
        throw e;
    } finally {
        if (conn != null) { 
            try { 
                conn.setAutoCommit(true); 
                conn.close(); 
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            } 
        }
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
    }
}

    public void marcarComoVendida(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("UPDATE formulaciones SET estado='Vendido' WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            LoggerUtil.logInfo("Formulación marcada como vendida: " + id);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    public void marcarComoDisponible(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("UPDATE formulaciones SET estado='Disponible' WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            LoggerUtil.logInfo("Formulación marcada como disponible: " + id);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Renombra una formulación ya creada (disponible o vendida), sin tocar
     * ningún otro dato. Se usa desde el Historial (solo Administrador) para
     * unificar bajo un mismo nombre lotes que se formularon de distintos
     * tamaños (sacos completos, medios sacos, cantidades en dólares, etc.)
     * y así agruparlos en una sola tarjeta en Inventario de Formulaciones.
     *
     * IMPORTANTE: esto NUNCA modifica ventas ya registradas. El nombre del
     * producto en una venta ya hecha (ventas.nombre_producto) queda como una
     * "foto" histórica de cómo se llamaba en el momento de la venta — es lo
     * correcto para no alterar reportes o historiales de ventas pasadas.
     * Solo cambia el nombre "vivo" de la formulación, usado por Inventario
     * de Formulaciones y por futuras duplicaciones.
     */
    /**
     * Renombra una formulación ya creada (disponible o vendida), sin tocar
     * ningún otro dato. Se usa desde el Historial (solo Administrador) para
     * unificar bajo un mismo nombre lotes que se formularon de distintos
     * tamaños (sacos completos, medios sacos, cantidades en dólares, etc.)
     * y así agruparlos en una sola tarjeta en Inventario de Formulaciones.
     *
     * IMPORTANTE: a diferencia de un cambio de nombre "solo cosmético", esto
     * TAMBIÉN actualiza el nombre_producto ya guardado en ventas y en
     * ventas_credito para esa formulación — así Reportes (Top Productos,
     * Materiales Más Usados) queda consistente con la nueva clasificación
     * en vez de seguir mostrando el nombre viejo para ventas pasadas. Esto
     * es intencional: el propósito de renombrar es unificar la clasificación
     * de una receta en todo el sistema, no solo en Inventario.
     */
    public void renombrar(int id, String nuevoNombre) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement("UPDATE formulaciones SET nombre=? WHERE id=?");
            ps.setString(1, nuevoNombre);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement("UPDATE ventas SET nombre_producto=? WHERE formulacion_id=?");
            ps.setString(1, nuevoNombre);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();

            try {
                ps = conn.prepareStatement("UPDATE ventas_credito SET nombre_producto=? WHERE formulacion_id=?");
                ps.setString(1, nuevoNombre);
                ps.setInt(2, id);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                // ventas_credito podría no existir en instalaciones muy antiguas
            }

            conn.commit();
            LoggerUtil.logInfo("Formulación " + id + " renombrada a: " + nuevoNombre + " (incluye ventas históricas)");
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Versión ligera de listarTodas(), pensada para vistas de RESUMEN (como
     * las tarjetas de "Inventario de Formulaciones") que solo necesitan
     * nombre/libras/costo/estado y NO los ingredientes de cada fórmula.
     * A diferencia de listarTodas(), esta NO hace una consulta adicional
     * por cada formulación (evita el problema de rendimiento N+1), por lo
     * que es segura de llamar frecuentemente (ej. cada vez que se guarda
     * stock en Inventario) sin ralentizar la aplicación.
     */
    public List<Formulacion> listarTodasResumen() throws SQLException {
        List<Formulacion> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM formulaciones ORDER BY id DESC");

            while (rs.next()) {
                lista.add(mapear(rs)); // Sin obtenerDetalles(): mucho más rápido
            }

        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public List<Formulacion> listarTodas() throws SQLException {
        List<Formulacion> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM formulaciones ORDER BY id DESC");
            
            while (rs.next()) {
                Formulacion f = mapear(rs);
                f.setDetalles(obtenerDetalles(f.getId()));
                lista.add(f);
            }
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public List<Formulacion> listarDisponibles() throws SQLException {
        List<Formulacion> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM formulaciones WHERE estado='Disponible' ORDER BY id DESC");
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Formulacion f = mapear(rs);
                f.setDetalles(obtenerDetalles(f.getId()));
                lista.add(f);
            }
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public Formulacion obtenerPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM formulaciones WHERE id=?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                Formulacion f = mapear(rs);
                f.setDetalles(obtenerDetalles(id));
                return f;
            }
            return null;
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

  public List<DetalleFormulacion> obtenerDetalles(int formulacionId) throws SQLException {
    List<DetalleFormulacion> lista = new ArrayList<>();
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        conn = DatabaseManager.getConnection();
        
        String sql = "SELECT * FROM formulacion_detalle WHERE formulacion_id=?";
        ps = conn.prepareStatement(sql);
        ps.setInt(1, formulacionId);
        rs = ps.executeQuery();
        
        while (rs.next()) {
            DetalleFormulacion d = new DetalleFormulacion();
            d.setId(rs.getInt("id"));
            d.setFormulacionId(formulacionId);
            d.setMateriaPrimaId(rs.getInt("materia_prima_id"));
            d.setNombreIngrediente(rs.getString("nombre_ingrediente"));
            d.setUnidad(rs.getString("unidad"));
            d.setCantidadUsada(rs.getDouble("cantidad_usada"));
            d.setPrecioUnitario(rs.getDouble("precio_unitario"));
            d.setSubtotal(rs.getDouble("subtotal"));
            
            // ============================================================
            //  RECUPERAR CANTIDAD ORIGINAL PARA LÍQUIDOS
            //  NOTA: la columna "unidad" en formulacion_detalle SIEMPRE es "lb"
            //  (peso del saco), así que el indicador confiable de líquido es
            //  la propia columna cantidad_original (>0 solo para líquidos).
            // ============================================================
            double cantidadOriginalRaw = 0;
            try {
                cantidadOriginalRaw = rs.getDouble("cantidad_original");
            } catch (SQLException e) {
                // La columna cantidad_original no existe todavía
            }

            if (cantidadOriginalRaw > 0) {
                d.setCantidadOriginal(cantidadOriginalRaw);
                d.setUnidadOriginal("litro");
            } else {
                d.setCantidadOriginal(0);
                d.setUnidadOriginal(d.getUnidad());
            }
            
            lista.add(d);
        }
        
    } finally {
        if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
    }
    return lista;
}

public void eliminar(int id) throws SQLException {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        
        // ============================================================
        //  1. OBTENER DETALLES DE LA FORMULACIÓN
        // ============================================================
        List<DetalleFormulacion> detalles = new ArrayList<>();
        String sqlDetalle = "SELECT materia_prima_id, cantidad_usada, nombre_ingrediente, unidad FROM formulacion_detalle WHERE formulacion_id=?";
        ps = conn.prepareStatement(sqlDetalle);
        ps.setInt(1, id);
        rs = ps.executeQuery();
        while (rs.next()) {
            DetalleFormulacion d = new DetalleFormulacion();
            d.setMateriaPrimaId(rs.getInt("materia_prima_id"));
            d.setCantidadUsada(rs.getDouble("cantidad_usada"));
            d.setNombreIngrediente(rs.getString("nombre_ingrediente"));
            d.setUnidad(rs.getString("unidad"));
            
            // ============================================================
            //  DETERMINAR SI ES LÍQUIDO Y CALCULAR LITROS
            // ============================================================
            try {
                MateriaPrima mp = materiaDAO.buscarPorId(d.getMateriaPrimaId());
                if (mp != null && ("litro".equals(mp.getUnidad()) || "litros".equals(mp.getUnidad()))) {
                    // Es líquido: calcular litros a partir del peso
                    double densidad = DensidadManager.getDensidad(d.getNombreIngrediente());
                    double litros = d.getCantidadUsada() / densidad;
                    litros = Math.round(litros * 100.0) / 100.0;
                    d.setCantidadOriginal(litros);
                    d.setUnidadOriginal("litro");
                    System.out.println("📊 Líquido detectado: " + d.getNombreIngrediente() + 
                        " | Peso: " + String.format("%.2f", d.getCantidadUsada()) + " lb" +
                        " | Litros: " + String.format("%.2f", litros));
                } else {
                    // Sólido: usar la cantidad usada
                    d.setCantidadOriginal(d.getCantidadUsada());
                    d.setUnidadOriginal(d.getUnidad());
                }
            } catch (Exception e) {
                d.setCantidadOriginal(d.getCantidadUsada());
                d.setUnidadOriginal(d.getUnidad());
            }
            
            detalles.add(d);
        }
        rs.close();
        ps.close();
        ps = null;
        
        // ============================================================
        //  2. DEVOLVER STOCK - CORREGIDO PARA LÍQUIDOS
        // ============================================================
        for (DetalleFormulacion d : detalles) {
            double cantidadADevolver;
            String unidadDevolucion;
            
            // ============================================================
            //  DETERMINAR LA CANTIDAD A DEVOLVER
            //  - Si es líquido: devolver en LITROS (cantidadOriginal)
            //  - Si es sólido: devolver en LIBRAS (cantidadUsada)
            // ============================================================
            if ("litro".equals(d.getUnidadOriginal()) || "litros".equals(d.getUnidadOriginal())) {
                cantidadADevolver = d.getCantidadOriginal();
                unidadDevolucion = "litro";
                System.out.println("📦 Devolviendo LÍQUIDO: " + d.getNombreIngrediente() + 
                    " +" + String.format("%.2f", cantidadADevolver) + " " + unidadDevolucion);
            } else {
                cantidadADevolver = d.getCantidadUsada();
                unidadDevolucion = "lb";
                System.out.println("📦 Devolviendo SÓLIDO: " + d.getNombreIngrediente() + 
                    " +" + String.format("%.2f", cantidadADevolver) + " " + unidadDevolucion);
            }
            
            // ============================================================
            //  ACTUALIZAR STOCK EN LA UNIDAD CORRECTA
            // ============================================================
            String sqlUpdate;
            if ("litro".equals(unidadDevolucion) || "litros".equals(unidadDevolucion)) {
                // Para líquidos: actualizar en litros
                sqlUpdate = "UPDATE materias_primas SET cantidad_disponible = cantidad_disponible + ? WHERE id = ? AND (unidad = 'litro' OR unidad = 'litros')";
            } else {
                // Para sólidos: actualizar en libras
                sqlUpdate = "UPDATE materias_primas SET cantidad_disponible = cantidad_disponible + ? WHERE id = ?";
            }
            
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, cantidadADevolver);
            ps.setInt(2, d.getMateriaPrimaId());
            int actualizados = ps.executeUpdate();
            ps.close();
            ps = null;
            
            if (actualizados > 0) {
                System.out.println("✅ Stock actualizado: " + d.getNombreIngrediente() + 
                    " +" + String.format("%.2f", cantidadADevolver) + " " + unidadDevolucion);
            } else {
                System.err.println("⚠️ No se pudo actualizar stock para: " + d.getNombreIngrediente());
            }
        }
        
        // ============================================================
        //  3. ELIMINAR DETALLES
        // ============================================================
        String sqlDetalleDelete = "DELETE FROM formulacion_detalle WHERE formulacion_id=?";
        ps = conn.prepareStatement(sqlDetalleDelete);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        ps = null;
        
        // ============================================================
        //  4. ELIMINAR FORMULACIÓN
        // ============================================================
        String sql = "DELETE FROM formulaciones WHERE id=?";
        ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
        ps = null;
        
        conn.commit();
        System.out.println("✅ Formulación ID " + id + " eliminada y stock devuelto correctamente.");
        
    } catch (SQLException e) {
        if (conn != null) { 
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } 
        }
        throw e;
    } finally {
        if (conn != null) { 
            try { 
                conn.setAutoCommit(true); 
                conn.close(); 
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            } 
        }
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
    }
}

    private Formulacion mapear(ResultSet rs) throws SQLException {
        Formulacion f = new Formulacion();
        f.setId(rs.getInt("id"));
        f.setNombre(rs.getString("nombre"));
        f.setTotalLibras(rs.getDouble("total_libras"));
        f.setCostoTotal(rs.getDouble("costo_total"));
        f.setFecha(rs.getString("fecha"));
        f.setHora(rs.getString("hora"));
        f.setEstado(rs.getString("estado"));
        return f;
    }
}