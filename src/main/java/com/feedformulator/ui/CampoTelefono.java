package com.feedformulator.ui;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.text.ParseException;

public class CampoTelefono extends JFormattedTextField {
    
    public CampoTelefono() {
        super(crearMaskFormatter());
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        setColumns(15);
    }
    
    private static MaskFormatter crearMaskFormatter() {
        try {
            MaskFormatter mask = new MaskFormatter("###-###-####");
            mask.setPlaceholderCharacter('_');
            return mask;
        } catch (ParseException e) {
            return null;
        }
    }
    
    public String getTelefonoSinFormato() {
        String texto = getText();
        return texto.replaceAll("[^0-9]", "");
    }
    
    @Override
    public String getText() {
        String texto = super.getText();
        if (texto == null) return "";
        return texto;
    }
}