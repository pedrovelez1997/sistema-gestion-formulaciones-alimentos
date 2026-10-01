package com.feedformulator.dao;

import com.feedformulator.model.HistorialPrecio;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.util.MathUtils;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistorialPrecioDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final MateriaPrimaDAO materiaDAO = new MateriaPrimaDAO();

    public void registrarCambioPrecio(int productoId, double nuevoPrecio) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO historial_precios (producto_id, precio, fecha) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, productoId);
            ps.setDouble(2, MathUtils.redondear(nuevoPrecio));
            ps.setString(3, LocalDate.now().format(DATE_FMT));
            ps.executeUpdate();
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    public List<HistorialPrecio> listarPorProducto(int productoId) throws SQLException {
        List<HistorialPrecio> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM historial_precios WHERE producto_id = ? ORDER BY fecha DESC");
            ps.setInt(1, productoId);
            rs = ps.executeQuery();

            while (rs.next()) {
                HistorialPrecio hp = new HistorialPrecio();
                hp.setId(rs.getInt("id"));
                hp.setProductoId(rs.getInt("producto_id"));
                hp.setPrecio(rs.getDouble("precio"));
                hp.setFecha(rs.getString("fecha"));
                hp.setProducto(materiaDAO.buscarPorId(hp.getProductoId()));
                lista.add(hp);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public HistorialPrecio obtenerUltimoPrecio(int productoId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM historial_precios WHERE producto_id = ? ORDER BY fecha DESC LIMIT 1");
            ps.setInt(1, productoId);
            rs = ps.executeQuery();

            if (rs.next()) {
                HistorialPrecio hp = new HistorialPrecio();
                hp.setId(rs.getInt("id"));
                hp.setProductoId(rs.getInt("producto_id"));
                hp.setPrecio(rs.getDouble("precio"));
                hp.setFecha(rs.getString("fecha"));
                return hp;
            }
            return null;
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }
}