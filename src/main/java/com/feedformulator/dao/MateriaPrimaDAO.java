package com.feedformulator.dao;

import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.MovimientoInventario;
import com.feedformulator.model.IngresoHistorial;
import com.feedformulator.util.MathUtils;
import com.feedformulator.util.LoggerUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MateriaPrimaDAO {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public void guardarRapido(MateriaPrima mp) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            mp.setFecha(LocalDate.now().toString());
            mp.setHora(LocalTime.now().format(TIME_FMT));
            
            if (mp.getCodigo() == null || mp.getCodigo().isEmpty()) {
                mp.setCodigo(generarCodigo(mp.getNombre()));
            }
            
            if (mp.getOrigen() == null || mp.getOrigen().isEmpty()) {
                mp.setOrigen("PEDIDO");
            }
            
            double cantidadTotal = 0;
            if (mp.getNumeroSacos() != null && mp.getPesoSaco() != null) {
                cantidadTotal = mp.getNumeroSacos() * mp.getPesoSaco();
            }
            if (mp.getLibrasSueltas() != null) {
                cantidadTotal += mp.getLibrasSueltas();
            }
            
            double precioTotal = cantidadTotal * mp.getPrecioPorUnidad();
            
            String sql = "INSERT INTO materias_primas (" +
                         "codigo, nombre, unidad, cantidad_total, cantidad_disponible, " +
                         "precio_total, precio_por_unidad, fecha_ingreso, hora_ingreso, " +
                         "peso_saco, numero_sacos, libras_sueltas, origen) " +
                         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, mp.getCodigo());
            ps.setString(2, mp.getNombre());
            ps.setString(3, mp.getUnidad());
            ps.setDouble(4, 0);
            ps.setDouble(5, 0);
            ps.setDouble(6, 0);
            ps.setDouble(7, mp.getPrecioPorUnidad());
            ps.setString(8, mp.getFecha());
            ps.setString(9, mp.getHora());
            ps.setDouble(10, mp.getPesoSaco() != null ? mp.getPesoSaco() : 0);
            ps.setInt(11, mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0);
            ps.setDouble(12, mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0);
            ps.setString(13, mp.getOrigen());
            ps.executeUpdate();
            ps.close();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rsId = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rsId.next()) {
                    int nuevoId = rsId.getInt(1);
                    mp.setId(nuevoId);
                    System.out.println("✅ Producto creado con ID: " + nuevoId + " - " + mp.getNombre() + " (Origen: " + mp.getOrigen() + ")");
                } else {
                    throw new SQLException("No se pudo obtener el ID del producto");
                }
            }
            
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) { 
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } 
            }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public void guardarOActualizar(MateriaPrima mp) throws SQLException {
        long tInicio = System.currentTimeMillis();
        try {
            guardarOActualizarInterno(mp);
        } finally {
            long tFin = System.currentTimeMillis();
            System.out.println("⏱️ guardarOActualizar() TOTAL: " + (tFin - tInicio) + "ms");
        }
    }

    private void guardarOActualizarInterno(MateriaPrima mp) throws SQLException {
    guardarOActualizar(mp, null);
}

