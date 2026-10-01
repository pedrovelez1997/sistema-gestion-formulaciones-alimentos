package com.feedformulator.dao;

import com.feedformulator.model.Venta;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void guardar(Venta v) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            v.setFecha(LocalDate.now().toString());
            v.setHora(LocalTime.now().format(TIME_FMT));
            
            String sql = "INSERT INTO ventas (formulacion_id, nombre_producto, libras_vendidas, costo_inversion, precio_venta, ganancia, fecha, hora, forma_pago, cliente_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, v.getFormulacionId());
            ps.setString(2, v.getNombreProducto());
            ps.setDouble(3, v.getLibrasVendidas());
            ps.setDouble(4, v.getCostoInversion());
            ps.setDouble(5, v.getPrecioVenta());
            ps.setDouble(6, v.getGanancia());
            ps.setString(7, v.getFecha());
            ps.setString(8, v.getHora());
            ps.setString(9, v.getFormaPago() != null ? v.getFormaPago() : "Efectivo");
            ps.setInt(10, v.getClienteId());
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    v.setId(rs.getInt(1));
                }
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Venta> listarTodas() throws SQLException {
        List<Venta> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM ventas ORDER BY id DESC");
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public Venta buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas WHERE id=?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(Venta v) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE ventas SET precio_venta = ?, ganancia = ? WHERE id = ?";
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, v.getPrecioVenta());
            ps.setDouble(2, v.getGanancia());
            ps.setInt(3, v.getId());
            ps.executeUpdate();
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("DELETE FROM ventas WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public Venta buscarPorFormulacionId(int formulacionId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM ventas WHERE formulacion_id=?");
            ps.setInt(1, formulacionId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setFormulacionId(rs.getInt("formulacion_id"));
        v.setNombreProducto(rs.getString("nombre_producto"));
        v.setLibrasVendidas(rs.getDouble("libras_vendidas"));
        v.setCostoInversion(rs.getDouble("costo_inversion"));
        v.setPrecioVenta(rs.getDouble("precio_venta"));
        v.setGanancia(rs.getDouble("ganancia"));
        v.setFecha(rs.getString("fecha"));
        v.setHora(rs.getString("hora"));
        try {
            String fp = rs.getString("forma_pago");
            v.setFormaPago(fp != null ? fp : "Efectivo");
        } catch (SQLException e) {
            v.setFormaPago("Efectivo"); // columna aún no existe en DB muy antigua sin migrar
        }
        try {
            v.setClienteId(rs.getInt("cliente_id"));
        } catch (SQLException e) {
            v.setClienteId(0); // columna aún no existe en DB muy antigua sin migrar
        }
        return v;
    }
}