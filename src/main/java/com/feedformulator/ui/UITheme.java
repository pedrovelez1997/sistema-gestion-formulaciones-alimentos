package com.feedformulator.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class UITheme {

    public static final Color VERDE          = new Color(16, 185, 129);
    public static final Color VERDE_OSCURO   = new Color(5, 150, 105);
    public static final Color FONDO          = new Color(248, 250, 252);
    public static final Color ROJO           = new Color(239, 68, 68);
    public static final Color NARANJA        = new Color(245, 158, 11);
    public static final Color AZUL           = new Color(37, 99, 235);
    public static final Color TEXTO          = new Color(15, 23, 42);

    public static void showSuccess(Component parent, String mensaje) {
        showNotification(parent, "✅  Operación Exitosa", mensaje, VERDE, new Color(236, 253, 245));
    }

    public static void showError(Component parent, String mensaje) {
        showNotification(parent, "❌  Error", mensaje, ROJO, new Color(254, 242, 242));
    }

    public static void showWarning(Component parent, String mensaje) {
        showNotification(parent, "⚠️  Advertencia", mensaje, NARANJA, new Color(255, 251, 235));
    }

    public static boolean showConfirm(Component parent, String mensaje) {
        int res = JOptionPane.showConfirmDialog(
            parent,
            "<html><body style='font-family:Segoe UI;font-size:12px;width:320px;padding:4px;'>" + mensaje + "</body></html>",
            "Confirmar Acción",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return res == JOptionPane.YES_OPTION;
    }

    private static void showNotification(Component parent, String titulo, String mensaje, Color colorBorder, Color colorBg) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(colorBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(colorBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 16, 16));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTit.setForeground(colorBorder);

        JLabel lblMsg = new JLabel("<html><body style='width:300px;font-family:Segoe UI;font-size:12px;color:#0F172A;'>" + mensaje + "</body></html>");
        lblMsg.setBorder(BorderFactory.createEmptyBorder(8, 0, 14, 0));

        JButton btnOk = DialogStyler.crearBotonPrimario("Aceptar");
        btnOk.addActionListener(e -> d.dispose());

        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pBtn.setOpaque(false);
        pBtn.add(btnOk);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(lblTit, BorderLayout.NORTH);
        content.add(lblMsg, BorderLayout.CENTER);
        content.add(pBtn, BorderLayout.SOUTH);

        root.add(content);
        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }
}