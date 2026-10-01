package com.feedformulator.dao;

import com.feedformulator.model.Usuario;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void guardar(Usuario usuario) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            usuario.setFechaRegistro(LocalDate.now().format(DATE_FMT));

            String sql = "INSERT INTO usuarios (nombre, usuario, contrasena, rol, estado, fecha_registro) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getContrasena());
            ps.setString(4, usuario.getRol());
            ps.setString(5, usuario.getEstado());
            ps.setString(6, usuario.getFechaRegistro());
            ps.executeUpdate();

            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public void actualizar(Usuario usuario) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseManager.getConnection();
            String sql = "UPDATE usuarios SET nombre=?, usuario=?, contrasena=?, rol=?, estado=? WHERE id=?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsuario());
            ps.setString(3, usuario.getContrasena());
            ps.setString(4, usuario.getRol());
            ps.setString(5, usuario.getEstado());
            ps.setInt(6, usuario.getId());
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public Usuario buscarPorId(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM usuarios WHERE id = ?");
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

    public Usuario buscarPorUsuario(String usuario) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM usuarios WHERE usuario = ?");
            ps.setString(1, usuario);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public Usuario autenticar(String usuario, String contrasena) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM usuarios WHERE usuario = ? AND contrasena = ? AND estado = 'ACTIVO'");
            ps.setString(1, usuario);
            ps.setString(2, contrasena);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapear(rs);
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM usuarios ORDER BY nombre");
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
        return lista;
    }

    public void eliminar(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public boolean existeUsuario(String usuario) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM usuarios WHERE usuario = ?");
            ps.setString(1, usuario);
            rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombre(rs.getString("nombre"));
        u.setUsuario(rs.getString("usuario"));
        u.setContrasena(rs.getString("contrasena"));
        u.setRol(rs.getString("rol"));
        u.setEstado(rs.getString("estado"));
        u.setFechaRegistro(rs.getString("fecha_registro"));
        return u;
    }
}