package com.feedformulator.dao;

import com.feedformulator.model.Cliente;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void guardar(Cliente cliente) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            if (cliente.getCodigo() == null || cliente.getCodigo().isEmpty()) {
                cliente.setCodigo(generarCodigo(cliente.getNombre()));
            }
            cliente.setFechaRegistro(LocalDate.now().format(DATE_FMT));
            
            String sql = "INSERT INTO clientes (codigo, nombre, telefono, email, direccion, ruc_cedula, fecha_registro, estado) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, cliente.getCodigo());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getDireccion());
            ps.setString(6, cliente.getRucCedula());
            ps.setString(7, cliente.getFechaRegistro());
            ps.setString(8, cliente.getEstado() != null ? cliente.getEstado() : "ACTIVO");
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    cliente.setId(rs.getInt(1));
                }
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(Cliente cliente) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE clientes SET nombre=?, telefono=?, email=?, direccion=?, ruc_cedula=?, estado=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getTelefono());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getDireccion());
            ps.setString(5, cliente.getRucCedula());
            ps.setString(6, cliente.getEstado());
            ps.setInt(7, cliente.getId());
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public Cliente buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM clientes WHERE id = ?");
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

    public Cliente buscarPorCodigo(String codigo) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM clientes WHERE codigo = ?");
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

    public Cliente buscarPorNombre(String nombre) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM clientes WHERE nombre LIKE ?");
            ps.setString(1, "%" + nombre + "%");
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM clientes ORDER BY nombre");
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM clientes WHERE estado = 'ACTIVO' ORDER BY nombre");
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
            ps = conn.prepareStatement("DELETE FROM clientes WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public boolean tieneCreditosPendientes(int clienteId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) as total FROM ventas_credito WHERE cliente_id = ? AND estado IN ('PENDIENTE', 'ABONADO')");
            ps.setInt(1, clienteId);
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
            String sql = "SELECT COUNT(*) FROM clientes WHERE codigo LIKE ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, prefijo + "%");
            rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt(1) : 0;
            return prefijo + "-" + String.format("%03d", count + 1);
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setCodigo(rs.getString("codigo"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        c.setRucCedula(rs.getString("ruc_cedula"));
        c.setFechaRegistro(rs.getString("fecha_registro"));
        c.setEstado(rs.getString("estado"));
        return c;
    }
}