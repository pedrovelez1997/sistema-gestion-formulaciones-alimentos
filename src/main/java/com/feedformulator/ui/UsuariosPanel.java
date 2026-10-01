package com.feedformulator.ui;

import com.feedformulator.dao.UsuarioDAO;
import com.feedformulator.model.Usuario;
import com.feedformulator.util.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class UsuariosPanel extends JPanel {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    
    private DefaultTableModel tableModel;
    private JTable tabla;
    private JTextField txtBuscar;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel lblTotalUsuarios;
    
    private static final Color COLOR_PRIMARIO = new Color(41, 128, 185);
    private static final Color COLOR_VERDE = new Color(34, 139, 34);
    private static final Color COLOR_ROJO = new Color(180, 40, 40);
    private static final Color COLOR_NARANJA = new Color(230, 126, 34);
    private static final Color FONDO = new Color(240, 245, 248);

    public UsuariosPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(FONDO);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        add(crearTitulo(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelResumen(), BorderLayout.SOUTH);
        
        cargarDatos();
    }
    
    private JPanel crearTitulo() {
        return DialogStyler.crearHeaderPagina("👤", "Gestión de Usuarios", "Cuentas con acceso al sistema");
    }
    
    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(FONDO);
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(FONDO);
        
        JPanel tituloPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tituloPanel.setBackground(FONDO);
        JLabel lblTitulo = new JLabel(" Usuarios Registrados");
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/icons/clipboard-icon.png"));
            Image img = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
            lblTitulo.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            lblTitulo.setText("📋  Usuarios Registrados");
        }
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tituloPanel.add(lblTitulo);
        header.add(tituloPanel, BorderLayout.WEST);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.setBackground(FONDO);
        
        // Buscador
        try {
            ImageIcon iconSearch = new ImageIcon(getClass().getResource("/icons/magnifying-glass.png"));
            Image imgSearch = iconSearch.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JLabel lblBuscar = new JLabel(new ImageIcon(imgSearch));
            lblBuscar.setToolTipText("Buscar");
            btnPanel.add(lblBuscar);
        } catch (Exception exSearch) {
            JLabel lblBuscar = new JLabel("🔍");
            lblBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btnPanel.add(lblBuscar);
        }
        
        txtBuscar = new JTextField(20);
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBuscar.setToolTipText("Buscar por nombre o usuario");
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filtrar();
            }
        });
        btnPanel.add(txtBuscar);
        
        // Botones
        try {
            ImageIcon iconAdd = new ImageIcon(getClass().getResource("/icons/add.png"));
            Image imgAdd = iconAdd.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JButton btnNuevo = new JButton("Nuevo", new ImageIcon(imgAdd));
            btnNuevo.setBackground(COLOR_PRIMARIO);
            btnNuevo.setForeground(Color.WHITE);
            btnNuevo.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnNuevo.setFocusPainted(false);
            btnNuevo.setBorderPainted(false);
            btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnNuevo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnNuevo.addActionListener(e -> mostrarDialogoNuevoUsuario());
            btnPanel.add(btnNuevo);
        } catch (Exception exAdd) {
            JButton btnNuevo = new JButton("➕ Nuevo");
            btnNuevo.setBackground(COLOR_PRIMARIO);
            btnNuevo.setForeground(Color.WHITE);
            btnNuevo.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnNuevo.setFocusPainted(false);
            btnNuevo.setBorderPainted(false);
            btnNuevo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnNuevo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnNuevo.addActionListener(e -> mostrarDialogoNuevoUsuario());
            btnPanel.add(btnNuevo);
        }

        try {
            ImageIcon iconEdit = new ImageIcon(getClass().getResource("/icons/edit.png"));
            Image imgEdit = iconEdit.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JButton btnEditar = new JButton("Editar", new ImageIcon(imgEdit));
            btnEditar.setBackground(COLOR_NARANJA);
            btnEditar.setForeground(Color.WHITE);
            btnEditar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnEditar.setFocusPainted(false);
            btnEditar.setBorderPainted(false);
            btnEditar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEditar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnEditar.addActionListener(e -> editarUsuario());
            btnPanel.add(btnEditar);
        } catch (Exception exEdit) {
            JButton btnEditar = new JButton("✏️ Editar");
            btnEditar.setBackground(COLOR_NARANJA);
            btnEditar.setForeground(Color.WHITE);
            btnEditar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnEditar.setFocusPainted(false);
            btnEditar.setBorderPainted(false);
            btnEditar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEditar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnEditar.addActionListener(e -> editarUsuario());
            btnPanel.add(btnEditar);
        }

        try {
            ImageIcon iconDelete = new ImageIcon(getClass().getResource("/icons/delete.png"));
            Image imgDelete = iconDelete.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JButton btnEliminar = new JButton("Eliminar", new ImageIcon(imgDelete));
            btnEliminar.setBackground(COLOR_ROJO);
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEliminar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnEliminar.addActionListener(e -> eliminarUsuario());
            btnPanel.add(btnEliminar);
        } catch (Exception exDelete) {
            JButton btnEliminar = new JButton("🗑 Eliminar");
            btnEliminar.setBackground(COLOR_ROJO);
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorderPainted(false);
            btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEliminar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnEliminar.addActionListener(e -> eliminarUsuario());
            btnPanel.add(btnEliminar);
        }

        try {
            ImageIcon iconRefresh = new ImageIcon(getClass().getResource("/icons/refresh.png"));
            Image imgRefresh = iconRefresh.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            JButton btnRefrescar = new JButton(new ImageIcon(imgRefresh));
            btnRefrescar.setBackground(new Color(100, 100, 100));
            btnRefrescar.setForeground(Color.WHITE);
            btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnRefrescar.setFocusPainted(false);
            btnRefrescar.setBorderPainted(false);
            btnRefrescar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnRefrescar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnRefrescar.addActionListener(e -> cargarDatos());
            btnRefrescar.setToolTipText("Refrescar");
            btnPanel.add(btnRefrescar);
        } catch (Exception exRefresh) {
            JButton btnRefrescar = new JButton("🔄");
            btnRefrescar.setBackground(new Color(100, 100, 100));
            btnRefrescar.setForeground(Color.WHITE);
            btnRefrescar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnRefrescar.setFocusPainted(false);
            btnRefrescar.setBorderPainted(false);
            btnRefrescar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnRefrescar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btnRefrescar.addActionListener(e -> cargarDatos());
            btnPanel.add(btnRefrescar);
        }
        
        header.add(btnPanel, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        
        String[] cols = {"ID", "Nombre", "Usuario", "Rol", "Estado", "Fecha Registro"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabla = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        tabla.setRowSorter(sorter);
        estilizarTabla(tabla);
        
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel crearPanelResumen() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        lblTotalUsuarios = new JLabel("👤 Total Usuarios: 0");
        lblTotalUsuarios.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        JLabel lblInfo = new JLabel("🔒 Solo Administradores pueden gestionar usuarios");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(new Color(100, 100, 100));
        
        panel.add(lblTotalUsuarios);
        panel.add(lblInfo);
        
        return panel;
    }
    
    private void estilizarTabla(JTable t) {
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(COLOR_PRIMARIO);
        t.getTableHeader().setForeground(Color.WHITE);
        t.setSelectionBackground(new Color(200, 220, 240));
        t.setShowGrid(true);
        t.setGridColor(new Color(220, 225, 230));
    }
    
    private void filtrar() {
        String filtro = txtBuscar.getText().trim().toLowerCase();
        if (filtro.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filtro, 1, 2));
        }
    }
    
    public void cargarDatos() {
        try {
            tableModel.setRowCount(0);
            List<Usuario> usuarios = usuarioDAO.listarTodos();
            
            for (Usuario u : usuarios) {
                tableModel.addRow(new Object[]{
                    u.getId(),
                    u.getNombre(),
                    u.getUsuario(),
                    u.getRol(),
                    u.getEstado(),
                    u.getFechaRegistro()
                });
            }
            
            lblTotalUsuarios.setText("👤 Total Usuarios: " + usuarios.size());
            filtrar();
            
        } catch (Exception e) {
            DialogStyler.mostrarError(this, "Error al cargar usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void mostrarDialogoNuevoUsuario() {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Nuevo Usuario",
            Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(500, 480);
        dialog.setLocationRelativeTo(this);

        DialogStyler.aplicar(dialog, "Nuevo Usuario", "👤");

        JPanel form = DialogStyler.crearPanelFormulario();
        GridBagConstraints gbc = DialogStyler.crearGBC();

        JTextField txtNombre = DialogStyler.crearCampo(20);
        JTextField txtUsuario = DialogStyler.crearCampo(20);
        JPasswordField txtContrasena = new JPasswordField(20);
        txtContrasena.setFont(DialogStyler.FUENTE_CAMPO);
        JComboBox<String> cmbRol = DialogStyler.crearCombo(new String[]{"USUARIO", "ADMIN"});
        JComboBox<String> cmbEstado = DialogStyler.crearCombo(new String[]{"ACTIVO", "INACTIVO"});

        int fila = 0;

        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        form.add(txtNombre, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Usuario:*"), gbc);
        gbc.gridx = 1;
        form.add(txtUsuario, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Contraseña:*"), gbc);
        gbc.gridx = 1;
        form.add(txtContrasena, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Rol:"), gbc);
        gbc.gridx = 1;
        form.add(cmbRol, gbc);

        fila++;
        gbc.gridx = 0; gbc.gridy = fila;
        form.add(DialogStyler.crearLabel("Estado:"), gbc);
        gbc.gridx = 1;
        form.add(cmbEstado, gbc);

        JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar");
        JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

        btnGuardar.addActionListener(e -> {
            try {
                String nombre = txtNombre.getText().trim();
                String usuario = txtUsuario.getText().trim();
                String contrasena = new String(txtContrasena.getPassword()).trim();

                if (nombre.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "El nombre no puede estar vacío.");
                    return;
                }
                if (usuario.isEmpty()) {
                    DialogStyler.mostrarError(dialog, "El usuario no puede estar vacío.");
                    return;
                }
                if (contrasena.isEmpty() || contrasena.length() < 4) {
                    DialogStyler.mostrarError(dialog, "La contraseña debe tener al menos 4 caracteres.");
                    return;
                }
                if (usuarioDAO.existeUsuario(usuario)) {
                    DialogStyler.mostrarError(dialog, "El usuario '" + usuario + "' ya existe.");
                    return;
                }

                Usuario u = new Usuario();
                u.setNombre(nombre);
                u.setUsuario(usuario);
                u.setContrasena(contrasena);
                u.setRol((String) cmbRol.getSelectedItem());
                u.setEstado((String) cmbEstado.getSelectedItem());

                usuarioDAO.guardar(u);

                dialog.dispose();
                cargarDatos();

                DialogStyler.mostrarExito(this, "Usuario <b>" + usuario + "</b> creado correctamente.", "Éxito");

            } catch (Exception ex) {
                DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        btnCancelar.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) { txtNombre.requestFocusInWindow(); }
        });

        dialog.setVisible(true);
    }
    
    private void editarUsuario() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un usuario para editar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tabla.convertRowIndexToModel(row);
            int id = (int) tableModel.getValueAt(modelRow, 0);
            Usuario usuario = usuarioDAO.buscarPorId(id);
            
            if (usuario == null) {
                DialogStyler.mostrarError(this, "Usuario no encontrado.");
                return;
            }
            
            JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Editar Usuario",
                Dialog.ModalityType.APPLICATION_MODAL
            );
            dialog.setSize(500, 480);
            dialog.setLocationRelativeTo(this);

            DialogStyler.aplicar(dialog, "Editar Usuario", "✏️");

            JPanel form = DialogStyler.crearPanelFormulario();
            GridBagConstraints gbc = DialogStyler.crearGBC();

            JTextField txtNombre = DialogStyler.crearCampo(20);
            txtNombre.setText(usuario.getNombre());
            JTextField txtUsuario = DialogStyler.crearCampo(20);
            txtUsuario.setText(usuario.getUsuario());
            txtUsuario.setEditable(false);
            JPasswordField txtContrasena = new JPasswordField(20);
            txtContrasena.setFont(DialogStyler.FUENTE_CAMPO);
            txtContrasena.setToolTipText("Dejar vacío para mantener la contraseña actual");
            JComboBox<String> cmbRol = DialogStyler.crearCombo(new String[]{"USUARIO", "ADMIN"});
            cmbRol.setSelectedItem(usuario.getRol());
            JComboBox<String> cmbEstado = DialogStyler.crearCombo(new String[]{"ACTIVO", "INACTIVO"});
            cmbEstado.setSelectedItem(usuario.getEstado());

            int fila = 0;

            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Nombre:*"), gbc);
            gbc.gridx = 1;
            form.add(txtNombre, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Usuario:"), gbc);
            gbc.gridx = 1;
            form.add(txtUsuario, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Nueva Contraseña:"), gbc);
            gbc.gridx = 1;
            form.add(txtContrasena, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Rol:"), gbc);
            gbc.gridx = 1;
            form.add(cmbRol, gbc);

            fila++;
            gbc.gridx = 0; gbc.gridy = fila;
            form.add(DialogStyler.crearLabel("Estado:"), gbc);
            gbc.gridx = 1;
            form.add(cmbEstado, gbc);

            JButton btnGuardar = DialogStyler.crearBotonPrimario("💾 Guardar Cambios");
            JButton btnCancelar = DialogStyler.crearBotonSecundario("Cancelar");

            btnGuardar.addActionListener(e -> {
                try {
                    String nombre = txtNombre.getText().trim();
                    String nuevaContrasena = new String(txtContrasena.getPassword()).trim();

                    if (nombre.isEmpty()) {
                        DialogStyler.mostrarError(dialog, "El nombre no puede estar vacío.");
                        return;
                    }

                    usuario.setNombre(nombre);
                    if (!nuevaContrasena.isEmpty()) {
                        if (nuevaContrasena.length() < 4) {
                            DialogStyler.mostrarError(dialog, "La contraseña debe tener al menos 4 caracteres.");
                            return;
                        }
                        usuario.setContrasena(nuevaContrasena);
                    }
                    usuario.setRol((String) cmbRol.getSelectedItem());
                    usuario.setEstado((String) cmbEstado.getSelectedItem());

                    usuarioDAO.actualizar(usuario);

                    dialog.dispose();
                    cargarDatos();

                    DialogStyler.mostrarExito(this, "Usuario <b>" + usuario.getUsuario() + "</b> actualizado correctamente.", "Éxito");

                } catch (Exception ex) {
                    DialogStyler.mostrarError(dialog, "Error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            btnCancelar.addActionListener(e -> dialog.dispose());

            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(DialogStyler.FONDO_FORM);

            dialog.add(scroll, BorderLayout.CENTER);
            dialog.add(DialogStyler.crearPanelBotones(btnCancelar, btnGuardar), BorderLayout.SOUTH);
            dialog.setVisible(true);
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void eliminarUsuario() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            DialogStyler.mostrarAdvertencia(this, "Seleccione un usuario para eliminar.", "Aviso");
            return;
        }
        
        try {
            int modelRow = tabla.convertRowIndexToModel(row);
            int id = (int) tableModel.getValueAt(modelRow, 0);
            String nombre = (String) tableModel.getValueAt(modelRow, 1);
            String usuario = (String) tableModel.getValueAt(modelRow, 2);
            
            // No permitir eliminar al usuario actual
            Usuario actual = SessionManager.getUsuarioActual();
            if (actual != null && actual.getId() == id) {
                DialogStyler.mostrarError(this, "No puede eliminar su propio usuario.");
                return;
            }
            
            String mensaje = "⚠️ ¿Eliminar el usuario <b>" + nombre + "</b>?\n\n" +
                "Usuario: <b>" + usuario + "</b>\n\n" +
                "Esta acción NO se puede deshacer.\n" +
                "¿Está seguro?";
            
            if (!DialogStyler.confirmar(this, mensaje, "Eliminar usuario")) return;
            
            usuarioDAO.eliminar(id);
            cargarDatos();
            
            DialogStyler.mostrarExito(this, "Usuario <b>" + nombre + "</b> eliminado correctamente.", "Éxito");
            
        } catch (Exception ex) {
            DialogStyler.mostrarError(this, "Error al eliminar usuario: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}