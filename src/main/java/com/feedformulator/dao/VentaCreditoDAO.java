package com.feedformulator.dao;

import com.feedformulator.model.Abono;
import com.feedformulator.model.Cliente;
import com.feedformulator.model.VentaCredito;
import com.feedformulator.util.MathUtils;
import com.feedformulator.util.LoggerUtil;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VentaCreditoDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public void guardar(VentaCredito ventaCredito) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            ventaCredito.setFechaVenta(LocalDate.now().format(DATE_FMT));
            
            String sql = "INSERT INTO ventas_credito (" +
                         "venta_id, cliente_id, formulacion_id, nombre_producto, total_libras, " +
                         "precio_total, abonado, saldo_pendiente, estado, fecha_venta, " +
                         "fecha_limite_pago, notas, ganancia_diferida, ganancia_realizada) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, ventaCredito.getVentaId());
            ps.setInt(2, ventaCredito.getClienteId());
            ps.setInt(3, ventaCredito.getFormulacionId());
            ps.setString(4, ventaCredito.getNombreProducto());
            ps.setDouble(5, ventaCredito.getTotalLibras());
            ps.setDouble(6, ventaCredito.getPrecioTotal());
            ps.setDouble(7, ventaCredito.getAbonado());
            ps.setDouble(8, ventaCredito.getSaldoPendiente());
            ps.setString(9, ventaCredito.getEstado() != null ? ventaCredito.getEstado() : "PENDIENTE");
            ps.setString(10, ventaCredito.getFechaVenta());
            ps.setString(11, ventaCredito.getFechaLimitePago());
            ps.setString(12, ventaCredito.getNotas());
            ps.setDouble(13, ventaCredito.getGananciaDiferida());
            ps.setDouble(14, ventaCredito.getGananciaRealizada());
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    ventaCredito.setId(rs.getInt(1));
                }
            }
            
            // Registrar auditoría
            String datos = String.format("venta_id:%d,cliente:%d,producto:%s,total:%.2f,ganancia_diferida:%.2f",
                ventaCredito.getVentaId(), ventaCredito.getClienteId(),
                ventaCredito.getNombreProducto(), ventaCredito.getPrecioTotal(),
                ventaCredito.getGananciaDiferida());
            auditoriaDAO.registrarInsert("ventas_credito", ventaCredito.getId(), datos);
            
            LoggerUtil.logInfo("Crédito guardado: " + ventaCredito.getId() + " - " + ventaCredito.getNombreProducto());
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(VentaCredito ventaCredito) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            VentaCredito anterior = buscarPorId(ventaCredito.getId());
            String datosAnteriores = anterior != null ? 
                String.format("id:%d,precio:%.2f,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                    anterior.getId(), anterior.getPrecioTotal(), anterior.getAbonado(),
                    anterior.getSaldoPendiente(), anterior.getEstado(),
                    anterior.getGananciaRealizada()) : null;
            
            String sql = "UPDATE ventas_credito SET " +
                         "precio_total = ?, abonado = ?, saldo_pendiente = ?, estado = ?, " +
                         "notas = ?, ganancia_realizada = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, ventaCredito.getPrecioTotal());
            ps.setDouble(2, ventaCredito.getAbonado());
            ps.setDouble(3, ventaCredito.getSaldoPendiente());
            ps.setString(4, ventaCredito.getEstado());
            ps.setString(5, ventaCredito.getNotas());
            ps.setDouble(6, ventaCredito.getGananciaRealizada());
            ps.setInt(7, ventaCredito.getId());
            ps.executeUpdate();
            
            String datosNuevos = String.format("id:%d,precio:%.2f,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                ventaCredito.getId(), ventaCredito.getPrecioTotal(), ventaCredito.getAbonado(),
                ventaCredito.getSaldoPendiente(), ventaCredito.getEstado(),
                ventaCredito.getGananciaRealizada());
            auditoriaDAO.registrarUpdate("ventas_credito", ventaCredito.getId(), datosAnteriores, datosNuevos);
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            VentaCredito vc = buscarPorId(id);
            String datos = vc != null ? 
                String.format("id:%d,cliente:%d,producto:%s,total:%.2f,estado:%s",
                    vc.getId(), vc.getClienteId(), vc.getNombreProducto(),
                    vc.getPrecioTotal(), vc.getEstado()) : null;
            
            String sqlAbonos = "DELETE FROM abonos WHERE venta_credito_id = ?";
            ps = conn.prepareStatement(sqlAbonos);
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
            
            String sql = "DELETE FROM ventas_credito WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            
            auditoriaDAO.registrarDelete("ventas_credito", id, datos);
            LoggerUtil.logInfo("Crédito eliminado: " + id);
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    // ============================================================
    //  ABONOS
    // ============================================================

    /**
     * Registra la fila de historial del abono INICIAL (el que se define al crear
     * la venta a crédito). A diferencia de registrarAbono(), este método NO suma
     * el monto a "abonado" en ventas_credito, porque ese valor ya se guardó
     * directamente al insertar la venta (vc.setAbonado(abonoInicial)). Sumarlo
     * de nuevo aquí duplicaba el abono inicial (5 -> aparecía como 10).
     */
    public void registrarAbonoInicial(int ventaCreditoId, int clienteId, double monto) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseManager.getConnection();

            String sqlAbono = "INSERT INTO abonos (venta_credito_id, cliente_id, monto, forma_pago, fecha_abono, hora_abono) " +
                              "VALUES (?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
            ps = conn.prepareStatement(sqlAbono);
            ps.setInt(1, ventaCreditoId);
            ps.setInt(2, clienteId);
            ps.setDouble(3, monto);
            ps.setString(4, "EFECTIVO");
            ps.executeUpdate();

            int abonoId = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    abonoId = rs.getInt(1);
                }
            }

            String datosAbono = String.format("abono_id:%d,credito_id:%d,monto:%.2f,cliente:%d,tipo:INICIAL",
                abonoId, ventaCreditoId, monto, clienteId);
            auditoriaDAO.registrarInsert("abonos", abonoId, datosAbono);

            LoggerUtil.logInfo("Abono inicial registrado: crédito " + ventaCreditoId + ", monto $" + monto);

        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public void registrarAbono(int ventaCreditoId, double monto) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            VentaCredito vc = buscarPorId(ventaCreditoId);
            if (vc == null) {
                throw new SQLException("Crédito no encontrado");
            }
            
            double nuevoAbonado = vc.getAbonado() + monto;
            double nuevoSaldo = vc.getPrecioTotal() - nuevoAbonado;
            String nuevoEstado = nuevoSaldo <= 0 ? "CANCELADO" : "ABONADO";
            if (nuevoSaldo < 0) nuevoSaldo = 0;
            
            // ============================================================
            //  CALCULAR GANANCIA REALIZADA PROPORCIONAL
            // ============================================================
            double gananciaRealizada = 0;
            if (vc.getPrecioTotal() > 0 && vc.getGananciaDiferida() > 0) {
                double porcentajeCobrado = nuevoAbonado / vc.getPrecioTotal();
                if (porcentajeCobrado > 1) porcentajeCobrado = 1;
                gananciaRealizada = vc.getGananciaDiferida() * porcentajeCobrado;
                gananciaRealizada = MathUtils.redondear(gananciaRealizada);
            }
            
            String datosAnteriores = String.format("credito_id:%d,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                ventaCreditoId, vc.getAbonado(), vc.getSaldoPendiente(),
                vc.getEstado(), vc.getGananciaRealizada());
            
            String sql = "UPDATE ventas_credito SET abonado = ?, saldo_pendiente = ?, estado = ?, ganancia_realizada = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, nuevoAbonado);
            ps.setDouble(2, nuevoSaldo);
            ps.setString(3, nuevoEstado);
            ps.setDouble(4, gananciaRealizada);
            ps.setInt(5, ventaCreditoId);
            ps.executeUpdate();
            ps.close();
            
            String sqlAbono = "INSERT INTO abonos (venta_credito_id, cliente_id, monto, forma_pago, fecha_abono, hora_abono) " +
                              "VALUES (?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
            ps = conn.prepareStatement(sqlAbono);
            ps.setInt(1, ventaCreditoId);
            ps.setInt(2, vc.getClienteId());
            ps.setDouble(3, monto);
            ps.setString(4, "EFECTIVO");
            ps.executeUpdate();
            
            int abonoId = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    abonoId = rs.getInt(1);
                }
            }
            
            String datosNuevos = String.format("credito_id:%d,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                ventaCreditoId, nuevoAbonado, nuevoSaldo, nuevoEstado, gananciaRealizada);
            auditoriaDAO.registrarUpdate("ventas_credito", ventaCreditoId, datosAnteriores, datosNuevos);
            
            String datosAbono = String.format("abono_id:%d,credito_id:%d,monto:%.2f,cliente:%d",
                abonoId, ventaCreditoId, monto, vc.getClienteId());
            auditoriaDAO.registrarInsert("abonos", abonoId, datosAbono);
            
            LoggerUtil.logInfo("Abono registrado: crédito " + ventaCreditoId + ", monto $" + monto + 
                ", ganancia realizada $" + gananciaRealizada);
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public void eliminarAbono(int abonoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            String sqlGet = "SELECT * FROM abonos WHERE id = ?";
            ps = conn.prepareStatement(sqlGet);
            ps.setInt(1, abonoId);
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                throw new SQLException("Abono no encontrado");
            }
            
            int ventaCreditoId = rs.getInt("venta_credito_id");
            double montoAbono = rs.getDouble("monto");
            int clienteId = rs.getInt("cliente_id");
            String fechaAbono = rs.getString("fecha_abono");
            String horaAbono = rs.getString("hora_abono");
            String referencia = rs.getString("referencia");
            rs.close();
            ps.close();
            
            String datosAbono = String.format("abono_id:%d,credito_id:%d,monto:%.2f,cliente:%d,fecha:%s,hora:%s,ref:%s",
                abonoId, ventaCreditoId, montoAbono, clienteId, fechaAbono, horaAbono, 
                referencia != null ? referencia : "N/A");
            
            VentaCredito vc = buscarPorId(ventaCreditoId);
            if (vc == null) {
                throw new SQLException("Crédito no encontrado");
            }
            
            String datosAnteriores = String.format("credito_id:%d,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                ventaCreditoId, vc.getAbonado(), vc.getSaldoPendiente(),
                vc.getEstado(), vc.getGananciaRealizada());
            
            double nuevoAbonado = vc.getAbonado() - montoAbono;
            double nuevoSaldo = vc.getPrecioTotal() - nuevoAbonado;
            String nuevoEstado = nuevoSaldo <= 0 ? "CANCELADO" : (nuevoAbonado > 0 ? "ABONADO" : "PENDIENTE");
            if (nuevoSaldo < 0) nuevoSaldo = 0;
            if (nuevoAbonado < 0) nuevoAbonado = 0;
            
            double gananciaRealizada = 0;
            if (vc.getPrecioTotal() > 0 && vc.getGananciaDiferida() > 0) {
                double porcentajeCobrado = nuevoAbonado / vc.getPrecioTotal();
                if (porcentajeCobrado > 1) porcentajeCobrado = 1;
                gananciaRealizada = vc.getGananciaDiferida() * porcentajeCobrado;
                gananciaRealizada = MathUtils.redondear(gananciaRealizada);
            }
            
            String sqlUpdate = "UPDATE ventas_credito SET abonado = ?, saldo_pendiente = ?, estado = ?, ganancia_realizada = ? WHERE id = ?";
            ps = conn.prepareStatement(sqlUpdate);
            ps.setDouble(1, nuevoAbonado);
            ps.setDouble(2, nuevoSaldo);
            ps.setString(3, nuevoEstado);
            ps.setDouble(4, gananciaRealizada);
            ps.setInt(5, ventaCreditoId);
            ps.executeUpdate();
            ps.close();
            
            String sqlDelete = "DELETE FROM abonos WHERE id = ?";
            ps = conn.prepareStatement(sqlDelete);
            ps.setInt(1, abonoId);
            ps.executeUpdate();
            
            String datosNuevos = String.format("credito_id:%d,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
                ventaCreditoId, nuevoAbonado, nuevoSaldo, nuevoEstado, gananciaRealizada);
            
            auditoriaDAO.registrarUpdate("ventas_credito", ventaCreditoId, datosAnteriores, datosNuevos);
            auditoriaDAO.registrarDelete("abonos", abonoId, datosAbono);
            
            LoggerUtil.logInfo("Abono eliminado: " + abonoId + ", crédito " + ventaCreditoId + 
                ", monto $" + montoAbono + ", ganancia realizada recalculada $" + gananciaRealizada);
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Abono> listarAbonosPorCredito(int ventaCreditoId) throws SQLException {
        List<Abono> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM abonos WHERE venta_credito_id = ? ORDER BY id DESC");
            ps.setInt(1, ventaCreditoId);
            rs = ps.executeQuery();
            while (rs.next()) {
                Abono a = new Abono();
                a.setId(rs.getInt("id"));
                a.setVentaCreditoId(rs.getInt("venta_credito_id"));
                a.setClienteId(rs.getInt("cliente_id"));
                a.setMonto(rs.getDouble("monto"));
                a.setFormaPago(rs.getString("forma_pago"));
                a.setFechaAbono(rs.getString("fecha_abono"));
                a.setHoraAbono(rs.getString("hora_abono"));
                a.setReferencia(rs.getString("referencia"));
                lista.add(a);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    // ============================================================
    //  CRÉDITOS - CONSULTAS
    // ============================================================

    public VentaCredito buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas_credito WHERE id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                VentaCredito vc = mapear(rs);
                vc.setCliente(clienteDAO.buscarPorId(vc.getClienteId()));
                return vc;
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public VentaCredito buscarPorVentaId(int ventaId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas_credito WHERE venta_id = ?");
            ps.setInt(1, ventaId);
            rs = ps.executeQuery();
            if (rs.next()) {
                VentaCredito vc = mapear(rs);
                vc.setCliente(clienteDAO.buscarPorId(vc.getClienteId()));
                return vc;
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<VentaCredito> listarPorCliente(int clienteId) throws SQLException {
        List<VentaCredito> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas_credito WHERE cliente_id = ? ORDER BY id DESC");
            ps.setInt(1, clienteId);
            rs = ps.executeQuery();
            while (rs.next()) {
                VentaCredito vc = mapear(rs);
                vc.setCliente(clienteDAO.buscarPorId(vc.getClienteId()));
                lista.add(vc);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<VentaCredito> listarPendientes() throws SQLException {
        List<VentaCredito> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas_credito WHERE estado IN ('PENDIENTE', 'ABONADO') ORDER BY id DESC");
            rs = ps.executeQuery();
            while (rs.next()) {
                VentaCredito vc = mapear(rs);
                vc.setCliente(clienteDAO.buscarPorId(vc.getClienteId()));
                lista.add(vc);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<VentaCredito> listarTodos() throws SQLException {
        List<VentaCredito> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM ventas_credito ORDER BY id DESC");
            while (rs.next()) {
                VentaCredito vc = mapear(rs);
                vc.setCliente(clienteDAO.buscarPorId(vc.getClienteId()));
                lista.add(vc);
            }
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public double getTotalPendiente() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT SUM(saldo_pendiente) as total FROM ventas_credito WHERE estado IN ('PENDIENTE', 'ABONADO')");
            rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public double getTotalAbonado() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT SUM(abonado) as total FROM ventas_credito");
            rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("total") : 0;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private VentaCredito mapear(ResultSet rs) throws SQLException {
        VentaCredito vc = new VentaCredito();
        vc.setId(rs.getInt("id"));
        vc.setVentaId(rs.getInt("venta_id"));
        vc.setClienteId(rs.getInt("cliente_id"));
        vc.setFormulacionId(rs.getInt("formulacion_id"));
        vc.setNombreProducto(rs.getString("nombre_producto"));
        vc.setTotalLibras(rs.getDouble("total_libras"));
        vc.setPrecioTotal(rs.getDouble("precio_total"));
        vc.setAbonado(rs.getDouble("abonado"));
        vc.setSaldoPendiente(rs.getDouble("saldo_pendiente"));
        vc.setEstado(rs.getString("estado"));
        vc.setFechaVenta(rs.getString("fecha_venta"));
        vc.setFechaLimitePago(rs.getString("fecha_limite_pago"));
        vc.setNotas(rs.getString("notas"));
        
        try {
            vc.setGananciaDiferida(rs.getDouble("ganancia_diferida"));
        } catch (SQLException e) {
            vc.setGananciaDiferida(0);
        }
        try {
            vc.setGananciaRealizada(rs.getDouble("ganancia_realizada"));
        } catch (SQLException e) {
            vc.setGananciaRealizada(0);
        }
        
        return vc;
    }
}