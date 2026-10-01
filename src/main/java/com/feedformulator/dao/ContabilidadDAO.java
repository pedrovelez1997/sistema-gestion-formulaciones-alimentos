package com.feedformulator.dao;

import com.feedformulator.model.MovimientoContable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContabilidadDAO {

    public void guardar(MovimientoContable m) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO contabilidad_movimientos " +
                "(tipo, categoria, descripcion, monto, fecha, estado, notas) VALUES (?,?,?,?,?,?,?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, m.getTipo());
            ps.setString(2, m.getCategoria());
            ps.setString(3, m.getDescripcion());
            ps.setDouble(4, m.getMonto());
            ps.setString(5, m.getFecha());
            ps.setString(6, m.getEstado() != null ? m.getEstado() : "ACTIVO");
            ps.setString(7, m.getNotas());
            ps.executeUpdate();

            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) m.setId(rs.getInt(1));
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(MovimientoContable m) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE contabilidad_movimientos SET tipo=?, categoria=?, descripcion=?, monto=?, fecha=?, estado=?, notas=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, m.getTipo());
            ps.setString(2, m.getCategoria());
            ps.setString(3, m.getDescripcion());
            ps.setDouble(4, m.getMonto());
            ps.setString(5, m.getFecha());
            ps.setString(6, m.getEstado());
            ps.setString(7, m.getNotas());
            ps.setInt(8, m.getId());
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public void marcarComoCobrado(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("UPDATE contabilidad_movimientos SET estado='COBRADO' WHERE id=?");
            ps.setInt(1, id);
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
            ps = conn.prepareStatement("DELETE FROM contabilidad_movimientos WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public List<MovimientoContable> listarTodos() throws SQLException {
        List<MovimientoContable> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM contabilidad_movimientos ORDER BY fecha DESC, id DESC");
            while (rs.next()) lista.add(mapear(rs));
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    /** Lee el texto libre de notas/borrador (vacío si nunca se ha guardado nada). */
    public String obtenerNotas() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT texto FROM contabilidad_notas WHERE id = 1");
            if (rs.next()) {
                String texto = rs.getString("texto");
                return texto != null ? texto : "";
            }
            return "";
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
    }

    /** Guarda (reemplaza) el texto libre de notas/borrador. */
    public void guardarNotas(String texto) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement(
                "INSERT INTO contabilidad_notas (id, texto) VALUES (1, ?) " +
                "ON CONFLICT(id) DO UPDATE SET texto = excluded.texto");
            ps.setString(1, texto);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    private MovimientoContable mapear(ResultSet rs) throws SQLException {
        MovimientoContable m = new MovimientoContable();
        m.setId(rs.getInt("id"));
        m.setTipo(rs.getString("tipo"));
        m.setCategoria(rs.getString("categoria"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setMonto(rs.getDouble("monto"));
        m.setFecha(rs.getString("fecha"));
        m.setEstado(rs.getString("estado"));
        m.setNotas(rs.getString("notas"));
        return m;
    }
}
