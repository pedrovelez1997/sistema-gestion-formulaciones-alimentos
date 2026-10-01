package com.feedformulator.report;

import com.feedformulator.dao.MovimientoInventarioDAO;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.MovimientoInventario;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReporteMovimientosMejorado {
    private static final BaseColor COLOR_VERDE = new BaseColor(34, 139, 34);
    private static final BaseColor COLOR_VERDE_CLARO = new BaseColor(144, 238, 144);
    private static final BaseColor COLOR_GRIS = new BaseColor(245, 245, 245);
    private static final BaseColor COLOR_VERDE_OSCURO = new BaseColor(20, 100, 20);
    private static final BaseColor COLOR_NARANJA = new BaseColor(200, 100, 20);
    private static final BaseColor COLOR_ROJO = new BaseColor(200, 50, 50);
    private static final BaseColor COLOR_AZUL = new BaseColor(0, 80, 200);
    
    private static final Font FONT_TITLE = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_VERDE);
    private static final Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_SECTION = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, COLOR_VERDE_OSCURO);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private static final Font FONT_ITALIC = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    public static String generarReporteMovimientosMejorado(
            List<MateriaPrima> productos,
            MovimientoInventarioDAO movimientoDAO,
            String rutaSalida) throws Exception {
        
        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        // ===== ENCABEZADO =====
        agregarEncabezado(doc, "HISTORIAL DE MOVIMIENTOS DE INVENTARIO");
        
        // Fecha de generación
        Paragraph fechaGen = new Paragraph("Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), FONT_ITALIC);
        fechaGen.setSpacingBefore(5);
        doc.add(fechaGen);
        doc.add(new Paragraph(" "));

        // ===== VARIABLES PARA ACUMULAR TOTALES GENERALES =====
        double granTotalInvertido = 0;
        double granTotalConsumido = 0;
        double granTotalDisponible = 0;
        int totalIngresosGlobal = 0;
        int totalUsosGlobal = 0;

        // ===== RECORRER CADA PRODUCTO =====
        for (MateriaPrima producto : productos) {
            
            // Obtener TODOS los movimientos de este producto
            List<MovimientoInventario> movimientos = movimientoDAO.listarPorProducto(producto.getId());
            
            if (movimientos.isEmpty()) {
                continue;
            }
            
            // ===== SECCIÓN DEL PRODUCTO =====
            doc.add(crearSeccionProducto(producto));
            
            // ===== TABLA DE MOVIMIENTOS (SIN SACOS Y SUELTOS) =====
            PdfPTable tabla = crearTablaMovimientos(movimientos);
            doc.add(tabla);
            
            // ===== RESUMEN DEL PRODUCTO =====
            double totalInvertido = movimientos.stream()
                .filter(m -> "INGRESO".equals(m.getTipoMovimiento()))
                .mapToDouble(MovimientoInventario::getSubtotal).sum();
            
            double totalConsumido = movimientos.stream()
                .filter(m -> "USO".equals(m.getTipoMovimiento()))
                .mapToDouble(MovimientoInventario::getSubtotal).sum();
            
            int totalIngresos = (int) movimientos.stream()
                .filter(m -> "INGRESO".equals(m.getTipoMovimiento())).count();
            
            int totalUsos = (int) movimientos.stream()
                .filter(m -> "USO".equals(m.getTipoMovimiento())).count();
            
            granTotalInvertido += totalInvertido;
            granTotalConsumido += totalConsumido;
            granTotalDisponible += producto.getCantidadDisponible();
            totalIngresosGlobal += totalIngresos;
            totalUsosGlobal += totalUsos;
            
            doc.add(crearResumenProducto(producto, totalIngresos, totalUsos, totalInvertido, totalConsumido));
            
            doc.add(new Paragraph(" "));
            doc.add(new LineSeparator(0.5f, 100f, COLOR_VERDE_CLARO, Element.ALIGN_CENTER, -5));
            doc.add(new Paragraph(" "));
        }

        // ===== RESUMEN GENERAL =====
        doc.add(crearResumenGeneral(totalIngresosGlobal, totalUsosGlobal, 
            granTotalInvertido, granTotalConsumido, granTotalDisponible));

        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    // ===== SECCIÓN DEL PRODUCTO =====
    private static Paragraph crearSeccionProducto(MateriaPrima producto) {
        Paragraph p = new Paragraph();
        p.setFont(FONT_SECTION);
        p.add("📦 ");
        p.add(producto.getNombre());
        p.add(" (");
        p.add(producto.getCodigo() != null ? producto.getCodigo() : "Sin código");
        p.add(")");
        p.setSpacingBefore(15);
        p.setSpacingAfter(5);
        return p;
    }

    // ===== TABLA DE MOVIMIENTOS (SIN SACOS Y SUELTOS) =====
    private static PdfPTable crearTablaMovimientos(List<MovimientoInventario> movimientos) {
        float[] columnWidths = {0.5f, 1.1f, 0.8f, 0.8f, 1.0f, 0.7f, 1.0f, 1.0f, 2.0f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        
        // Headers - SIN SACOS Y SUELTOS
        String[] headers = {"#", "Fecha", "Hora", "Tipo", "Cantidad", "Unidad", "Precio/Unid.", "Subtotal", "Referencia"};
        for (String h : headers) {
            addHeaderCell(table, h);
        }
        
        boolean alt = false;
        int contador = 1;
        
        for (MovimientoInventario m : movimientos) {
            BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
            
            // Color para el tipo de movimiento
            BaseColor colorTipo = "INGRESO".equals(m.getTipoMovimiento()) ? COLOR_VERDE : COLOR_ROJO;
            Font fontTipo = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, colorTipo);
            
            // Celda de tipo con color
            PdfPCell cellTipo = new PdfPCell(new Phrase(m.getTipoMovimiento(), fontTipo));
            cellTipo.setBackgroundColor(bg);
            cellTipo.setPadding(4);
            cellTipo.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            // Celda de cantidad con signo
            String cantidadStr = "INGRESO".equals(m.getTipoMovimiento()) ? 
                "+" + String.format("%.2f", m.getCantidad()) : 
                "-" + String.format("%.2f", m.getCantidad());
            Font fontCantidad = new Font(Font.FontFamily.HELVETICA, 9, 
                "INGRESO".equals(m.getTipoMovimiento()) ? Font.NORMAL : Font.BOLD, 
                "INGRESO".equals(m.getTipoMovimiento()) ? COLOR_VERDE : COLOR_ROJO);
            
            addDataCell(table, String.valueOf(contador++), bg);
            addDataCell(table, m.getFecha(), bg);
            addDataCell(table, m.getHora(), bg);
            table.addCell(cellTipo);
            
            PdfPCell cellCantidad = new PdfPCell(new Phrase(cantidadStr, fontCantidad));
            cellCantidad.setBackgroundColor(bg);
            cellCantidad.setPadding(4);
            cellCantidad.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellCantidad);
            
            addDataCell(table, m.getUnidad(), bg);
            addDataCell(table, String.format("$%.4f", m.getPrecioPorUnidad()), bg);
            
            String subtotalStr = "INGRESO".equals(m.getTipoMovimiento()) ? 
                "+$" + String.format("%.2f", m.getSubtotal()) : 
                "-$" + String.format("%.2f", m.getSubtotal());
            Font fontSubtotal = new Font(Font.FontFamily.HELVETICA, 9, 
                "INGRESO".equals(m.getTipoMovimiento()) ? Font.NORMAL : Font.BOLD, 
                "INGRESO".equals(m.getTipoMovimiento()) ? COLOR_VERDE : COLOR_ROJO);
            PdfPCell cellSubtotal = new PdfPCell(new Phrase(subtotalStr, fontSubtotal));
            cellSubtotal.setBackgroundColor(bg);
            cellSubtotal.setPadding(4);
            cellSubtotal.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cellSubtotal);
            
            String referencia = m.getReferencia() != null ? m.getReferencia() : "";
            if (referencia.length() > 40) {
                referencia = referencia.substring(0, 37) + "...";
            }
            addDataCell(table, referencia, bg);
            
            alt = !alt;
        }
        
        return table;
    }

    // ===== RESUMEN DEL PRODUCTO =====
    private static PdfPTable crearResumenProducto(MateriaPrima producto, int totalIngresos, int totalUsos, 
                                                   double totalInvertido, double totalConsumido) {
        PdfPTable table = new PdfPTable(new float[]{1.2f, 1.0f, 1.2f, 1.0f, 1.2f, 1.0f, 1.2f, 1.0f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        
        // Fila de resumen
        PdfPCell cell = new PdfPCell(new Phrase("📊 RESUMEN DEL PRODUCTO", FONT_BOLD));
        cell.setColspan(8);
        cell.setBackgroundColor(COLOR_VERDE_CLARO);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        
        addDataCell(table, "Ingresos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalIngresos), BaseColor.WHITE);
        addDataCell(table, "Usos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalUsos), BaseColor.WHITE);
        addDataCell(table, "Stock actual:", BaseColor.WHITE);
        addDataCell(table, String.format("%.2f %s", producto.getCantidadDisponible(), producto.getUnidad()), BaseColor.WHITE);
        addDataCell(table, "Precio promedio:", BaseColor.WHITE);
        addDataCell(table, String.format("$%.4f/%s", producto.getPrecioPorUnidad(), producto.getUnidad()), BaseColor.WHITE);
        
        addDataCell(table, "💰 Total invertido:", new BaseColor(240, 240, 240));
        addDataCell(table, String.format("$%.2f", totalInvertido), new BaseColor(240, 240, 240));
        addDataCell(table, "📉 Total consumido:", new BaseColor(240, 240, 240));
        addDataCell(table, String.format("$%.2f", totalConsumido), new BaseColor(240, 240, 240));
        addDataCell(table, "💰 Valor en stock:", new BaseColor(240, 240, 240));
        double valorStock = producto.getCantidadDisponible() * producto.getPrecioPorUnidad();
        addDataCell(table, String.format("$%.2f", valorStock), new BaseColor(240, 240, 240));
        addDataCell(table, "", new BaseColor(240, 240, 240));
        addDataCell(table, "", new BaseColor(240, 240, 240));
        
        return table;
    }

    // ===== RESUMEN GENERAL =====
    private static PdfPTable crearResumenGeneral(int totalIngresos, int totalUsos, 
                                                  double totalInvertido, double totalConsumido, 
                                                  double totalDisponible) {
        PdfPTable table = new PdfPTable(new float[]{1.5f, 1.0f, 1.5f, 1.0f, 1.5f, 1.0f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        // Título
        PdfPCell cell = new PdfPCell(new Phrase("📊 RESUMEN GENERAL DE TODOS LOS PRODUCTOS", 
            new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_VERDE_OSCURO)));
        cell.setColspan(6);
        cell.setBackgroundColor(new BaseColor(200, 230, 200));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        
        // Datos
        addDataCell(table, "📥 Total de ingresos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalIngresos), BaseColor.WHITE);
        addDataCell(table, "📤 Total de usos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalUsos), BaseColor.WHITE);
        addDataCell(table, "📊 Total movimientos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalIngresos + totalUsos), BaseColor.WHITE);
        
        // Totales con formato especial
        addDataCell(table, "💰 TOTAL INVERTIDO:", new BaseColor(230, 255, 230));
        PdfPCell cellInv = new PdfPCell(new Phrase("$" + String.format("%.2f", totalInvertido), 
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_VERDE_OSCURO)));
        cellInv.setBackgroundColor(new BaseColor(230, 255, 230));
        cellInv.setPadding(5);
        cellInv.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellInv);
        
        addDataCell(table, "📉 TOTAL CONSUMIDO:", new BaseColor(255, 230, 230));
        PdfPCell cellCons = new PdfPCell(new Phrase("$" + String.format("%.2f", totalConsumido), 
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_ROJO)));
        cellCons.setBackgroundColor(new BaseColor(255, 230, 230));
        cellCons.setPadding(5);
        cellCons.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellCons);
        
        addDataCell(table, "📦 STOCK DISPONIBLE:", new BaseColor(230, 240, 255));
        PdfPCell cellStock = new PdfPCell(new Phrase(String.format("%.2f", totalDisponible) + " lb", 
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_AZUL)));
        cellStock.setBackgroundColor(new BaseColor(230, 240, 255));
        cellStock.setPadding(5);
        cellStock.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellStock);
        
        // Balance final
        PdfPCell balanceLabel = new PdfPCell(new Phrase("⚖️ BALANCE FINAL (Invertido - Consumido):", 
            new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(50, 50, 50))));
        balanceLabel.setColspan(3);
        balanceLabel.setBackgroundColor(new BaseColor(255, 255, 200));
        balanceLabel.setPadding(6);
        balanceLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(balanceLabel);
        
        double balance = totalInvertido - totalConsumido;
        BaseColor colorBalance = balance >= 0 ? COLOR_VERDE_OSCURO : COLOR_ROJO;
        PdfPCell balanceVal = new PdfPCell(new Phrase("$" + String.format("%.2f", balance), 
            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, colorBalance)));
        balanceVal.setColspan(3);
        balanceVal.setBackgroundColor(new BaseColor(255, 255, 200));
        balanceVal.setPadding(6);
        balanceVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(balanceVal);
        
        return table;
    }

    // ===== ENCABEZADO =====
    private static void agregarEncabezado(Document doc, String titulo) throws DocumentException {
        Paragraph t = new Paragraph("🌿 FEED FORMULATOR PRO", FONT_TITLE);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);
        
        Paragraph sub = new Paragraph(titulo, new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_NARANJA));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        doc.add(sub);
        
        doc.add(new LineSeparator(1f, 100f, COLOR_VERDE, Element.ALIGN_CENTER, -5));
    }

    // ===== PIE DE PÁGINA =====
    private static void agregarPiePagina(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
        doc.add(new LineSeparator(0.5f, 100f, COLOR_VERDE, Element.ALIGN_CENTER, -5));
        Paragraph pie = new Paragraph("Feed Formulator Pro | Sistema de Gestión de Alimentos Balanceados | Historial de Movimientos", 
            new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(5);
        doc.add(pie);
    }

    // ===== MÉTODOS AUXILIARES =====
    private static void addHeaderCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_HEADER));
        c.setBackgroundColor(COLOR_VERDE);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private static void addDataCell(PdfPTable t, String text, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_NORMAL));
        c.setBackgroundColor(bg);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }
}