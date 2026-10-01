package com.feedformulator.dao;

import com.feedformulator.model.Proveedor;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void guardar(Proveedor proveedor) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            if (proveedor.getCodigo() == null || proveedor.getCodigo().isEmpty()) {
                proveedor.setCodigo(generarCodigo(proveedor.getNombre()));
            }
            proveedor.setFechaRegistro(LocalDate.now().format(DATE_FMT));
            
            String sql = "INSERT INTO proveedores (codigo, nombre, telefono, email, direccion, ruc_cedula, contacto, tipo_proveedor, fecha_registro, estado) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, proveedor.getCodigo());
            ps.setString(2, proveedor.getNombre());
            ps.setString(3, proveedor.getTelefono());
            ps.setString(4, proveedor.getEmail());
            ps.setString(5, proveedor.getDireccion());
            ps.setString(6, proveedor.getRucCedula());
            ps.setString(7, proveedor.getContacto());
            ps.setString(8, proveedor.getTipoProveedor() != null ? proveedor.getTipoProveedor() : "MATERIA_PRIMA");
            ps.setString(9, proveedor.getFechaRegistro());
            ps.setString(10, proveedor.getEstado() != null ? proveedor.getEstado() : "ACTIVO");
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    proveedor.setId(rs.getInt(1));
                }
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(Proveedor proveedor) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE proveedores SET nombre=?, telefono=?, email=?, direccion=?, ruc_cedula=?, contacto=?, tipo_proveedor=?, estado=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, proveedor.getNombre());
            ps.setString(2, proveedor.getTelefono());
            ps.setString(3, proveedor.getEmail());
            ps.setString(4, proveedor.getDireccion());
            ps.setString(5, proveedor.getRucCedula());
            ps.setString(6, proveedor.getContacto());
            ps.setString(7, proveedor.getTipoProveedor());
            ps.setString(8, proveedor.getEstado());
            ps.setInt(9, proveedor.getId());
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public Proveedor buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM proveedores WHERE id = ?");
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

    public Proveedor buscarPorCodigo(String codigo) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM proveedores WHERE codigo = ?");
            ps.setString(1, codigo);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Proveedor> listarTodos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM proveedores ORDER BY nombre");
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public List<Proveedor> listarActivos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM proveedores WHERE estado = 'ACTIVO' ORDER BY nombre");
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("DELETE FROM proveedores WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public boolean tieneComprasPendientes(int proveedorId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) as total FROM compras_proveedores WHERE proveedor_id = ? AND estado_pago IN ('PENDIENTE', 'ABONADO')");
            ps.setInt(1, proveedorId);
            rs = ps.executeQuery();
            return rs.next() && rs.getInt("total") > 0;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private String generarCodigo(String nombre) throws SQLException {
        String prefijo = nombre.length() >= 3 ? nombre.substring(0, 3).toUpperCase() : nombre.toUpperCase();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "SELECT COUNT(*) FROM proveedores WHERE codigo LIKE ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, prefijo + "%");
            rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt(1) : 0;
            return prefijo + "-" + String.format("%03d", count + 1);
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setDireccion(rs.getString("direccion"));
        p.setRucCedula(rs.getString("ruc_cedula"));
        p.setContacto(rs.getString("contacto"));
        p.setTipoProveedor(rs.getString("tipo_proveedor"));
        p.setFechaRegistro(rs.getString("fecha_registro"));
        p.setEstado(rs.getString("estado"));
        return p;
    }
}