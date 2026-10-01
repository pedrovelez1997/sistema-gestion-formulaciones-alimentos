package com.feedformulator.dao;

import com.feedformulator.model.MovimientoInventario;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

 public void registrarMovimiento(MovimientoInventario movimiento) throws SQLException {
    Connection conn = null;
    PreparedStatement ps = null;
    
    try {
        conn = DatabaseManager.getConnection();
        
        movimiento.setFecha(LocalDate.now().toString());
        movimiento.setHora(LocalTime.now().format(TIME_FMT));
        
        String sql = "INSERT INTO movimientos_inventario (" +
                     "materia_prima_id, nombre_producto, unidad, tipo_movimiento, " +
                     "cantidad, precio_por_unidad, subtotal, sacos_afectados, " +
                     "libras_sueltas_afectadas, fecha, hora, referencia, compra_id, pedido_id) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        ps = conn.prepareStatement(sql);
        ps.setInt(1, movimiento.getMateriaPrimaId());
        ps.setString(2, movimiento.getNombreProducto());
        ps.setString(3, movimiento.getUnidad());
        ps.setString(4, movimiento.getTipoMovimiento());
        ps.setDouble(5, movimiento.getCantidad());
        ps.setDouble(6, movimiento.getPrecioPorUnidad());
        ps.setDouble(7, movimiento.getSubtotal());
        ps.setObject(8, movimiento.getSacosAfectados());
        ps.setObject(9, movimiento.getLibrasSueltasAfectadas());
        ps.setString(10, movimiento.getFecha());
        ps.setString(11, movimiento.getHora());
        ps.setString(12, movimiento.getReferencia());
        ps.setObject(13, movimiento.getCompraId());
        ps.setObject(14, movimiento.getPedidoId());
        ps.executeUpdate();
        
    } finally {
        // Cerrar recursos correctamente
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (conn != null) try { conn.close(); } catch (SQLException e) {}
    }
}

    public List<MovimientoInventario> listarPorProducto(int materiaPrimaId) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movimientos_inventario WHERE materia_prima_id = ? ORDER BY id DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, materiaPrimaId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<MovimientoInventario> listarTodos() throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM movimientos_inventario ORDER BY id DESC");
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public List<MovimientoInventario> listarPorTipo(String tipo) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movimientos_inventario WHERE tipo_movimiento = ? ORDER BY id DESC";
            ps = conn.prepareStatement(sql);
            ps.setString(1, tipo);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<MovimientoInventario> listarPorCompra(int compraId) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movimientos_inventario WHERE compra_id = ? ORDER BY id DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, compraId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<MovimientoInventario> listarPorPedido(int pedidoId) throws SQLException {
        List<MovimientoInventario> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT * FROM movimientos_inventario WHERE pedido_id = ? ORDER BY id DESC";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, pedidoId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        MovimientoInventario m = new MovimientoInventario();
        m.setId(rs.getInt("id"));
        m.setMateriaPrimaId(rs.getInt("materia_prima_id"));
        m.setNombreProducto(rs.getString("nombre_producto"));
        m.setUnidad(rs.getString("unidad"));
        m.setTipoMovimiento(rs.getString("tipo_movimiento"));
        m.setCantidad(rs.getDouble("cantidad"));
        m.setPrecioPorUnidad(rs.getDouble("precio_por_unidad"));
        m.setSubtotal(rs.getDouble("subtotal"));
        m.setSacosAfectados(rs.getInt("sacos_afectados"));
        m.setLibrasSueltasAfectadas(rs.getDouble("libras_sueltas_afectadas"));
        m.setFecha(rs.getString("fecha"));
        m.setHora(rs.getString("hora"));
        m.setReferencia(rs.getString("referencia"));
        
        try {
            m.setCompraId(rs.getInt("compra_id"));
        } catch (SQLException e) {
            m.setCompraId(null);
        }
        
        try {
            m.setPedidoId(rs.getInt("pedido_id"));
        } catch (SQLException e) {
            m.setPedidoId(null);
        }
        
        return m;
    }
}