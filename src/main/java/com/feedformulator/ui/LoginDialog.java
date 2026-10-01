package com.feedformulator.ui;

import com.feedformulator.dao.UsuarioDAO;
import com.feedformulator.model.Usuario;
import com.feedformulator.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginDialog extends JDialog {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnCancelar;
    private JLabel lblError;
    private boolean autenticado = false;
    
    // Selector de tipo de usuario
    private JComboBox<String> cmbTipoUsuario;
    private JPanel panelAdminInfo;  // Panel con mensaje informativo para admin

    public LoginDialog(JFrame parent) {
        super(parent, "Iniciar Sesión - Feed Formulator Pro", ModalityType.APPLICATION_MODAL);
        setSize(450, 620);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // ============================================================
        //  1. BANNER SUPERIOR
        // ============================================================
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(6, 78, 59), getWidth(), getHeight(), new Color(2, 44, 34));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setPreferredSize(new Dimension(450, 110));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Icono
        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillOval(0, 0, 48, 48);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 22));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (48 - fm.stringWidth("F")) / 2;
                int y = (48 + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString("F", x, y);
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(48, 48);
            }
        };
        iconCircle.setOpaque(false);
        iconCircle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconCircle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel lblTitle = new JLabel("FEED FORMULATOR PRO");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblTitle);

        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel lblSub = new JLabel("Sistema de Gestión de Alimentos Balanceados");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(167, 243, 208));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(lblSub);

        add(headerPanel, BorderLayout.NORTH);

        // ============================================================
        //  2. CUERPO DEL FORMULARIO
        // ============================================================
        JPanel bodyPanel = new JPanel();
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.setLayout(new GridBagLayout());
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        // ===== Selector de tipo de usuario =====
        JLabel lblTipoUsuario = new JLabel("Tipo de acceso:");
        lblTipoUsuario.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTipoUsuario.setForeground(new Color(51, 65, 85));
        gbc.gridy = row++;
        gbc.insets = new Insets(6, 0, 2, 0);
        bodyPanel.add(lblTipoUsuario, gbc);

        cmbTipoUsuario = new JComboBox<>(new String[]{"👤 Usuario", "👑 Administrador"});
        cmbTipoUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbTipoUsuario.setPreferredSize(new Dimension(340, 38));
        cmbTipoUsuario.setBackground(Color.WHITE);
        cmbTipoUsuario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 12, 0);
        bodyPanel.add(cmbTipoUsuario, gbc);

        // ===== Panel de información para Administrador (NUEVO) =====
        panelAdminInfo = new JPanel();
        panelAdminInfo.setOpaque(true);
        panelAdminInfo.setBackground(new Color(255, 248, 240));
        panelAdminInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 200, 100), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panelAdminInfo.setLayout(new BorderLayout());
        panelAdminInfo.setVisible(false);  // Oculto por defecto

        JLabel lblAdminInfo = new JLabel("👑  Modo Administrador");
        lblAdminInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAdminInfo.setForeground(new Color(180, 100, 20));
        panelAdminInfo.add(lblAdminInfo, BorderLayout.WEST);

        JLabel lblAdminHint = new JLabel("Usuario: admin | Contraseña: admin123 (puedes cambiarla)");
        lblAdminHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblAdminHint.setForeground(new Color(120, 120, 120));
        panelAdminInfo.add(lblAdminHint, BorderLayout.EAST);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        bodyPanel.add(panelAdminInfo, gbc);

        // ===== LABEL DE ERROR =====
        lblError = new JLabel("", SwingConstants.CENTER);
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblError.setForeground(new Color(220, 38, 38));
        lblError.setVisible(false);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 6, 0);
        bodyPanel.add(lblError, gbc);

        // ===== CAMPO USUARIO =====
        JLabel lblUser = new JLabel("Usuario:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(51, 65, 85));
        gbc.gridy = row++;
        gbc.insets = new Insets(6, 0, 2, 0);
        bodyPanel.add(lblUser, gbc);

        txtUsuario = new JTextField();
        txtUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtUsuario.setPreferredSize(new Dimension(340, 38));
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 8, 0);
        bodyPanel.add(txtUsuario, gbc);

        // ===== CAMPO CONTRASEÑA =====
        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(new Color(51, 65, 85));
        gbc.gridy = row++;
        gbc.insets = new Insets(6, 0, 2, 0);
        bodyPanel.add(lblPass, gbc);

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPassword.setPreferredSize(new Dimension(340, 38));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);
        bodyPanel.add(txtPassword, gbc);

        // ===== BOTONES =====
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setOpaque(false);

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancelar.setForeground(new Color(71, 85, 105));
        btnCancelar.setBackground(new Color(241, 245, 249));
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBorderPainted(false);
        btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancelar.setPreferredSize(new Dimension(150, 40));
        btnCancelar.addActionListener(e -> {
            autenticado = false;
            dispose();
        });

        btnLogin = new JButton("Iniciar Sesión");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(new Color(5, 150, 105));
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setPreferredSize(new Dimension(150, 40));
        btnLogin.addActionListener(e -> iniciarSesion());

        // Enter para login
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    iniciarSesion();
                }
            }
        });
        txtUsuario.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    txtPassword.requestFocus();
                }
            }
        });

        // ===== CAMBIO DE TIPO DE USUARIO =====
        cmbTipoUsuario.addActionListener(e -> {
            boolean esAdmin = cmbTipoUsuario.getSelectedIndex() == 1;
            
            // Mostrar/ocultar panel de información de admin
            panelAdminInfo.setVisible(esAdmin);
            
            if (esAdmin) {
                // Precargar credenciales de admin
                txtUsuario.setText("admin");
                txtPassword.setText("admin123");
                // Enfocar el campo de contraseña para que el usuario pueda escribir si quiere cambiarla
                txtPassword.selectAll();
                txtPassword.requestFocus();
            } else {
                txtUsuario.setText("");
                txtPassword.setText("");
                txtUsuario.requestFocus();
            }
            pack();
        });

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnLogin);

        gbc.gridy = row++;
        gbc.insets = new Insets(16, 0, 0, 0);
        bodyPanel.add(buttonPanel, gbc);

        // ============================================================
        //  3. PIE DE PÁGINA
        // ============================================================
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        footerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        JLabel lblFooter = new JLabel("v2.0  |  Elige tu tipo de acceso y escribe tus credenciales");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter.setForeground(new Color(148, 163, 184));
        footerPanel.add(lblFooter);

        add(bodyPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);
        
        // Seleccionar "Usuario" por defecto
        cmbTipoUsuario.setSelectedIndex(0);
    }

    // ============================================================
    //  MÉTODO DE AUTENTICACIÓN
    // ============================================================
    private void iniciarSesion() {
        // Ocultar error anterior
        lblError.setVisible(false);

        // ===== OBTENER VALORES =====
        final String usuario = txtUsuario.getText().trim();
        final String contrasena = new String(txtPassword.getPassword()).trim();
        final boolean esAdminSeleccionado = cmbTipoUsuario.getSelectedIndex() == 1;

        // Validaciones
        if (usuario.isEmpty()) {
            lblError.setText("Ingrese el nombre de usuario");
            lblError.setVisible(true);
            txtUsuario.requestFocus();
            return;
        }
        if (contrasena.isEmpty()) {
            lblError.setText("Ingrese la contraseña");
            lblError.setVisible(true);
            txtPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verificando...");

        SwingWorker<Usuario, Void> worker = new SwingWorker<Usuario, Void>() {
            @Override
            protected Usuario doInBackground() {
                try {
                    return usuarioDAO.autenticar(usuario, contrasena);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar Sesión");

                try {
                    Usuario user = get();
                    
                    if (user == null) {
                        lblError.setText("Usuario o contraseña incorrectos");
                        lblError.setVisible(true);
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        return;
                    }

                    // ============================================================
                    //  VALIDAR ROL SEGÚN LO SELECCIONADO
                    // ============================================================
                    boolean esAdminReal = "ADMIN".equals(user.getRol());

                    if (esAdminSeleccionado && !esAdminReal) {
                        lblError.setText("❌ Este usuario NO es administrador");
                        lblError.setVisible(true);
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        return;
                    }

                    if (!esAdminSeleccionado && esAdminReal) {
                        int opcion = JOptionPane.showConfirmDialog(
                            LoginDialog.this,
                            "👑 Este usuario es ADMINISTRADOR.\n\n" +
                            "¿Desea continuar como usuario normal?\n" +
                            "(Si selecciona 'No', podrá cambiar a modo Administrador)",
                            "Acceso especial",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                        );
                        
                        if (opcion != JOptionPane.YES_OPTION) {
                            cmbTipoUsuario.setSelectedIndex(1);
                            txtPassword.setText("");
                            txtPassword.requestFocus();
                            return;
                        }
                    }

                    // ===== INICIO DE SESIÓN EXITOSO =====
                    SessionManager.setUsuarioActual(user);
                    autenticado = true;
                    dispose();

                } catch (Exception e) {
                    lblError.setText("Error al iniciar sesión: " + e.getMessage());
                    lblError.setVisible(true);
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    public boolean isAutenticado() {
        return autenticado;
    }

    public static Usuario mostrarLogin(JFrame parent) {
        LoginDialog dialog = new LoginDialog(parent);
        dialog.setVisible(true);
        if (dialog.isAutenticado()) {
            return SessionManager.getUsuarioActual();
        }
        return null;
    }
}