public void guardarOActualizar(MateriaPrima mp, Integer compraId) throws SQLException {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    int productoId = -1;
    MovimientoInventario movimientoIngreso = null;
    String datosAnteriores = null;
    String datosNuevos = null;
    
    try {
        conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);

        String nombreNormalizado = mp.getNombre().trim().toUpperCase();
        
        MateriaPrima existente = buscarPorNombreNormalizado(nombreNormalizado);
        
        // Valores INDIVIDUALES de este ingreso para guardar en el historial
        // (se calculan segun la rama, nunca deben quedar como el acumulado)
        double cantidadParaHistorial;
        double precioUnitarioParaHistorial;
        double precioTotalParaHistorial;
        int numSacosParaHistorial;
        double pesoSacoParaHistorial;
        double librasSueltasParaHistorial;
        
        if (existente != null) {
            productoId = existente.getId();
            mp.setId(productoId);
            
            if (!existente.getNombre().equals(mp.getNombre())) {
                existente.setNombre(mp.getNombre());
            }
            
            datosAnteriores = String.format("id:%d,nombre:%s,total:%.2f,disponible:%.2f,precio_total:%.2f,sacos:%d",
                existente.getId(), existente.getNombre(), existente.getCantidadTotal(),
                existente.getCantidadDisponible(), existente.getPrecioTotal(),
                existente.getNumeroSacos() != null ? existente.getNumeroSacos() : 0);
            
            double cantidadNuevoIngreso = mp.getCantidadTotal();
            double precioNuevoIngreso = mp.getPrecioTotal();
            
            // Guardamos los valores INDIVIDUALES de este ingreso (no acumulados) para el historial
            cantidadParaHistorial = cantidadNuevoIngreso;
            precioUnitarioParaHistorial = mp.getPrecioPorUnidad();
            precioTotalParaHistorial = precioNuevoIngreso;
            numSacosParaHistorial = mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0;
            pesoSacoParaHistorial = mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
            librasSueltasParaHistorial = mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0;
            
            double nuevaCantidadTotal = MathUtils.redondear(existente.getCantidadTotal() + cantidadNuevoIngreso);
            double nuevaCantidadDisponible = MathUtils.redondear(existente.getCantidadDisponible() + cantidadNuevoIngreso);
            double nuevoPrecioTotal = MathUtils.redondear(existente.getPrecioTotal() + precioNuevoIngreso);
            
            // IMPORTANTE: el precio por unidad NO se calcula con el total histórico
            // (cantidad_total/precio_total, que siguen creciendo con cada compra y
            // NUNCA bajan al usar material). Si se usara ese total, el precio
            // quedaría "inflado" con dinero que ya se gastó y usó en fórmulas
            // pasadas, aunque físicamente ya no tengas esa mercancía.
            //
            // Ejemplo: compras 50 lb por $20 ($0.40/lb), usas 40 lb, te quedan 10.
            // Compras 100 lb más por $50. El precio correcto debe salir de lo que
            // REALMENTE tienes: (10 lb x $0.40 = $4) + $50 = $54, entre 110 lb =
            // $0.4909/lb. Usar el total histórico (50+100=150 lb, $20+$50=$70)
            // daría $0.4667/lb, un precio más bajo del real porque diluye con
            // dinero de mercancía que ya no existe físicamente.
            double valorDisponibleActual = MathUtils.redondear(existente.getCantidadDisponible() * existente.getPrecioPorUnidad());
            double nuevoValorDisponible = MathUtils.redondear(valorDisponibleActual + precioNuevoIngreso);
            double nuevoPrecioPorUnidad = MathUtils.redondear(
                nuevaCantidadDisponible > 0 ? nuevoValorDisponible / nuevaCantidadDisponible : 0, 4);
            
            int nuevosSacos = (existente.getNumeroSacos() != null ? existente.getNumeroSacos() : 0) + 
                              (mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0);
            
            double nuevoPesoSaco = existente.getPesoSaco() != null ? existente.getPesoSaco() : 0;
            if (nuevoPesoSaco == 0 && mp.getPesoSaco() != null && mp.getPesoSaco() > 0) {
                nuevoPesoSaco = mp.getPesoSaco();
            }
            
            double nuevasLibrasSueltas = MathUtils.redondear((existente.getLibrasSueltas() != null ? existente.getLibrasSueltas() : 0) + 
                                          (mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0));
            
            String sql = "UPDATE materias_primas SET " +
                         "nombre = ?, cantidad_total = ?, cantidad_disponible = ?, precio_total = ?, " +
                         "precio_por_unidad = ?, numero_sacos = ?, peso_saco = ?, libras_sueltas = ? " +
                         "WHERE id = ?";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, mp.getNombre());
            ps.setDouble(2, nuevaCantidadTotal);
            ps.setDouble(3, nuevaCantidadDisponible);
            ps.setDouble(4, nuevoPrecioTotal);
            ps.setDouble(5, nuevoPrecioPorUnidad);
            ps.setInt(6, nuevosSacos);
            ps.setDouble(7, nuevoPesoSaco);
            ps.setDouble(8, nuevasLibrasSueltas);
            ps.setInt(9, productoId);
            ps.executeUpdate();
            ps.close();
            
            mp.setCantidadTotal(nuevaCantidadTotal);
            mp.setCantidadDisponible(nuevaCantidadDisponible);
            mp.setPrecioTotal(nuevoPrecioTotal);
            mp.setPrecioPorUnidad(nuevoPrecioPorUnidad);
            mp.setNumeroSacos(nuevosSacos);
            mp.setPesoSaco(nuevoPesoSaco);
            mp.setLibrasSueltas(nuevasLibrasSueltas);
            
            datosNuevos = String.format("id:%d,nombre:%s,total:%.2f,disponible:%.2f,precio_total:%.2f,sacos:%d",
                mp.getId(), mp.getNombre(), mp.getCantidadTotal(),
                mp.getCantidadDisponible(), mp.getPrecioTotal(),
                mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0);
            
        } else {
            mp.setFecha(LocalDate.now().toString());
            mp.setHora(LocalTime.now().format(TIME_FMT));
            
            if (mp.getCodigo() == null || mp.getCodigo().isEmpty()) {
                mp.setCodigo(generarCodigo(mp.getNombre()));
            }
            
            if (mp.getOrigen() == null || mp.getOrigen().isEmpty()) {
                mp.setOrigen("INVENTARIO");
            }
            
            String sql = "INSERT INTO materias_primas (" +
                         "codigo, nombre, unidad, cantidad_total, cantidad_disponible, " +
                         "precio_total, precio_por_unidad, fecha_ingreso, hora_ingreso, " +
                         "peso_saco, numero_sacos, libras_sueltas, origen) " +
                         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, mp.getCodigo());
            ps.setString(2, mp.getNombre());
            ps.setString(3, mp.getUnidad());
            ps.setDouble(4, mp.getCantidadTotal());
            ps.setDouble(5, mp.getCantidadDisponible());
            ps.setDouble(6, mp.getPrecioTotal());
            ps.setDouble(7, mp.getPrecioPorUnidad());
            ps.setString(8, mp.getFecha());
            ps.setString(9, mp.getHora());
            ps.setDouble(10, mp.getPesoSaco() != null ? mp.getPesoSaco() : 0);
            ps.setInt(11, mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0);
            ps.setDouble(12, mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0);
            ps.setString(13, mp.getOrigen());
            ps.executeUpdate();
            ps.close();
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rsId = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rsId.next()) {
                    productoId = rsId.getInt(1);
                    mp.setId(productoId);
                } else {
                    throw new SQLException("No se pudo obtener el ID del producto");
                }
            }
            
            datosNuevos = String.format("id:%d,nombre:%s,total:%.2f,disponible:%.2f,precio_total:%.2f,sacos:%d,origen:%s",
                mp.getId(), mp.getNombre(), mp.getCantidadTotal(),
                mp.getCantidadDisponible(), mp.getPrecioTotal(),
                mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0,
                mp.getOrigen());
            
            cantidadParaHistorial = mp.getCantidadTotal();
            precioUnitarioParaHistorial = mp.getPrecioPorUnidad();
            precioTotalParaHistorial = mp.getPrecioTotal();
            numSacosParaHistorial = mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0;
            pesoSacoParaHistorial = mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
            librasSueltasParaHistorial = mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0;
        }
        
        // ============================================================
        //  REGISTRAR EN EL HISTORIAL CON ORIGEN
        //  IMPORTANTE: se guarda el valor INDIVIDUAL de este ingreso,
        //  nunca el acumulado (mp ya fue sobrescrito arriba con los totales
        //  cuando el producto ya existia).
        // ============================================================
        try {
            String sqlHistorial = "INSERT INTO ingresos_historial (" +
                                  "producto_id, nombre_producto, unidad, cantidad, precio_unitario, " +
                                  "precio_total, numero_sacos, peso_saco, libras_sueltas, " +
                                  "fecha_ingreso, hora_ingreso, compra_id, pedido_id, cantidad_usada, origen) " +
                                  "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            
            ps = conn.prepareStatement(sqlHistorial);
            ps.setInt(1, productoId);
            ps.setString(2, mp.getNombre());
            ps.setString(3, mp.getUnidad());
            ps.setDouble(4, cantidadParaHistorial);
            ps.setDouble(5, precioUnitarioParaHistorial);
            ps.setDouble(6, precioTotalParaHistorial);
            ps.setInt(7, numSacosParaHistorial);
            ps.setDouble(8, pesoSacoParaHistorial);
            ps.setDouble(9, librasSueltasParaHistorial);
            ps.setString(10, LocalDate.now().toString());
            ps.setString(11, LocalTime.now().format(TIME_FMT));
            ps.setObject(12, compraId);
            ps.setObject(13, null);
            ps.setDouble(14, 0);
            ps.setString(15, mp.getOrigen() != null ? mp.getOrigen() : "INVENTARIO");
            ps.executeUpdate();
            ps.close();
            
        } catch (SQLException e) {
            System.err.println("Error al registrar en historial: " + e.getMessage());
            LoggerUtil.logError("Error registrando historial para producto " + productoId, e);
        }
        
        // Preparar el movimiento (el INSERT real se hace DESPUÉS del commit,
        // ver más abajo: registrarMovimiento() abre su propia conexión, y si
        // se llama aquí mientras esta transacción principal sigue sin confirmar,
        // SQLite bloquea esa segunda conexión hasta que esta libere el lock —
        // lo cual fallaba en silencio (capturado por el catch) y dejaba los
        // movimientos de tipo INGRESO sin registrar, afectando los reportes.
        movimientoIngreso = new MovimientoInventario();
        movimientoIngreso.setMateriaPrimaId(productoId);
        movimientoIngreso.setNombreProducto(mp.getNombre());
        movimientoIngreso.setUnidad(mp.getUnidad());
        movimientoIngreso.setTipoMovimiento("INGRESO");
        // IMPORTANTE: usar los valores INDIVIDUALES de este ingreso (los mismos
        // que se guardan en ingresos_historial), NO el total acumulado del
        // producto (mp ya fue sobrescrito con los totales cuando el producto
        // ya existia). De lo contrario "Movimientos" muestra cifras infladas
        // o inconsistentes con lo que realmente se ingresó cada vez.
        movimientoIngreso.setCantidad(cantidadParaHistorial);
        movimientoIngreso.setPrecioPorUnidad(precioUnitarioParaHistorial);
        movimientoIngreso.setSubtotal(precioTotalParaHistorial);
        movimientoIngreso.setSacosAfectados(numSacosParaHistorial);
        movimientoIngreso.setLibrasSueltasAfectadas(librasSueltasParaHistorial);
        movimientoIngreso.setReferencia(compraId != null ? "Compra ID: " + compraId : "Ingreso de stock");
        movimientoIngreso.setCompraId(compraId);
        
        // Registrar auditoría DESPUÉS de cerrar la conexión principal (ver
        // comentario arriba sobre movimientoIngreso): auditoriaDAO abre su
        // propia conexión, y si se llama aquí mientras esta transacción
        // sigue sin confirmar, esa segunda conexión choca contra el candado
        // de escritura que esta misma transacción todavía tiene abierto —
        // quedándose esperando a sí misma hasta agotar el busy_timeout
        // (30 segundos) en CADA guardado. Se mueve fuera, junto al movimiento.

        conn.commit();

    } catch (SQLException e) {
        if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
        throw e;
    } finally {
        if (conn != null) { 
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } 
        }
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
    }

    // Ahora que la transacción principal ya confirmó y liberó su conexión,
    // es seguro que auditoriaDAO y registrarMovimiento() abran la suya
    // propia sin bloquearse ni esperar el busy_timeout.
    try {
        if (datosAnteriores != null) {
            auditoriaDAO.registrarUpdate("materias_primas", productoId, datosAnteriores, datosNuevos);
        } else {
            auditoriaDAO.registrarInsert("materias_primas", productoId, datosNuevos);
        }
    } catch (Exception e) {
        System.err.println("Error registrando auditoría: " + e.getMessage());
        LoggerUtil.logError("Error registrando auditoría para producto " + productoId, e);
    }

    if (movimientoIngreso != null) {
        try {
            movimientoDAO.registrarMovimiento(movimientoIngreso);
        } catch (Exception e) {
            System.err.println("Error registrando movimiento: " + e.getMessage());
            LoggerUtil.logError("Error registrando movimiento para producto " + productoId, e);
        }
    }
}

    public double consumirStockFIFO(int productoId, double cantidad, String referencia) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);
            
            MateriaPrima mp = buscarPorId(productoId);
            if (mp == null) {
                throw new SQLException("Producto no encontrado: ID " + productoId);
            }
            
            if (mp.getCantidadDisponible() < cantidad) {
                throw new SQLException("Stock insuficiente. Disponible: " + mp.getCantidadDisponible() + 
                    ", Necesario: " + cantidad);
            }
            
            String sqlLotes = "SELECT * FROM ingresos_historial " +
                              "WHERE producto_id = ? AND (cantidad - cantidad_usada) > 0 " +
                              "ORDER BY fecha_ingreso ASC, id ASC";
            ps = conn.prepareStatement(sqlLotes);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            
            List<IngresoHistorial> lotes = new ArrayList<>();
            while (rs.next()) {
                IngresoHistorial lote = new IngresoHistorial();
                lote.setId(rs.getInt("id"));
                lote.setProductoId(rs.getInt("producto_id"));
                lote.setCantidad(rs.getDouble("cantidad"));
                lote.setCantidadUsada(rs.getDouble("cantidad_usada"));
                lote.setPrecioUnitario(rs.getDouble("precio_unitario"));
                lote.setFecha(rs.getString("fecha_ingreso"));
                lote.setHora(rs.getString("hora_ingreso"));
                lotes.add(lote);
            }
            rs.close();
            ps.close();
            ps = null;
            
            if (lotes.isEmpty()) {
                throw new SQLException("No hay lotes disponibles para consumir.");
            }
            
            double cantidadRestante = cantidad;
            double costoTotal = 0;
            
            for (IngresoHistorial lote : lotes) {
                if (cantidadRestante <= 0) break;
                
                double disponibleLote = lote.getCantidad() - lote.getCantidadUsada();
                if (disponibleLote <= 0) continue;
                
                double cantidadTomar = Math.min(cantidadRestante, disponibleLote);
                double nuevaCantidadUsada = lote.getCantidadUsada() + cantidadTomar;
                
                String sqlUpdateLote = "UPDATE ingresos_historial SET cantidad_usada = ? WHERE id = ?";
                ps = conn.prepareStatement(sqlUpdateLote);
                ps.setDouble(1, nuevaCantidadUsada);
                ps.setInt(2, lote.getId());
                ps.executeUpdate();
                ps.close();
                ps = null;
                
                costoTotal += cantidadTomar * lote.getPrecioUnitario();
                cantidadRestante -= cantidadTomar;
            }
            
            double nuevoDisponible = mp.getCantidadDisponible() - cantidad;
            if (nuevoDisponible < 0) nuevoDisponible = 0;
            
            actualizarDisponible(productoId, nuevoDisponible, referencia);
            
            MovimientoInventario movimientoUso = new MovimientoInventario();
            movimientoUso.setMateriaPrimaId(productoId);
            movimientoUso.setNombreProducto(mp.getNombre());
            movimientoUso.setUnidad(mp.getUnidad());
            movimientoUso.setTipoMovimiento("USO");
            movimientoUso.setCantidad(cantidad);
            movimientoUso.setPrecioPorUnidad(costoTotal / cantidad);
            movimientoUso.setSubtotal(costoTotal);
            movimientoUso.setReferencia(referencia);
            
            conn.commit();
            
            LoggerUtil.logInfo("FIFO: Consumido " + cantidad + " " + mp.getUnidad() + 
                " de " + mp.getNombre() + " - Costo real: $" + costoTotal);
            
            // Se registra despues del commit -- ver nota en guardarOActualizarInterno.
            // registrarMovimiento() abre su propia conexión, y hacerlo mientras esta
            // transacción seguía sin confirmar podía bloquearse esperando el lock.
            try {
                movimientoDAO.registrarMovimiento(movimientoUso);
            } catch (Exception e) {
                System.err.println("Error registrando movimiento USO: " + e.getMessage());
                LoggerUtil.logError("Error registrando movimiento USO para producto " + productoId, e);
            }
            
            return costoTotal;
            
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) { 
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } 
            }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public double calcularCostoFIFO(int productoId, double cantidad) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            String sqlLotes = "SELECT * FROM ingresos_historial " +
                              "WHERE producto_id = ? AND (cantidad - cantidad_usada) > 0 " +
                              "ORDER BY fecha_ingreso ASC, id ASC";
            ps = conn.prepareStatement(sqlLotes);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            
            List<IngresoHistorial> lotes = new ArrayList<>();
            while (rs.next()) {
                IngresoHistorial lote = new IngresoHistorial();
                lote.setId(rs.getInt("id"));
                lote.setCantidad(rs.getDouble("cantidad"));
                lote.setCantidadUsada(rs.getDouble("cantidad_usada"));
                lote.setPrecioUnitario(rs.getDouble("precio_unitario"));
                lotes.add(lote);
            }
            rs.close();
            ps.close();
            
            if (lotes.isEmpty()) {
                return 0;
            }
            
            double cantidadRestante = cantidad;
            double costoTotal = 0;
            
            for (IngresoHistorial lote : lotes) {
                if (cantidadRestante <= 0) break;
                
                double disponibleLote = lote.getCantidad() - lote.getCantidadUsada();
                if (disponibleLote <= 0) continue;
                
                double cantidadTomar = Math.min(cantidadRestante, disponibleLote);
                costoTotal += cantidadTomar * lote.getPrecioUnitario();
                cantidadRestante -= cantidadTomar;
            }
            
            return costoTotal;
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public List<IngresoHistorial> obtenerLotesDisponibles(int productoId) throws SQLException {
        List<IngresoHistorial> lotes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM ingresos_historial " +
                         "WHERE producto_id = ? AND (cantidad - cantidad_usada) > 0 " +
                         "ORDER BY fecha_ingreso ASC, id ASC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                IngresoHistorial lote = new IngresoHistorial();
                lote.setId(rs.getInt("id"));
                lote.setProductoId(rs.getInt("producto_id"));
                lote.setNombreProducto(rs.getString("nombre_producto"));
                lote.setUnidad(rs.getString("unidad"));
                lote.setCantidad(rs.getDouble("cantidad"));
                lote.setCantidadUsada(rs.getDouble("cantidad_usada"));
                lote.setPrecioUnitario(rs.getDouble("precio_unitario"));
                lote.setPrecioTotal(rs.getDouble("precio_total"));
                lote.setNumeroSacos(rs.getInt("numero_sacos"));
                lote.setPesoSaco(rs.getDouble("peso_saco"));
                lote.setLibrasSueltas(rs.getDouble("libras_sueltas"));
                lote.setFecha(rs.getString("fecha_ingreso"));
                lote.setHora(rs.getString("hora_ingreso"));
                lotes.add(lote);
            }
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lotes;
    }

    public MateriaPrima buscarPorNombreNormalizado(String nombre) throws SQLException {
        if (nombre == null) return null;
        nombre = nombre.trim().toUpperCase();
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM materias_primas WHERE UPPER(TRIM(nombre)) = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, nombre);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public MateriaPrima buscarPorNombre(String nombre) throws SQLException {
        if (nombre == null) return null;
        return buscarPorNombreNormalizado(nombre.trim().toUpperCase());
    }

    public MateriaPrima buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM materias_primas WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public MateriaPrima buscarPorCodigo(String codigo) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM materias_primas WHERE codigo = ?");
            ps.setString(1, codigo);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public String obtenerOrigen(int productoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT origen FROM materias_primas WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("origen");
            }
            return "INVENTARIO";
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

        public void actualizarProducto(MateriaPrima mp) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            String sql = "UPDATE materias_primas SET " +
                         "nombre = ?, unidad = ?, cantidad_total = ?, cantidad_disponible = ?, " +
                         "precio_total = ?, precio_por_unidad = ?, numero_sacos = ?, " +
                         "peso_saco = ?, libras_sueltas = ?, origen = ? " +
                         "WHERE id = ?";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, mp.getNombre());
            ps.setString(2, mp.getUnidad());
            ps.setDouble(3, mp.getCantidadTotal());
            ps.setDouble(4, mp.getCantidadDisponible());
            ps.setDouble(5, mp.getPrecioTotal());
            ps.setDouble(6, mp.getPrecioPorUnidad());
            ps.setInt(7, mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0);
            ps.setDouble(8, mp.getPesoSaco() != null ? mp.getPesoSaco() : 0);
            ps.setDouble(9, mp.getLibrasSueltas() != null ? mp.getLibrasSueltas() : 0);
            ps.setString(10, mp.getOrigen() != null ? mp.getOrigen() : "INVENTARIO");
            ps.setInt(11, mp.getId());
            ps.executeUpdate();
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    public void actualizarDisponible(int id, double nuevaCantidad, String referencia) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            MateriaPrima anterior = buscarPorId(id);
            String datosAnteriores = anterior != null ? 
                String.format("id:%d,disponible:%.2f", id, anterior.getCantidadDisponible()) : null;
            
            if (nuevaCantidad < 0) {
                nuevaCantidad = 0;
            }
            
            String sql = "UPDATE materias_primas SET cantidad_disponible = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, nuevaCantidad);
            ps.setInt(2, id);
            ps.executeUpdate();
            
            String datosNuevos = String.format("id:%d,disponible:%.2f", id, nuevaCantidad);
            auditoriaDAO.registrarUpdate("materias_primas", id, datosAnteriores, datosNuevos);
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    public List<MateriaPrima> listarTodos() throws SQLException {
        List<MateriaPrima> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM materias_primas ORDER BY nombre");
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public List<MateriaPrima> listarTodas() throws SQLException {
        return listarTodos();
    }

    public List<MateriaPrima> listarAgrupados() throws SQLException {
        return listarTodos();
    }

    public List<MateriaPrima> listarConStock() throws SQLException {
        List<MateriaPrima> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM materias_primas WHERE cantidad_disponible > 0 ORDER BY nombre");
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
        return lista;
    }

    public boolean estaEnFormulacion(int materiaPrimaId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) as total FROM formulacion_detalle WHERE materia_prima_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, materiaPrimaId);
            rs = ps.executeQuery();
            return rs.next() && rs.getInt("total") > 0;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            if (estaEnFormulacion(id)) {
                throw new SQLException("No se puede eliminar el producto porque está siendo usado en una o más formulaciones.");
            }
            
            MateriaPrima mp = buscarPorId(id);
            String datos = mp != null ? 
                String.format("id:%d,nombre:%s,total:%.2f,disponible:%.2f,origen:%s",
                    mp.getId(), mp.getNombre(), mp.getCantidadTotal(), mp.getCantidadDisponible(), mp.getOrigen()) : null;
            
            ps = conn.prepareStatement("DELETE FROM materias_primas WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            
            auditoriaDAO.registrarDelete("materias_primas", id, datos);
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    public void eliminarPorNombre(String nombre) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            MateriaPrima mp = buscarPorNombre(nombre);
            if (mp == null) {
                throw new SQLException("Producto no encontrado: " + nombre);
            }
            
            int productoId = mp.getId();
            
            if (estaEnFormulacion(productoId)) {
                throw new SQLException("No se puede eliminar el producto '" + nombre + 
                    "' porque está siendo usado en una o más formulaciones.");
            }
            
            String sqlMovimientos = "DELETE FROM movimientos_inventario WHERE materia_prima_id = ?";
            ps = conn.prepareStatement(sqlMovimientos);
            ps.setInt(1, productoId);
            ps.executeUpdate();
            ps.close();
            
            String sqlDetalles = "DELETE FROM formulacion_detalle WHERE materia_prima_id = ?";
            ps = conn.prepareStatement(sqlDetalles);
            ps.setInt(1, productoId);
            ps.executeUpdate();
            ps.close();
            
            String sqlProducto = "DELETE FROM materias_primas WHERE id = ?";
            ps = conn.prepareStatement(sqlProducto);
            ps.setInt(1, productoId);
            ps.executeUpdate();
            
            String datos = String.format("id:%d,nombre:%s,total:%.2f,disponible:%.2f,origen:%s",
                mp.getId(), mp.getNombre(), mp.getCantidadTotal(), mp.getCantidadDisponible(), mp.getOrigen());
            auditoriaDAO.registrarDelete("materias_primas", productoId, datos);
            
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    public void insertar(MateriaPrima mp) throws SQLException {
        guardarOActualizar(mp);
    }

    private String generarCodigo(String nombre) throws SQLException {
        String prefijo = nombre.length() >= 3 ? nombre.substring(0, 3).toUpperCase() : nombre.toUpperCase();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) FROM materias_primas WHERE codigo LIKE ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, prefijo + "%");
            rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt(1) : 0;
            return prefijo + "-" + String.format("%03d", count + 1);
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    private MateriaPrima mapear(ResultSet rs) throws SQLException {
        MateriaPrima mp = new MateriaPrima();
        mp.setId(rs.getInt("id"));
        mp.setCodigo(rs.getString("codigo"));
        mp.setNombre(rs.getString("nombre"));
        mp.setUnidad(rs.getString("unidad"));
        mp.setCantidadTotal(rs.getDouble("cantidad_total"));
        mp.setCantidadDisponible(rs.getDouble("cantidad_disponible"));
        mp.setPrecioTotal(rs.getDouble("precio_total"));
        mp.setPrecioPorUnidad(rs.getDouble("precio_por_unidad"));
        mp.setFecha(rs.getString("fecha_ingreso"));
        mp.setHora(rs.getString("hora_ingreso"));
        mp.setPesoSaco(rs.getDouble("peso_saco"));
        mp.setNumeroSacos(rs.getInt("numero_sacos"));
        
        try {
            mp.setLibrasSueltas(rs.getDouble("libras_sueltas"));
        } catch (SQLException e) {
            mp.setLibrasSueltas(0.0);
        }
        
        try {
            mp.setOrigen(rs.getString("origen"));
        } catch (SQLException e) {
            mp.setOrigen("INVENTARIO");
        }
        
        try {
            mp.setImagenPath(rs.getString("imagen_path"));
        } catch (SQLException e) {
            mp.setImagenPath(null);
        }
        
        return mp;
    }

    /** Actualiza únicamente la ruta de la imagen de fondo de un producto,
     *  sin tocar ningún otro campo (cantidades, precios, etc.). */
    public void actualizarImagen(int id, String rutaImagen) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("UPDATE materias_primas SET imagen_path = ? WHERE id = ?");
            ps.setString(1, rutaImagen);
            ps.setInt(2, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }
}