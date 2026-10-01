package com.feedformulator.dao;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaDAO {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // ============================================================
    //  MÉTODO PRINCIPAL - REGISTRAR AUDITORÍA
    // ============================================================
    public void registrar(String accion, String tabla, int registroId, String datosAnteriores, String datosNuevos) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            String sql = "INSERT INTO auditoria (usuario, accion, tabla, registro_id, datos_anteriores, datos_nuevos, fecha, hora) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, "Sistema");
            ps.setString(2, accion);
            ps.setString(3, tabla);
            ps.setInt(4, registroId);
            ps.setString(5, datosAnteriores);
            ps.setString(6, datosNuevos);
            ps.setString(7, LocalDate.now().format(DATE_FMT));
            ps.setString(8, LocalTime.now().format(TIME_FMT));
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    // ============================================================
    //  MÉTODOS ESPECÍFICOS PARA CADA OPERACIÓN
    // ============================================================
    
    public void registrarInsert(String tabla, int registroId, String datos) {
        registrar("INSERT", tabla, registroId, null, datos);
    }

    public void registrarUpdate(String tabla, int registroId, String datosAnteriores, String datosNuevos) {
        registrar("UPDATE", tabla, registroId, datosAnteriores, datosNuevos);
    }

    public void registrarDelete(String tabla, int registroId, String datos) {
        registrar("DELETE", tabla, registroId, datos, null);
    }

    // ============================================================
    //  NUEVO: MÉTODOS PARA ABONOS (con más detalle)
    // ============================================================
    
    /**
     * Registra la creación de un abono con información detallada
     */
    public void registrarAbonoInsert(int abonoId, int creditoId, int clienteId, double monto, String formaPago, String referencia) {
        String datos = String.format("abono_id:%d,credito_id:%d,cliente_id:%d,monto:%.2f,forma_pago:%s,referencia:%s",
            abonoId, creditoId, clienteId, monto, formaPago != null ? formaPago : "EFECTIVO", 
            referencia != null ? referencia : "N/A");
        registrarInsert("abonos", abonoId, datos);
    }

    /**
     * Registra la eliminación de un abono con toda la información
     */
    public void registrarAbonoDelete(int abonoId, int creditoId, int clienteId, double monto, String formaPago, 
                                      String fechaAbono, String horaAbono, String referencia) {
        String datos = String.format("abono_id:%d,credito_id:%d,cliente_id:%d,monto:%.2f,forma_pago:%s,fecha:%s,hora:%s,referencia:%s",
            abonoId, creditoId, clienteId, monto, formaPago != null ? formaPago : "EFECTIVO",
            fechaAbono, horaAbono, referencia != null ? referencia : "N/A");
        registrarDelete("abonos", abonoId, datos);
    }

    /**
     * Registra la actualización de un crédito al registrar/eliminar un abono
     */
    public void registrarCreditoUpdate(int creditoId, double nuevoAbonado, double nuevoSaldo, 
                                        String nuevoEstado, double gananciaRealizada) {
        String datos = String.format("credito_id:%d,abonado:%.2f,saldo:%.2f,estado:%s,ganancia_realizada:%.2f",
            creditoId, nuevoAbonado, nuevoSaldo, nuevoEstado, gananciaRealizada);
        registrarUpdate("ventas_credito", creditoId, null, datos);
    }

    /**
     * Registra la actualización completa de un crédito con datos anteriores y nuevos
     */
    public void registrarCreditoUpdateConComparacion(int creditoId, String datosAnteriores, String datosNuevos) {
        registrarUpdate("ventas_credito", creditoId, datosAnteriores, datosNuevos);
    }

    // ============================================================
    //  CONSULTAS DE AUDITORÍA
    // ============================================================
    
    /**
     * Obtiene todos los registros de auditoría para una tabla específica
     */
    public List<AuditoriaRegistro> obtenerPorTabla(String tabla) throws SQLException {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM auditoria WHERE tabla = ? ORDER BY id DESC");
            ps.setString(1, tabla);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    /**
     * Obtiene todos los registros de auditoría para un registro específico
     */
    public List<AuditoriaRegistro> obtenerPorRegistro(String tabla, int registroId) throws SQLException {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM auditoria WHERE tabla = ? AND registro_id = ? ORDER BY id DESC");
            ps.setString(1, tabla);
            ps.setInt(2, registroId);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    /**
     * Obtiene todos los registros de auditoría para una acción específica
     */
    public List<AuditoriaRegistro> obtenerPorAccion(String accion) throws SQLException {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM auditoria WHERE accion = ? ORDER BY id DESC");
            ps.setString(1, accion);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    /**
     * Obtiene el historial completo de auditoría (últimos 100 registros)
     */
    public List<AuditoriaRegistro> obtenerUltimos(int limite) throws SQLException {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM auditoria ORDER BY id DESC LIMIT ?");
            ps.setInt(1, limite);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    /**
     * Obtiene el historial de auditoría por rango de fechas
     */
    public List<AuditoriaRegistro> obtenerPorFecha(String fechaInicio, String fechaFin) throws SQLException {
        List<AuditoriaRegistro> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT * FROM auditoria WHERE fecha BETWEEN ? AND ? ORDER BY id DESC");
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
        return lista;
    }

    /**
     * Obtiene el historial de auditoría de abonos específicamente
     */
    public List<AuditoriaRegistro> obtenerHistorialAbonos() throws SQLException {
        return obtenerPorTabla("abonos");
    }

    /**
     * Obtiene el historial de auditoría de un abono específico
     */
    public List<AuditoriaRegistro> obtenerHistorialAbono(int abonoId) throws SQLException {
        return obtenerPorRegistro("abonos", abonoId);
    }

    /**
     * Obtiene el historial de auditoría de un crédito específico
     */
    public List<AuditoriaRegistro> obtenerHistorialCredito(int creditoId) throws SQLException {
        return obtenerPorRegistro("ventas_credito", creditoId);
    }

    // ============================================================
    //  MÉTODOS DE LIMPIEZA
    // ============================================================
    
    /**
     * Elimina registros de auditoría antiguos (más de X días)
     */
    public int eliminarAntiguos(int dias) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String fechaLimite = LocalDate.now().minusDays(dias).format(DATE_FMT);
            String sql = "DELETE FROM auditoria WHERE fecha < ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, fechaLimite);
            return ps.executeUpdate();
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    /**
     * Elimina registros de auditoría de una tabla específica
     */
    public int eliminarPorTabla(String tabla) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseManager.getConnection();
            String sql = "DELETE FROM auditoria WHERE tabla = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, tabla);
            return ps.executeUpdate();
            
        } finally {
            DatabaseManager.closeResources(conn, ps);
        }
    }

    // ============================================================
    //  MÉTODOS DE ESTADÍSTICAS
    // ============================================================
    
    /**
     * Obtiene el conteo de registros de auditoría por tabla
     */
    public int contarPorTabla(String tabla) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) as total FROM auditoria WHERE tabla = ?");
            ps.setString(1, tabla);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
            
        } finally {
            DatabaseManager.closeResources(conn, ps, rs);
        }
    }

    /**
     * Obtiene el conteo total de registros de auditoría
     */
    public int contarTotal() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM auditoria");
            return rs.next() ? rs.getInt("total") : 0;
            
        } finally {
            DatabaseManager.closeResources(conn, stmt, rs);
        }
    }

    // ============================================================
    //  MAPEAR RESULSET A OBJETO
    // ============================================================
    
    private AuditoriaRegistro mapear(ResultSet rs) throws SQLException {
        AuditoriaRegistro a = new AuditoriaRegistro();
        a.setId(rs.getInt("id"));
        a.setUsuario(rs.getString("usuario"));
        a.setAccion(rs.getString("accion"));
        a.setTabla(rs.getString("tabla"));
        a.setRegistroId(rs.getInt("registro_id"));
        a.setDatosAnteriores(rs.getString("datos_anteriores"));
        a.setDatosNuevos(rs.getString("datos_nuevos"));
        a.setFecha(rs.getString("fecha"));
        a.setHora(rs.getString("hora"));
        a.setIp(rs.getString("ip"));
        return a;
    }

    // ============================================================
    //  CLASE INTERNA PARA REGISTROS DE AUDITORÍA
    // ============================================================
    
    public static class AuditoriaRegistro {
        private int id;
        private String usuario;
        private String accion;
        private String tabla;
        private int registroId;
        private String datosAnteriores;
        private String datosNuevos;
        private String fecha;
        private String hora;
        private String ip;

        // Getters y Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        
        public String getAccion() { return accion; }
        public void setAccion(String accion) { this.accion = accion; }
        
        public String getTabla() { return tabla; }
        public void setTabla(String tabla) { this.tabla = tabla; }
        
        public int getRegistroId() { return registroId; }
        public void setRegistroId(int registroId) { this.registroId = registroId; }
        
        public String getDatosAnteriores() { return datosAnteriores; }
        public void setDatosAnteriores(String datosAnteriores) { this.datosAnteriores = datosAnteriores; }
        
        public String getDatosNuevos() { return datosNuevos; }
        public void setDatosNuevos(String datosNuevos) { this.datosNuevos = datosNuevos; }
        
        public String getFecha() { return fecha; }
        public void setFecha(String fecha) { this.fecha = fecha; }
        
        public String getHora() { return hora; }
        public void setHora(String hora) { this.hora = hora; }
        
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }

        /**
         * Obtiene un resumen legible del registro de auditoría
         */
        public String getResumen() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(fecha).append(" ").append(hora).append("] ");
            sb.append(accion).append(" en ").append(tabla);
            if (registroId > 0) {
                sb.append(" (ID: ").append(registroId).append(")");
            }
            sb.append(" por ").append(usuario);
            return sb.toString();
        }

        /**
         * Obtiene los detalles completos del registro de auditoría
         */
        public String getDetallesCompletos() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══════════════════════════════════════════════════════════════\n");
            sb.append("ID: ").append(id).append("\n");
            sb.append("Fecha: ").append(fecha).append(" ").append(hora).append("\n");
            sb.append("Usuario: ").append(usuario).append("\n");
            sb.append("Acción: ").append(accion).append("\n");
            sb.append("Tabla: ").append(tabla).append("\n");
            sb.append("Registro ID: ").append(registroId).append("\n");
            if (datosAnteriores != null && !datosAnteriores.isEmpty()) {
                sb.append("Datos anteriores: ").append(datosAnteriores).append("\n");
            }
            if (datosNuevos != null && !datosNuevos.isEmpty()) {
                sb.append("Datos nuevos: ").append(datosNuevos).append("\n");
            }
            sb.append("═══════════════════════════════════════════════════════════════\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return getResumen();
        }
    }
}