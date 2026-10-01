package com.feedformulator.dao;

import com.feedformulator.model.DetallePedido;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.PagoPedido;
import com.feedformulator.model.Pedido;
import com.feedformulator.model.Proveedor;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.feedformulator.model.MovimientoInventario;
import com.feedformulator.dao.MovimientoInventarioDAO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.feedformulator.util.LoggerUtil;
import com.feedformulator.util.*;
public class PedidoDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public void guardar(Pedido pedido) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean recibirAutomatico = false;
        String datos = null;
        
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            pedido.setFechaPedido(LocalDate.now().format(DATE_FMT));
            pedido.setAbonado(0);
            pedido.setSaldoPendiente(pedido.getTotalPedido());
            
            if (pedido.getEstadoPago() == null) {
                pedido.setEstadoPago("PENDIENTE");
            }
            
            recibirAutomatico = "RECIBIDA".equals(pedido.getEstadoRecepcion());
            
            if (recibirAutomatico) {
                pedido.setEstadoRecepcion("PENDIENTE");
            }
            
            String sql = "INSERT INTO pedidos (proveedor_id, fecha_pedido, estado_pago, estado_recepcion, abonado, saldo_pendiente, notas) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, pedido.getProveedorId());
            ps.setString(2, pedido.getFechaPedido());
            ps.setString(3, pedido.getEstadoPago());
            ps.setString(4, pedido.getEstadoRecepcion());
            ps.setDouble(5, pedido.getAbonado());
            ps.setDouble(6, pedido.getSaldoPendiente());
            ps.setString(7, pedido.getNotas());
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    pedido.setId(rs.getInt(1));
                }
            }
            
            String sqlDetalle = "INSERT INTO detalle_pedido (" +
                                "pedido_id, materia_prima_id, nombre_producto, unidad, cantidad, " +
                                "precio_unitario, subtotal, numero_sacos, peso_saco, libras_sueltas) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sqlDetalle);
            
            for (DetallePedido detalle : pedido.getDetalles()) {
                detalle.setPedidoId(pedido.getId());
                ps.setInt(1, pedido.getId());
                ps.setInt(2, detalle.getMateriaPrimaId());
                ps.setString(3, detalle.getNombreProducto());
                ps.setString(4, detalle.getUnidad());
                ps.setDouble(5, detalle.getCantidad());
                ps.setDouble(6, detalle.getPrecioUnitario());
                ps.setDouble(7, detalle.getSubtotal());
                ps.setObject(8, detalle.getNumeroSacos());
                ps.setObject(9, detalle.getPesoSaco());
                ps.setObject(10, detalle.getLibrasSueltas());
                ps.executeUpdate();
            }
            
            datos = String.format("pedido_id:%d,proveedor:%d,total:%.2f,estado_pago:%s,estado_recepcion:%s",
                pedido.getId(), pedido.getProveedorId(), pedido.getTotalPedido(),
                pedido.getEstadoPago(), pedido.getEstadoRecepcion());

            conn.commit();
            System.out.println("✅ Pedido #" + pedido.getId() + " guardado correctamente.");

        } catch (SQLException e) {
            if (conn != null) { 
                try { 
                    conn.rollback(); 
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                } 
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
            DatabaseManager.closeResources(null, ps, rs);
        }
        
        // Auditoría DESPUÉS de cerrar la conexión (evita el auto-bloqueo de
        // 30s por busy_timeout que ocurría al abrir una segunda conexión
        // mientras esta transacción seguía sin confirmar).
        try {
            auditoriaDAO.registrarInsert("pedidos", pedido.getId(), datos);
        } catch (Exception e) {
            System.err.println("Error registrando auditoría de pedido: " + e.getMessage());
        }

        if (recibirAutomatico) {
            try {
                System.out.println("📦 Recibiendo pedido #" + pedido.getId() + " automáticamente...");
                recibirPedido(pedido.getId());
            } catch (SQLException e) {
                System.err.println("⚠️ Error al recibir el pedido #" + pedido.getId() + ": " + e.getMessage());
                throw e;
            }
        }
    }

    public void recibirPedido(int pedidoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Pedido p = buscarPorId(pedidoId);
            if (p == null) {
                throw new SQLException("Pedido no encontrado");
            }

            if ("RECIBIDA".equals(p.getEstadoRecepcion())) {
                throw new SQLException("Este pedido ya fue recibido");
            }

            MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();
            
            for (DetallePedido detalle : p.getDetalles()) {
                // Se usa la MISMA conexión (conn) en vez de materiaDAO.buscarPorId(),
                // que abre una conexión nueva y podía bloquearse si un producto
                // anterior en este mismo bucle ya escribió cambios sin confirmar.
                MateriaPrima mp = null;
                String sqlBuscarMp = "SELECT * FROM materias_primas WHERE id = ?";
                PreparedStatement psBuscarMp = conn.prepareStatement(sqlBuscarMp);
                psBuscarMp.setInt(1, detalle.getMateriaPrimaId());
                ResultSet rsBuscarMp = psBuscarMp.executeQuery();
                if (rsBuscarMp.next()) {
                    mp = new MateriaPrima();
                    mp.setId(rsBuscarMp.getInt("id"));
                    mp.setNombre(rsBuscarMp.getString("nombre"));
                    mp.setUnidad(rsBuscarMp.getString("unidad"));
                    mp.setCantidadTotal(rsBuscarMp.getDouble("cantidad_total"));
                    mp.setCantidadDisponible(rsBuscarMp.getDouble("cantidad_disponible"));
                    mp.setPrecioTotal(rsBuscarMp.getDouble("precio_total"));
                    mp.setPrecioPorUnidad(rsBuscarMp.getDouble("precio_por_unidad"));
                    mp.setNumeroSacos(rsBuscarMp.getInt("numero_sacos"));
                    mp.setPesoSaco(rsBuscarMp.getDouble("peso_saco"));
                    mp.setLibrasSueltas(rsBuscarMp.getDouble("libras_sueltas"));
                }
                rsBuscarMp.close();
                psBuscarMp.close();
                
                if (mp == null) {
                    // IMPORTANTE: antes esto llamaba a materiaDAO.guardarRapido(mp),
                    // que abre su propia conexión nueva mientras esta transacción
                    // (conn) seguía abierta y sin confirmar — causando el mismo
                    // bloqueo "SQLITE_BUSY: database is locked". Ahora se crea el
                    // producto usando la MISMA conexión de esta transacción.
                    String sqlCrearMp = "INSERT INTO materias_primas " +
                        "(codigo, nombre, unidad, cantidad_total, cantidad_disponible, precio_total, precio_por_unidad, " +
                        "numero_sacos, peso_saco, libras_sueltas, fecha_ingreso, hora_ingreso, origen) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PEDIDO')";
                    PreparedStatement psCrearMp = conn.prepareStatement(sqlCrearMp);
                    psCrearMp.setString(1, generarCodigoProducto(conn, detalle.getNombreProducto()));
                    psCrearMp.setString(2, detalle.getNombreProducto());
                    psCrearMp.setString(3, detalle.getUnidad());
                    psCrearMp.setDouble(4, detalle.getCantidad());
                    psCrearMp.setDouble(5, detalle.getCantidad());
                    psCrearMp.setDouble(6, detalle.getSubtotal());
                    psCrearMp.setDouble(7, detalle.getPrecioUnitario());
                    psCrearMp.setInt(8, detalle.getNumeroSacos() != null ? detalle.getNumeroSacos() : 0);
                    psCrearMp.setDouble(9, detalle.getPesoSaco() != null ? detalle.getPesoSaco() : 0);
                    psCrearMp.setDouble(10, detalle.getLibrasSueltas() != null ? detalle.getLibrasSueltas() : 0);
                    psCrearMp.setString(11, LocalDate.now().toString());
                    psCrearMp.setString(12, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                    psCrearMp.executeUpdate();
                    psCrearMp.close();

                    int nuevoMpId;
                    try (Statement stmtMpId = conn.createStatement();
                         ResultSet rsMpId = stmtMpId.executeQuery("SELECT last_insert_rowid()")) {
                        rsMpId.next();
                        nuevoMpId = rsMpId.getInt(1);
                    }
                    mp = new MateriaPrima();
                    mp.setId(nuevoMpId);
                    mp.setNombre(detalle.getNombreProducto());

                    System.out.println("📦 Producto creado con stock: " + detalle.getNombreProducto() + " (Origen: PEDIDO)");
                } else {
                    if (mp.getCantidadDisponible() == 0 && mp.getCantidadTotal() == 0) {
                        mp.setCantidadTotal(detalle.getCantidad());
                        mp.setCantidadDisponible(detalle.getCantidad());
                        mp.setPrecioTotal(detalle.getSubtotal());
                        mp.setPrecioPorUnidad(detalle.getPrecioUnitario());
                        mp.setNumeroSacos(detalle.getNumeroSacos() != null ? detalle.getNumeroSacos() : 0);
                        mp.setPesoSaco(detalle.getPesoSaco() != null ? detalle.getPesoSaco() : 0);
                        mp.setLibrasSueltas(detalle.getLibrasSueltas() != null ? detalle.getLibrasSueltas() : 0);

                        // Se usa la MISMA conexión (conn) — ver nota más arriba sobre
                        // por qué materiaDAO.actualizarProducto() causaba SQLITE_BUSY.
                        String sqlAsignar = "UPDATE materias_primas SET cantidad_total=?, cantidad_disponible=?, " +
                            "precio_total=?, precio_por_unidad=?, numero_sacos=?, peso_saco=?, libras_sueltas=? WHERE id=?";
                        PreparedStatement psAsignar = conn.prepareStatement(sqlAsignar);
                        psAsignar.setDouble(1, mp.getCantidadTotal());
                        psAsignar.setDouble(2, mp.getCantidadDisponible());
                        psAsignar.setDouble(3, mp.getPrecioTotal());
                        psAsignar.setDouble(4, mp.getPrecioPorUnidad());
                        psAsignar.setInt(5, mp.getNumeroSacos());
                        psAsignar.setDouble(6, mp.getPesoSaco());
                        psAsignar.setDouble(7, mp.getLibrasSueltas());
                        psAsignar.setInt(8, mp.getId());
                        psAsignar.executeUpdate();
                        psAsignar.close();

                        System.out.println("📦 Stock asignado a producto existente: " + mp.getNombre() + 
                            " | +" + detalle.getCantidad());
                    } else {
                       // ============================================================
                       //  PRODUCTO CON STOCK EXISTENTE - SUMAR
                       //  El peso del saco se mantiene del primer registro
                       // ============================================================
                       double nuevoTotal = mp.getCantidadTotal() + detalle.getCantidad();
                       double nuevoDisponible = mp.getCantidadDisponible() + detalle.getCantidad();
                       double nuevoPrecioTotal = mp.getPrecioTotal() + detalle.getSubtotal();

                       // IMPORTANTE: el precio por unidad se calcula con el VALOR DE
                       // LO QUE REALMENTE QUEDA DISPONIBLE, no con el total histórico
                       // comprado (que no baja aunque ya se haya usado material en
                       // fórmulas). Usar el total histórico "diluye" el precio con
                       // dinero de mercancía que ya no existe físicamente.
                       double valorDisponibleActual = mp.getCantidadDisponible() * mp.getPrecioPorUnidad();
                       double nuevoValorDisponible = valorDisponibleActual + detalle.getSubtotal();
                       double nuevoPrecioUnitario = nuevoDisponible > 0 ? nuevoValorDisponible / nuevoDisponible : 0;

                       int nuevosSacos = (mp.getNumeroSacos() != null ? mp.getNumeroSacos() : 0) + 
                                         (detalle.getNumeroSacos() != null ? detalle.getNumeroSacos() : 0);

                       // ============================================================
                       //  CORRECCIÓN: El peso del saco NO se actualiza.
                       //  Se mantiene el que ya tiene el producto (del primer registro)
                       //  SOLO se actualiza si el producto no tiene peso definido (0)
                       // ============================================================
                       double nuevoPesoSaco = mp.getPesoSaco() != null ? mp.getPesoSaco() : 0;
                       // Si el producto tiene peso 0 (nunca se definió) y el nuevo pedido tiene peso, se usa ese
                       if (nuevoPesoSaco == 0 && detalle.getPesoSaco() != null && detalle.getPesoSaco() > 0) {
                           nuevoPesoSaco = detalle.getPesoSaco();
                       }

                       // Calcular libras sueltas: total - (sacos * peso_saco)
                       double nuevasSueltas = nuevoTotal - (nuevosSacos * nuevoPesoSaco);
                       if (nuevasSueltas < 0) nuevasSueltas = 0;

                       // Se usa la MISMA conexión (conn) — ver nota más arriba sobre
                       // por qué materiaDAO.actualizarProducto() causaba SQLITE_BUSY.
                       String sqlSumar = "UPDATE materias_primas SET cantidad_total=?, cantidad_disponible=?, " +
                           "precio_total=?, precio_por_unidad=?, numero_sacos=?, peso_saco=?, libras_sueltas=? WHERE id=?";
                       PreparedStatement psSumar = conn.prepareStatement(sqlSumar);
                       psSumar.setDouble(1, nuevoTotal);
                       psSumar.setDouble(2, nuevoDisponible);
                       psSumar.setDouble(3, nuevoPrecioTotal);
                       psSumar.setDouble(4, nuevoPrecioUnitario);
                       psSumar.setInt(5, nuevosSacos);
                       psSumar.setDouble(6, nuevoPesoSaco);
                       psSumar.setDouble(7, nuevasSueltas);
                       psSumar.setInt(8, mp.getId());
                       psSumar.executeUpdate();
                       psSumar.close();

                       System.out.println("📦 Stock sumado a producto existente: " + mp.getNombre() + 
                           " | +" + detalle.getCantidad() + 
                           " | Sacos: " + nuevosSacos + 
                           " | Peso saco: " + nuevoPesoSaco + 
                           " | Sueltas: " + nuevasSueltas);
                    }
                }
                
                String sqlMov = "INSERT INTO movimientos_inventario (" +
                    "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                    "cantidad, precio_por_unidad, subtotal, fecha, hora, referencia, pedido_id) " +
                    "VALUES (?, ?, ?, 'INGRESO', ?, ?, ?, date('now'), time('now'), ?, ?)";
                
                ps = conn.prepareStatement(sqlMov);
                ps.setInt(1, mp.getId());
                ps.setString(2, mp.getNombre());
                ps.setString(3, mp.getUnidad());
                ps.setDouble(4, detalle.getCantidad());
                ps.setDouble(5, detalle.getPrecioUnitario());
                ps.setDouble(6, detalle.getSubtotal());
                ps.setString(7, "Recepción de pedido #" + pedidoId + " - " + detalle.getNombreProducto());
                ps.setInt(8, pedidoId);
                ps.executeUpdate();
                ps.close();
                ps = null;
            }
            
            String sqlUpdateEstado = "UPDATE pedidos SET estado_recepcion = 'RECIBIDA' WHERE id = ?";
            ps = conn.prepareStatement(sqlUpdateEstado);
            ps.setInt(1, pedidoId);
            ps.executeUpdate();
            ps.close();
            ps = null;

            conn.commit();
            System.out.println("📦 Pedido #" + pedidoId + " recibido.");

        } catch (SQLException e) {
            if (conn != null) { 
                try { 
                    conn.rollback(); 
                    System.out.println("⚠️ Rollback en recibirPedido");
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                } 
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
            DatabaseManager.closeResources(null, ps, rs);
        }
    }

    public void registrarAbono(int pedidoId, double monto) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        String datosAnteriores = null;
        String datosNuevos = null;
        
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Pedido p = buscarPorId(pedidoId);
            if (p == null) {
                throw new SQLException("Pedido no encontrado");
            }
            
            datosAnteriores = String.format("pedido_id:%d,abonado:%.2f,saldo:%.2f,estado_pago:%s",
                pedidoId, p.getAbonado(), p.getSaldoPendiente(), p.getEstadoPago());
            
            double nuevoAbonado = p.getAbonado() + monto;
            double nuevoSaldo = p.getTotalPedido() - nuevoAbonado;
            String nuevoEstadoPago = nuevoSaldo <= 0 ? "CANCELADO" : "ABONADO";
            if (nuevoSaldo < 0) nuevoSaldo = 0;
            
            String sql = "UPDATE pedidos SET abonado = ?, saldo_pendiente = ?, estado_pago = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, nuevoAbonado);
            ps.setDouble(2, nuevoSaldo);
            ps.setString(3, nuevoEstadoPago);
            ps.setInt(4, pedidoId);
            ps.executeUpdate();
            ps.close();
            
            String sqlPago = "INSERT INTO pagos_pedido (pedido_id, proveedor_id, monto, forma_pago, fecha_pago, hora_pago) " +
                             "VALUES (?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
            ps = conn.prepareStatement(sqlPago);
            ps.setInt(1, pedidoId);
            ps.setInt(2, p.getProveedorId());
            ps.setDouble(3, monto);
            ps.setString(4, "EFECTIVO");
            ps.executeUpdate();
            
            datosNuevos = String.format("pedido_id:%d,abonado:%.2f,saldo:%.2f,estado_pago:%s",
                pedidoId, nuevoAbonado, nuevoSaldo, nuevoEstadoPago);

            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); } }
            DatabaseManager.closeResources(conn, ps);
        }

        // Auditoría DESPUÉS de cerrar la conexión (evita el auto-bloqueo por
        // busy_timeout de 30s al abrir una segunda conexión mientras esta
        // transacción seguía sin confirmar).
        try {
            auditoriaDAO.registrarUpdate("pedidos", pedidoId, datosAnteriores, datosNuevos);
        } catch (Exception e) {
            System.err.println("Error registrando auditoría de abono: " + e.getMessage());
        }
    }

          public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean fkEstabaActivo = false;

        try {
            conn = DatabaseManager.getConnection();

            // ============================================================
            //  DESACTIVAR FOREIGN KEYS TEMPORALMENTE
            // ============================================================
            try (Statement stmt = conn.createStatement()) {
                ResultSet rsFk = stmt.executeQuery("PRAGMA foreign_keys");
                if (rsFk.next()) {
                    fkEstabaActivo = rsFk.getInt(1) == 1;
                }
                rsFk.close();
                stmt.execute("PRAGMA foreign_keys = OFF");
            }

            conn.setAutoCommit(false);

            // ============================================================
            //  1. VERIFICAR SI EL PEDIDO TIENE PAGOS
            // ============================================================
            String sqlCheckPagos = "SELECT COUNT(*) as total FROM pagos_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlCheckPagos);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            int totalPagos = rs.next() ? rs.getInt("total") : 0;
            rs.close();
            ps.close();
            ps = null;

            if (totalPagos > 0) {
                throw new SQLException("No se puede eliminar el pedido porque tiene " + totalPagos + 
                                       " pago(s) registrados. Primero elimine los pagos.");
            }

            // ============================================================
            //  2. OBTENER DATOS DEL PEDIDO
            // ============================================================
            String sqlPedido = "SELECT * FROM pedidos WHERE id = ?";
            ps = conn.prepareStatement(sqlPedido);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Pedido no encontrado");
            }

            String estadoRecepcion = rs.getString("estado_recepcion");
            rs.close();
            ps.close();
            ps = null;

            // ============================================================
            //  3. OBTENER DETALLES DEL PEDIDO
            // ============================================================
            String sqlDetalles = "SELECT * FROM detalle_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlDetalles);
            ps.setInt(1, id);
            rs = ps.executeQuery();

            List<DetallePedido> detalles = new ArrayList<>();
            while (rs.next()) {
                DetallePedido d = new DetallePedido();
                d.setId(rs.getInt("id"));
                d.setPedidoId(rs.getInt("pedido_id"));
                d.setMateriaPrimaId(rs.getInt("materia_prima_id"));
                d.setNombreProducto(rs.getString("nombre_producto"));
                d.setUnidad(rs.getString("unidad"));
                d.setCantidad(rs.getDouble("cantidad"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                d.setSubtotal(rs.getDouble("subtotal"));
                d.setNumeroSacos(rs.getInt("numero_sacos"));
                d.setPesoSaco(rs.getDouble("peso_saco"));
                d.setLibrasSueltas(rs.getDouble("libras_sueltas"));
                detalles.add(d);
            }
            rs.close();
            ps.close();
            ps = null;

            System.out.println("📦 Eliminando pedido #" + id + " - Recepción: " + estadoRecepcion);

            // ============================================================
            //  4. SI EL PEDIDO FUE RECIBIDO, RESTAR EL STOCK
            // ============================================================
            if ("RECIBIDA".equals(estadoRecepcion)) {
                System.out.println("📦 Pedido recibido - Ajustando inventario...");

                for (DetallePedido detalle : detalles) {
                    String sqlMp = "SELECT * FROM materias_primas WHERE id = ?";
                    ps = conn.prepareStatement(sqlMp);
                    ps.setInt(1, detalle.getMateriaPrimaId());
                    rs = ps.executeQuery();

                    if (rs.next()) {
                        int mpId = rs.getInt("id");
                        String nombre = rs.getString("nombre");
                        String unidad = rs.getString("unidad");
                        double cantidadDisponible = rs.getDouble("cantidad_disponible");
                        double cantidadTotal = rs.getDouble("cantidad_total");
                        double precioTotal = rs.getDouble("precio_total");
                        int numeroSacosActual = rs.getInt("numero_sacos");
                        double pesoSacoActual = rs.getDouble("peso_saco");
                        double librasSueltasActual = rs.getDouble("libras_sueltas");
                        rs.close();
                        ps.close();
                        ps = null;

                        double nuevoDisponible = cantidadDisponible - detalle.getCantidad();
                        if (nuevoDisponible < 0) nuevoDisponible = 0;

                        double nuevoTotal = cantidadTotal - detalle.getCantidad();
                        if (nuevoTotal < 0) nuevoTotal = 0;

                        double nuevoPrecioTotal = precioTotal - detalle.getSubtotal();
                        if (nuevoPrecioTotal < 0) nuevoPrecioTotal = 0;

                        int nuevoNumeroSacos = numeroSacosActual - (detalle.getNumeroSacos() != null ? detalle.getNumeroSacos() : 0);
                        if (nuevoNumeroSacos < 0) nuevoNumeroSacos = 0;

                        double nuevoPesoSaco = pesoSacoActual;
                        if (nuevoNumeroSacos == 0) {
                            nuevoPesoSaco = 0;
                        }

                        double nuevasLibrasSueltas = librasSueltasActual - (detalle.getLibrasSueltas() != null ? detalle.getLibrasSueltas() : 0);
                        if (nuevasLibrasSueltas < 0) nuevasLibrasSueltas = 0;

                        double nuevoPrecioPorUnidad = nuevoTotal > 0 ? nuevoPrecioTotal / nuevoTotal : 0;

                        // Si el producto queda con stock 0, ELIMINARLO
                        boolean productoFueEliminado = (nuevoTotal <= 0 && nuevoDisponible <= 0);

                        if (productoFueEliminado) {
                            // Eliminar movimientos
                            String sqlDelMov = "DELETE FROM movimientos_inventario WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDelMov);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                            // Eliminar ingresos historial
                            String sqlDelIng = "DELETE FROM ingresos_historial WHERE producto_id = ?";
                            ps = conn.prepareStatement(sqlDelIng);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                            // Eliminar detalles de formulación
                            String sqlDelForm = "DELETE FROM formulacion_detalle WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDelForm);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                            // Eliminar compras a proveedores (evita FOREIGN KEY constraint failed)
                            String sqlDelCompras = "DELETE FROM compras_proveedores WHERE materia_prima_id = ?";
                            ps = conn.prepareStatement(sqlDelCompras);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                            // Eliminar historial de precios (evita FOREIGN KEY constraint failed)
                            String sqlDelHist = "DELETE FROM historial_precios WHERE producto_id = ?";
                            ps = conn.prepareStatement(sqlDelHist);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                            // Eliminar el producto
                            String sqlDelMp = "DELETE FROM materias_primas WHERE id = ?";
                            ps = conn.prepareStatement(sqlDelMp);
                            ps.setInt(1, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;

                        } else {
                            // Actualizar el producto
                            String sqlUpdate = "UPDATE materias_primas SET " +
                                "cantidad_total = ?, cantidad_disponible = ?, " +
                                "precio_total = ?, precio_por_unidad = ?, " +
                                "numero_sacos = ?, peso_saco = ?, libras_sueltas = ? " +
                                "WHERE id = ?";

                            ps = conn.prepareStatement(sqlUpdate);
                            ps.setDouble(1, nuevoTotal);
                            ps.setDouble(2, nuevoDisponible);
                            ps.setDouble(3, nuevoPrecioTotal);
                            ps.setDouble(4, nuevoPrecioPorUnidad);
                            ps.setInt(5, nuevoNumeroSacos);
                            ps.setDouble(6, nuevoPesoSaco);
                            ps.setDouble(7, nuevasLibrasSueltas);
                            ps.setInt(8, mpId);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;
                        }

                        // Registrar movimiento de salida — SOLO si el producto sigue
                        // existiendo. Si se acaba de eliminar arriba (stock en 0),
                        // ya no hay a qué producto referenciar, y esto violaba la
                        // llave foránea (el producto ya no está en materias_primas).
                        if (!productoFueEliminado) {
                            String sqlMov = "INSERT INTO movimientos_inventario (" +
                                "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                                "cantidad, precio_por_unidad, subtotal, fecha, hora, referencia, pedido_id) " +
                                "VALUES (?, ?, ?, 'SALIDA', ?, ?, ?, date('now'), time('now'), ?, ?)";

                            ps = conn.prepareStatement(sqlMov);
                            ps.setInt(1, mpId);
                            ps.setString(2, nombre);
                            ps.setString(3, unidad);
                            ps.setDouble(4, detalle.getCantidad());
                            ps.setDouble(5, detalle.getPrecioUnitario());
                            ps.setDouble(6, detalle.getSubtotal());
                            ps.setString(7, "Eliminación de pedido #" + id + " - " + nombre);
                            ps.setInt(8, id);
                            ps.executeUpdate();
                            ps.close();
                            ps = null;
                        }

                    } else {
                        if (rs != null) { rs.close(); }
                        if (ps != null) { ps.close(); }
                        ps = null;
                    }
                }
            }

            // ============================================================
            //  5. ELIMINAR DETALLES Y PEDIDO
            // ============================================================
            String sqlDeleteDetalles = "DELETE FROM detalle_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlDeleteDetalles);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            ps = null;

            String sqlDeletePedido = "DELETE FROM pedidos WHERE id = ?";
            ps = conn.prepareStatement(sqlDeletePedido);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            ps = null;

            conn.commit();
            System.out.println("✅ Pedido #" + id + " eliminado correctamente");

        } catch (SQLException e) {
            if (conn != null) { 
                try { 
                    conn.rollback(); 
                    System.out.println("⚠️ Rollback ejecutado en eliminar pedido");
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                } 
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    try (Statement stmt = conn.createStatement()) {
                        if (fkEstabaActivo) {
                            stmt.execute("PRAGMA foreign_keys = ON");
                        }
                    }
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }

    public void actualizarEstadoRecepcion(int pedidoId, String estado) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            Pedido anterior = buscarPorId(pedidoId);
            String datosAnteriores = anterior != null ? 
                String.format("pedido_id:%d,estado_recepcion:%s", pedidoId, anterior.getEstadoRecepcion()) : null;
            
            String sql = "UPDATE pedidos SET estado_recepcion = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, estado);
            ps.setInt(2, pedidoId);
            ps.executeUpdate();
            
            String datosNuevos = String.format("pedido_id:%d,estado_recepcion:%s", pedidoId, estado);
            auditoriaDAO.registrarUpdate("pedidos", pedidoId, datosAnteriores, datosNuevos);
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public Pedido buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM pedidos WHERE id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                Pedido p = mapear(rs);
                p.setProveedor(proveedorDAO.buscarPorId(p.getProveedorId()));
                p.setDetalles(obtenerDetalles(id));
                p.setPagos(obtenerPagos(id));
                return p;
            }
            return null;
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<DetallePedido> obtenerDetalles(int pedidoId) throws SQLException {
        List<DetallePedido> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM detalle_pedido WHERE pedido_id = ?");
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                DetallePedido d = new DetallePedido();
                d.setId(rs.getInt("id"));
                d.setPedidoId(rs.getInt("pedido_id"));
                d.setMateriaPrimaId(rs.getInt("materia_prima_id"));
                d.setNombreProducto(rs.getString("nombre_producto"));
                d.setUnidad(rs.getString("unidad"));
                d.setCantidad(rs.getDouble("cantidad"));
                d.setPrecioUnitario(rs.getDouble("precio_unitario"));
                d.setSubtotal(rs.getDouble("subtotal"));
                d.setNumeroSacos(rs.getInt("numero_sacos"));
                d.setPesoSaco(rs.getDouble("peso_saco"));
                d.setLibrasSueltas(rs.getDouble("libras_sueltas"));
                lista.add(d);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<PagoPedido> obtenerPagos(int pedidoId) throws SQLException {
        List<PagoPedido> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM pagos_pedido WHERE pedido_id = ? ORDER BY id DESC");
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                PagoPedido p = new PagoPedido();
                p.setId(rs.getInt("id"));
                p.setPedidoId(rs.getInt("pedido_id"));
                p.setProveedorId(rs.getInt("proveedor_id"));
                p.setMonto(rs.getDouble("monto"));
                p.setFormaPago(rs.getString("forma_pago"));
                p.setFechaPago(rs.getString("fecha_pago"));
                p.setHoraPago(rs.getString("hora_pago"));
                p.setReferencia(rs.getString("referencia"));
                lista.add(p);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<Pedido> listarTodos() throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM pedidos ORDER BY id DESC");
            
            while (rs.next()) {
                Pedido p = mapear(rs);
                p.setProveedor(proveedorDAO.buscarPorId(p.getProveedorId()));
                p.setDetalles(obtenerDetalles(p.getId()));
                p.setPagos(obtenerPagos(p.getId()));
                lista.add(p);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public List<Pedido> listarPorProveedor(int proveedorId) throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM pedidos WHERE proveedor_id = ? ORDER BY id DESC");
            ps.setInt(1, proveedorId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Pedido p = mapear(rs);
                p.setProveedor(proveedorDAO.buscarPorId(p.getProveedorId()));
                p.setDetalles(obtenerDetalles(p.getId()));
                p.setPagos(obtenerPagos(p.getId()));
                lista.add(p);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<Pedido> listarPendientesRecepcion() throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM pedidos WHERE estado_recepcion IN ('PENDIENTE', 'PARCIAL') ORDER BY id DESC");
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Pedido p = mapear(rs);
                p.setProveedor(proveedorDAO.buscarPorId(p.getProveedorId()));
                p.setDetalles(obtenerDetalles(p.getId()));
                p.setPagos(obtenerPagos(p.getId()));
                lista.add(p);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<Pedido> listarPendientesPago() throws SQLException {
        List<Pedido> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM pedidos WHERE estado_pago IN ('PENDIENTE', 'ABONADO') ORDER BY id DESC");
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Pedido p = mapear(rs);
                p.setProveedor(proveedorDAO.buscarPorId(p.getProveedorId()));
                p.setDetalles(obtenerDetalles(p.getId()));
                p.setPagos(obtenerPagos(p.getId()));
                lista.add(p);
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id"));
        p.setProveedorId(rs.getInt("proveedor_id"));
        p.setFechaPedido(rs.getString("fecha_pedido"));
        p.setEstadoPago(rs.getString("estado_pago"));
        p.setEstadoRecepcion(rs.getString("estado_recepcion"));
        p.setAbonado(rs.getDouble("abonado"));
        p.setSaldoPendiente(rs.getDouble("saldo_pendiente"));
        p.setNotas(rs.getString("notas"));
        return p;
    }
        /**
     * Actualiza un pedido completo con sus nuevos detalles
     * @param pedidoId ID del pedido a actualizar
     * @param detalles Lista de nuevos detalles del pedido
     * @param yaRecibido true si el pedido ya fue recibido (afecta inventario)
     * @throws SQLException
     */
       /**
     * Actualiza un pedido completo con sus nuevos detalles
     * @param pedidoId ID del pedido a actualizar
     * @param detalles Lista de nuevos detalles del pedido
     * @param yaRecibido true si el pedido ya fue recibido (afecta inventario)
     * @throws SQLException
     */
    public void actualizarPedidoConDetalles(int pedidoId, List<DetallePedido> detalles, boolean yaRecibido) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // ============================================================
            //  1. OBTENER EL PEDIDO ORIGINAL Y SUS DETALLES
            // ============================================================
            Pedido pedidoOriginal = buscarPorId(pedidoId);
            if (pedidoOriginal == null) {
                throw new SQLException("Pedido no encontrado: " + pedidoId);
            }

            List<DetallePedido> detallesOriginales = null;
            if (yaRecibido) {
                detallesOriginales = obtenerDetalles(pedidoId);
                System.out.println("📦 Pedido #" + pedidoId + " YA FUE RECIBIDO - Actualizando inventario...");
                System.out.println("📦 Detalles originales: " + (detallesOriginales != null ? detallesOriginales.size() : 0) + " productos");
            }

            // ============================================================
            //  2. ELIMINAR TODOS LOS DETALLES ACTUALES
            // ============================================================
            String sqlDelete = "DELETE FROM detalle_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlDelete);
            ps.setInt(1, pedidoId);
            ps.executeUpdate();
            ps.close();

            // ============================================================
            //  3. INSERTAR LOS NUEVOS DETALLES
            // ============================================================
            String sqlInsert = "INSERT INTO detalle_pedido (" +
                               "pedido_id, materia_prima_id, nombre_producto, unidad, cantidad, " +
                               "precio_unitario, subtotal, numero_sacos, peso_saco, libras_sueltas) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sqlInsert);

            double nuevoTotal = 0;
            for (DetallePedido d : detalles) {
                // IMPORTANTE: antes esto llamaba a materiaDAO.buscarPorNombre() y
                // materiaDAO.guardarRapido(), que abren CADA UNO su propia conexión
                // nueva a la base de datos. Como esta transacción (conn) ya estaba
                // abierta y sin confirmar en este punto, SQLite bloqueaba esa
                // segunda conexión hasta agotar el tiempo de espera, y terminaba
                // fallando con "SQLITE_BUSY: database is locked". Ahora se usa la
                // MISMA conexión (conn) para buscar y, si hace falta, crear el
                // producto — sin abrir ninguna conexión adicional.
                MateriaPrima mp = null;
                String sqlBuscar = "SELECT * FROM materias_primas WHERE UPPER(TRIM(nombre)) = ?";
                PreparedStatement psBuscar = conn.prepareStatement(sqlBuscar);
                psBuscar.setString(1, d.getNombreProducto().trim().toUpperCase());
                ResultSet rsBuscar = psBuscar.executeQuery();
                if (rsBuscar.next()) {
                    mp = new MateriaPrima();
                    mp.setId(rsBuscar.getInt("id"));
                    mp.setNombre(rsBuscar.getString("nombre"));
                    mp.setUnidad(rsBuscar.getString("unidad"));
                }
                rsBuscar.close();
                psBuscar.close();

                if (mp == null) {
                    // Crear producto nuevo usando la MISMA conexión (conn)
                    String sqlCrear = "INSERT INTO materias_primas " +
                        "(codigo, nombre, unidad, cantidad_total, cantidad_disponible, precio_total, precio_por_unidad, " +
                        "fecha_ingreso, hora_ingreso, origen) " +
                        "VALUES (?, ?, ?, 0, 0, 0, ?, ?, ?, 'PEDIDO')";
                    PreparedStatement psCrear = conn.prepareStatement(sqlCrear);
                    psCrear.setString(1, generarCodigoProducto(conn, d.getNombreProducto()));
                    psCrear.setString(2, d.getNombreProducto());
                    psCrear.setString(3, d.getUnidad());
                    psCrear.setDouble(4, d.getPrecioUnitario());
                    psCrear.setString(5, LocalDate.now().toString());
                    psCrear.setString(6, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                    psCrear.executeUpdate();
                    psCrear.close();

                    int nuevoId;
                    try (Statement stmtId = conn.createStatement();
                         ResultSet rsId = stmtId.executeQuery("SELECT last_insert_rowid()")) {
                        rsId.next();
                        nuevoId = rsId.getInt(1);
                    }

                    mp = new MateriaPrima();
                    mp.setId(nuevoId);
                    mp.setNombre(d.getNombreProducto());
                    mp.setUnidad(d.getUnidad());
                }

                d.setMateriaPrimaId(mp.getId());
                d.setPedidoId(pedidoId);

                ps.setInt(1, pedidoId);
                ps.setInt(2, d.getMateriaPrimaId());
                ps.setString(3, d.getNombreProducto());
                ps.setString(4, d.getUnidad());
                ps.setDouble(5, d.getCantidad());
                ps.setDouble(6, d.getPrecioUnitario());
                ps.setDouble(7, d.getSubtotal());
                ps.setObject(8, d.getNumeroSacos());
                ps.setObject(9, d.getPesoSaco());
                ps.setObject(10, d.getLibrasSueltas());
                ps.executeUpdate();

                nuevoTotal += d.getSubtotal();
            }
            ps.close();

            // ============================================================
            //  4. ACTUALIZAR TOTAL DEL PEDIDO
            // ============================================================
            double nuevoSaldo = nuevoTotal - pedidoOriginal.getAbonado();
            if (nuevoSaldo < 0) nuevoSaldo = 0;
            String nuevoEstadoPago = nuevoSaldo <= 0 ? "CANCELADO" : 
                                     (pedidoOriginal.getAbonado() > 0 ? "ABONADO" : "PENDIENTE");

            String sqlUpdate = "UPDATE pedidos SET saldo_pendiente = ?, estado_pago = ? WHERE id = ?";
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, nuevoSaldo);
            ps.setString(2, nuevoEstadoPago);
            ps.setInt(3, pedidoId);
            ps.executeUpdate();
            ps.close();

            // ============================================================
            //  5. ACTUALIZAR INVENTARIO (si ya fue recibido)
            // ============================================================
            if (yaRecibido && detallesOriginales != null) {
                System.out.println("📦 ACTUALIZANDO INVENTARIO por cambios en pedido #" + pedidoId);
                actualizarStockPorCambios(pedidoId, detallesOriginales, detalles, conn);
            }

            conn.commit();
            System.out.println("✅ Pedido #" + pedidoId + " actualizado correctamente.");

        } catch (SQLException e) {
            if (conn != null) { 
                try { 
                    conn.rollback(); 
                    System.out.println("⚠️ Rollback ejecutado en actualizarPedidoConDetalles");
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                } 
            }
            throw e;
        } finally {
            if (conn != null) { 
                try { 
                    conn.setAutoCommit(true); 
                    conn.close(); 
                } catch (SQLException e) {} 
            }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }
        /**
     * Actualiza el stock del inventario cuando se edita un pedido ya recibido
     * @param pedidoId ID del pedido
     * @param originales Lista de detalles originales
     * @param nuevos Lista de nuevos detalles
     */
         /**
     * Actualiza el stock del inventario cuando se edita un pedido ya recibido
     * @param pedidoId ID del pedido
     * @param originales Lista de detalles originales
     * @param nuevos Lista de nuevos detalles
     * @param conn Conexión activa (para usar la misma transacción)
     */
    /**
     * Genera un código para un producto nuevo (ej: "MAI-001"), igual que
     * MateriaPrimaDAO.generarCodigo(), pero usando la MISMA conexión (conn)
     * en vez de abrir una nueva — para evitar el bloqueo SQLITE_BUSY que
     * ocurría al crear productos desde un pedido.
     */
    private String generarCodigoProducto(Connection conn, String nombre) throws SQLException {
        String prefijo = nombre.length() >= 3 ? nombre.substring(0, 3).toUpperCase() : nombre.toUpperCase();
        String sql = "SELECT COUNT(*) FROM materias_primas WHERE codigo LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int count = rs.next() ? rs.getInt(1) : 0;
                return prefijo + "-" + String.format("%03d", count + 1);
            }
        }
    }

    private void actualizarStockPorCambios(int pedidoId, List<DetallePedido> originales, List<DetallePedido> nuevos, Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            System.out.println("📦 ========================================");
            System.out.println("📦 ACTUALIZANDO STOCK - Pedido #" + pedidoId);
            System.out.println("📦 Originales: " + originales.size() + " productos");
            System.out.println("📦 Nuevos: " + nuevos.size() + " productos");

            // ============================================================
            //  PASO 1: Crear mapa de productos originales
            // ============================================================
            Map<Integer, DetallePedido> mapaOriginales = new HashMap<>();
            for (DetallePedido d : originales) {
                mapaOriginales.put(d.getMateriaPrimaId(), d);
                System.out.println("📦 Original: ID=" + d.getMateriaPrimaId() + 
                    " | " + d.getNombreProducto() + 
                    " | Cantidad: " + d.getCantidad() +
                    " | Sacos: " + (d.getNumeroSacos() != null ? d.getNumeroSacos() : 0) +
                    " | Peso/Saco: " + (d.getPesoSaco() != null ? d.getPesoSaco() : 0) +
                    " | Sueltos: " + (d.getLibrasSueltas() != null ? d.getLibrasSueltas() : 0));
            }

            // ============================================================
            //  PASO 2: Procesar todos los cambios (modificados, nuevos, eliminados)
            // ============================================================
            
            // 2a: Productos modificados o nuevos
            for (DetallePedido nuevo : nuevos) {
                DetallePedido original = mapaOriginales.get(nuevo.getMateriaPrimaId());
                
                if (original != null) {
                    // Producto EXISTENTE - recalcular todo
                    System.out.println("📦 MODIFICADO: " + nuevo.getNombreProducto());
                    recalcularProductoCompleto(conn, nuevo, original, pedidoId, "MODIFICADO");
                } else {
                    // Producto NUEVO - sumar todo
                    System.out.println("📦 NUEVO PRODUCTO: " + nuevo.getNombreProducto());
                    recalcularProductoCompleto(conn, nuevo, null, pedidoId, "NUEVO");
                }
            }

            // 2b: Productos ELIMINADOS
            Set<Integer> idsNuevos = new HashSet<>();
            for (DetallePedido d : nuevos) {
                idsNuevos.add(d.getMateriaPrimaId());
            }

            for (DetallePedido original : originales) {
                if (!idsNuevos.contains(original.getMateriaPrimaId())) {
                    System.out.println("📦 ELIMINADO: " + original.getNombreProducto());
                    recalcularProductoCompleto(conn, null, original, pedidoId, "ELIMINADO");
                }
            }

            System.out.println("📦 ========================================");

        } catch (SQLException ex) {
            System.err.println("⚠️ Error actualizando stock: " + ex.getMessage());
            LoggerUtil.logError("Error actualizando stock por cambios en pedido", ex);
            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("⚠️ Rollback ejecutado en actualizarStockPorCambios");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            throw ex;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Recalcula TODOS los campos de un producto en el inventario
     */
    private void recalcularProductoCompleto(Connection conn, DetallePedido nuevo, DetallePedido original, 
                                             int pedidoId, String tipoOperacion) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            int productoId;
            double cantidadCambio = 0;
            double precioCambio = 0;
            int sacosCambio = 0;
            double pesoSacoCambio = 0;
            double sueltosCambio = 0;
            String nombreProducto = "";
            String unidad = "";
            
            if ("NUEVO".equals(tipoOperacion)) {
                // Producto nuevo: sumar todo
                productoId = nuevo.getMateriaPrimaId();
                cantidadCambio = nuevo.getCantidad();
                precioCambio = nuevo.getSubtotal();
                sacosCambio = nuevo.getNumeroSacos() != null ? nuevo.getNumeroSacos() : 0;
                pesoSacoCambio = nuevo.getPesoSaco() != null ? nuevo.getPesoSaco() : 0;
                sueltosCambio = nuevo.getLibrasSueltas() != null ? nuevo.getLibrasSueltas() : 0;
                nombreProducto = nuevo.getNombreProducto();
                unidad = nuevo.getUnidad();
            } else if ("ELIMINADO".equals(tipoOperacion)) {
                // Producto eliminado: restar todo
                productoId = original.getMateriaPrimaId();
                cantidadCambio = -original.getCantidad();
                precioCambio = -original.getSubtotal();
                sacosCambio = -(original.getNumeroSacos() != null ? original.getNumeroSacos() : 0);
                pesoSacoCambio = -(original.getPesoSaco() != null ? original.getPesoSaco() : 0);
                sueltosCambio = -(original.getLibrasSueltas() != null ? original.getLibrasSueltas() : 0);
                nombreProducto = original.getNombreProducto();
                unidad = original.getUnidad();
            } else {
                // MODIFICADO: calcular diferencia
                productoId = nuevo.getMateriaPrimaId();
                cantidadCambio = nuevo.getCantidad() - original.getCantidad();
                precioCambio = nuevo.getSubtotal() - original.getSubtotal();
                sacosCambio = (nuevo.getNumeroSacos() != null ? nuevo.getNumeroSacos() : 0) - 
                               (original.getNumeroSacos() != null ? original.getNumeroSacos() : 0);
                pesoSacoCambio = (nuevo.getPesoSaco() != null ? nuevo.getPesoSaco() : 0) - 
                                  (original.getPesoSaco() != null ? original.getPesoSaco() : 0);
                sueltosCambio = (nuevo.getLibrasSueltas() != null ? nuevo.getLibrasSueltas() : 0) - 
                                 (original.getLibrasSueltas() != null ? original.getLibrasSueltas() : 0);
                nombreProducto = nuevo.getNombreProducto();
                unidad = nuevo.getUnidad();
            }
            
            System.out.println("📦 " + tipoOperacion + " - Producto: " + nombreProducto);
            System.out.println("   Cantidad: " + (cantidadCambio >= 0 ? "+" : "") + cantidadCambio);
            System.out.println("   Precio: " + (precioCambio >= 0 ? "+" : "") + precioCambio);
            System.out.println("   Sacos: " + (sacosCambio >= 0 ? "+" : "") + sacosCambio);
            System.out.println("   Peso/Saco: " + (pesoSacoCambio >= 0 ? "+" : "") + pesoSacoCambio);
            System.out.println("   Sueltos: " + (sueltosCambio >= 0 ? "+" : "") + sueltosCambio);
            
            // ============================================================
            //  OBTENER EL PRODUCTO ACTUAL
            // ============================================================
            String sqlSelect = "SELECT * FROM materias_primas WHERE id = ?";
            ps = conn.prepareStatement(sqlSelect);
            ps.setInt(1, productoId);
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                System.err.println("⚠️ Producto no encontrado: ID " + productoId);
                return;
            }
            
            double cantidadTotalActual = rs.getDouble("cantidad_total");
            double cantidadDisponibleActual = rs.getDouble("cantidad_disponible");
            double precioTotalActual = rs.getDouble("precio_total");
            int numeroSacosActual = rs.getInt("numero_sacos");
            double pesoSacoActual = rs.getDouble("peso_saco");
            double librasSueltasActual = rs.getDouble("libras_sueltas");
            rs.close();
            ps.close();
            ps = null;
            
            // ============================================================
            //  CALCULAR NUEVOS VALORES
            // ============================================================
            double nuevaCantidadTotal = cantidadTotalActual + cantidadCambio;
            double nuevaCantidadDisponible = cantidadDisponibleActual + cantidadCambio;
            double nuevoPrecioTotal = precioTotalActual + precioCambio;
            int nuevosSacos = numeroSacosActual + sacosCambio;
            double nuevoPesoSaco = pesoSacoActual;
            double nuevasLibrasSueltas = librasSueltasActual + sueltosCambio;
            
            // Evitar valores negativos
            if (nuevaCantidadTotal < 0) nuevaCantidadTotal = 0;
            if (nuevaCantidadDisponible < 0) nuevaCantidadDisponible = 0;
            if (nuevoPrecioTotal < 0) nuevoPrecioTotal = 0;
            if (nuevosSacos < 0) nuevosSacos = 0;
            if (nuevasLibrasSueltas < 0) nuevasLibrasSueltas = 0;
            
            // Recalcular peso del saco si es necesario
            if (nuevosSacos > 0 && nuevaCantidadTotal > 0) {
                // Si hay sacos, el peso del saco se recalcula como promedio
                nuevoPesoSaco = (nuevaCantidadTotal - nuevasLibrasSueltas) / nuevosSacos;
                if (nuevoPesoSaco < 0) nuevoPesoSaco = 0;
            } else if (nuevosSacos == 0) {
                nuevoPesoSaco = 0;
                nuevasLibrasSueltas = nuevaCantidadTotal;
            }
            
            // Recalcular precio por unidad
            double nuevoPrecioPorUnidad = nuevaCantidadTotal > 0 ? nuevoPrecioTotal / nuevaCantidadTotal : 0;
            
            System.out.println("   📊 NUEVOS VALORES:");
            System.out.println("      Total: " + nuevaCantidadTotal);
            System.out.println("      Disponible: " + nuevaCantidadDisponible);
            System.out.println("      Precio Total: " + nuevoPrecioTotal);
            System.out.println("      Sacos: " + nuevosSacos);
            System.out.println("      Peso/Saco: " + nuevoPesoSaco);
            System.out.println("      Sueltos: " + nuevasLibrasSueltas);
            System.out.println("      $/Unidad: " + nuevoPrecioPorUnidad);
            
            // ============================================================
            //  ACTUALIZAR PRODUCTO CON TODOS LOS CAMPOS
            // ============================================================
            String sqlUpdate = "UPDATE materias_primas SET " +
                               "cantidad_total = ?, " +
                               "cantidad_disponible = ?, " +
                               "precio_total = ?, " +
                               "precio_por_unidad = ?, " +
                               "numero_sacos = ?, " +
                               "peso_saco = ?, " +
                               "libras_sueltas = ? " +
                               "WHERE id = ?";
            
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, nuevaCantidadTotal);
            ps.setDouble(2, nuevaCantidadDisponible);
            ps.setDouble(3, nuevoPrecioTotal);
            ps.setDouble(4, nuevoPrecioPorUnidad);
            ps.setInt(5, nuevosSacos);
            ps.setDouble(6, nuevoPesoSaco);
            ps.setDouble(7, nuevasLibrasSueltas);
            ps.setInt(8, productoId);
            
            int actualizados = ps.executeUpdate();
            ps.close();
            ps = null;
            
            if (actualizados > 0) {
                System.out.println("   ✅ Producto ACTUALIZADO correctamente");
                
                // Si el producto quedó sin stock de NINGÚN origen (ni pedidos ni
                // inventario — cantidad_total ya es la suma de todos los orígenes),
                // se elimina el registro completo en vez de dejarlo como un
                // "fantasma" con 0 en todo pero visible en la tabla para siempre.
                if (nuevaCantidadTotal <= 0.0001 && nuevaCantidadDisponible <= 0.0001) {
                    System.out.println("   🗑️ El producto quedó sin stock de ningún origen - eliminando registro completo");

                    // Se desactivan las llaves foráneas TEMPORALMENTE durante esta
                    // limpieza (igual que ya hace exitosamente PedidoDAO.eliminar()),
                    // para no depender del orden exacto de los DELETE ni de que el
                    // CASCADE se aplique perfectamente en cada tabla relacionada.
                    boolean fkEstabaActivo = false;
                    try (Statement stmtFk = conn.createStatement()) {
                        try (ResultSet rsFk = stmtFk.executeQuery("PRAGMA foreign_keys")) {
                            if (rsFk.next()) fkEstabaActivo = rsFk.getInt(1) == 1;
                        }
                        stmtFk.execute("PRAGMA foreign_keys = OFF");
                    }

                    try {
                        String sqlDelIngresos = "DELETE FROM ingresos_historial WHERE producto_id = ?";
                        try (PreparedStatement psDelIng = conn.prepareStatement(sqlDelIngresos)) {
                            psDelIng.setInt(1, productoId);
                            psDelIng.executeUpdate();
                        }

                        String sqlDelMov = "DELETE FROM movimientos_inventario WHERE materia_prima_id = ?";
                        try (PreparedStatement psDelMov = conn.prepareStatement(sqlDelMov)) {
                            psDelMov.setInt(1, productoId);
                            psDelMov.executeUpdate();
                        }

                        String sqlDelForm = "DELETE FROM formulacion_detalle WHERE materia_prima_id = ?";
                        try (PreparedStatement psDelForm = conn.prepareStatement(sqlDelForm)) {
                            psDelForm.setInt(1, productoId);
                            psDelForm.executeUpdate();
                        }

                        String sqlDelCompras = "DELETE FROM compras_proveedores WHERE materia_prima_id = ?";
                        try (PreparedStatement psDelCompras = conn.prepareStatement(sqlDelCompras)) {
                            psDelCompras.setInt(1, productoId);
                            psDelCompras.executeUpdate();
                        }

                        String sqlDelHist = "DELETE FROM historial_precios WHERE producto_id = ?";
                        try (PreparedStatement psDelHist = conn.prepareStatement(sqlDelHist)) {
                            psDelHist.setInt(1, productoId);
                            psDelHist.executeUpdate();
                        }

                        // Por si quedó referenciado por otro pedido distinto al que se
                        // está editando ahora mismo (defensivo, no debería pasar si el
                        // stock ya está en 0, pero evita el FOREIGN KEY constraint failed
                        // en cualquier caso residual).
                        String sqlDelDetallePedido = "DELETE FROM detalle_pedido WHERE materia_prima_id = ?";
                        try (PreparedStatement psDelDp = conn.prepareStatement(sqlDelDetallePedido)) {
                            psDelDp.setInt(1, productoId);
                            psDelDp.executeUpdate();
                        }

                        String sqlDelMp = "DELETE FROM materias_primas WHERE id = ?";
                        try (PreparedStatement psDelMp = conn.prepareStatement(sqlDelMp)) {
                            psDelMp.setInt(1, productoId);
                            psDelMp.executeUpdate();
                        }
                    } finally {
                        try (Statement stmtFkRestore = conn.createStatement()) {
                            stmtFkRestore.execute("PRAGMA foreign_keys = " + (fkEstabaActivo ? "ON" : "OFF"));
                        }
                    }
                }
            }
            
            // ============================================================
            //  REGISTRAR MOVIMIENTO DE INVENTARIO
            // ============================================================
            if (cantidadCambio != 0) {
                String tipoMovimiento = cantidadCambio > 0 ? "INGRESO" : "SALIDA";
                String referencia = tipoOperacion + " en pedido #" + pedidoId + " - " + nombreProducto;
                
                String sqlMov = "INSERT INTO movimientos_inventario (" +
                    "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                    "cantidad, precio_por_unidad, subtotal, fecha, hora, referencia, pedido_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, date('now'), time('now'), ?, ?)";
                
                ps = conn.prepareStatement(sqlMov);
                ps.setInt(1, productoId);
                ps.setString(2, nombreProducto);
                ps.setString(3, unidad);
                ps.setString(4, tipoMovimiento);
                ps.setDouble(5, Math.abs(cantidadCambio));
                double precioUnitario = nuevaCantidadTotal > 0 ? nuevoPrecioTotal / nuevaCantidadTotal : 0;
                ps.setDouble(6, precioUnitario);
                ps.setDouble(7, Math.abs(cantidadCambio) * precioUnitario);
                ps.setString(8, referencia);
                ps.setInt(9, pedidoId);
                ps.executeUpdate();
                ps.close();
                ps = null;
                
                System.out.println("   📝 Movimiento registrado: " + tipoMovimiento + " " + Math.abs(cantidadCambio) + " " + unidad);
            }
            
        } catch (SQLException e) {
            throw e;
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
        }
    }
        /**
     * Elimina un pago de pedido y actualiza el estado del pedido
     */
    public void eliminarPagoPedido(int pagoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. OBTENER EL PAGO
            String sqlPago = "SELECT * FROM pagos_pedido WHERE id = ?";
            ps = conn.prepareStatement(sqlPago);
            ps.setInt(1, pagoId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Pago no encontrado");
            }

            int pedidoId = rs.getInt("pedido_id");
            rs.close();
            ps.close();

            // 2. ELIMINAR EL PAGO
            String sqlDelete = "DELETE FROM pagos_pedido WHERE id = ?";
            ps = conn.prepareStatement(sqlDelete);
            ps.setInt(1, pagoId);
            ps.executeUpdate();
            ps.close();

            // 3. RECALCULAR ABONADO
            String sqlSum = "SELECT COALESCE(SUM(monto), 0) as total_abonado FROM pagos_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlSum);
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();

            double nuevoAbonado = rs.next() ? rs.getDouble("total_abonado") : 0;
            rs.close();
            ps.close();

            // 4. OBTENER TOTAL DEL PEDIDO
            String sqlPedido = "SELECT * FROM pedidos WHERE id = ?";
            ps = conn.prepareStatement(sqlPedido);
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Pedido no encontrado");
            }

            double totalPedido = 0;
            // Calcular total desde detalles
            String sqlTotal = "SELECT COALESCE(SUM(subtotal), 0) as total FROM detalle_pedido WHERE pedido_id = ?";
            ps = conn.prepareStatement(sqlTotal);
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();
            totalPedido = rs.next() ? rs.getDouble("total") : 0;
            rs.close();
            ps.close();

            double nuevoSaldo = totalPedido - nuevoAbonado;
            if (nuevoSaldo < 0) nuevoSaldo = 0;

            // Determinar el nuevo estado
            String nuevoEstadoPago;
            if (nuevoSaldo <= 0.01) {
                nuevoEstadoPago = "CANCELADO";
            } else if (nuevoAbonado > 0) {
                nuevoEstadoPago = "ABONADO";
            } else {
                nuevoEstadoPago = "PENDIENTE";
            }

            // 5. ACTUALIZAR EL PEDIDO
            String sqlUpdate = "UPDATE pedidos SET abonado = ?, saldo_pendiente = ?, estado_pago = ? WHERE id = ?";
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, nuevoAbonado);
            ps.setDouble(2, nuevoSaldo);
            ps.setString(3, nuevoEstadoPago);
            ps.setInt(4, pedidoId);
            ps.executeUpdate();

            conn.commit();

            System.out.println("✅ Pago eliminado - Pedido #" + pedidoId + " nuevo estado: " + nuevoEstadoPago);

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {} }
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
        }
    }
   
}