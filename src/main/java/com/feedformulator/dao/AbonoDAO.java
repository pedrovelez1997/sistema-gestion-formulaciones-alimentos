package com.feedformulator.dao;

import com.feedformulator.model.Abono;
import com.feedformulator.model.Cliente;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AbonoDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ClienteDAO clienteDAO = new ClienteDAO();

    public void guardar(Abono abono) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            abono.setFechaAbono(LocalDate.now().format(DATE_FMT));
            abono.setHoraAbono(LocalTime.now().format(TIME_FMT));
            
            String sql = "INSERT INTO abonos (venta_credito_id, cliente_id, monto, forma_pago, fecha_abono, hora_abono, referencia) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, abono.getVentaCreditoId());
            ps.setInt(2, abono.getClienteId());
            ps.setDouble(3, abono.getMonto());
            ps.setString(4, abono.getFormaPago() != null ? abono.getFormaPago() : "EFECTIVO");
            ps.setString(5, abono.getFechaAbono());
            ps.setString(6, abono.getHoraAbono());
            ps.setString(7, abono.getReferencia());
            ps.executeUpdate();
            
            try (Statement stmt = conn.createStatement()) {
                rs = stmt.executeQuery("SELECT last_insert_rowid()");
                if (rs.next()) {
                    abono.setId(rs.getInt(1));
                }
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    public List<Abono> listarPorCliente(int clienteId) throws SQLException {
        List<Abono> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM abonos WHERE cliente_id = ? ORDER BY id DESC");
            ps.setInt(1, clienteId);
            rs = ps.executeQuery();
            while (rs.next()) {
                Abono a = mapear(rs);
                a.setCliente(clienteDAO.buscarPorId(a.getClienteId()));
                lista.add(a);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    public List<Abono> listarPorCredito(int ventaCreditoId) throws SQLException {
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
                Abono a = mapear(rs);
                a.setCliente(clienteDAO.buscarPorId(a.getClienteId()));
                lista.add(a);
            }
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    private Abono mapear(ResultSet rs) throws SQLException {
        Abono a = new Abono();
        a.setId(rs.getInt("id"));
        a.setVentaCreditoId(rs.getInt("venta_credito_id"));
        a.setClienteId(rs.getInt("cliente_id"));
        a.setMonto(rs.getDouble("monto"));
        a.setFormaPago(rs.getString("forma_pago"));
        a.setFechaAbono(rs.getString("fecha_abono"));
        a.setHoraAbono(rs.getString("hora_abono"));
        a.setReferencia(rs.getString("referencia"));
        return a;
    }
}