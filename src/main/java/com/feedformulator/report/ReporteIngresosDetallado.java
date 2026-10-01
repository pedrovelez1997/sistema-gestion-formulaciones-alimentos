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
import java.util.ArrayList;

public class ReporteIngresosDetallado {
    private static final BaseColor COLOR_VERDE = new BaseColor(34, 139, 34);
    private static final BaseColor COLOR_VERDE_CLARO = new BaseColor(144, 238, 144);
    private static final BaseColor COLOR_GRIS = new BaseColor(245, 245, 245);
    private static final BaseColor COLOR_VERDE_OSCURO = new BaseColor(20, 100, 20);
    private static final BaseColor COLOR_NARANJA = new BaseColor(200, 100, 20);
    
    private static final Font FONT_TITLE = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_VERDE);
    private static final Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_SECTION = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_VERDE_OSCURO);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private static final Font FONT_ITALIC = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    public static String generarReporteIngresosDetallado(
            List<MateriaPrima> productos,
            MovimientoInventarioDAO movimientoDAO,
            String rutaSalida) throws Exception {
        
        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        agregarEncabezado(doc, "REPORTE DE INGRESOS DETALLADO");
        
        Paragraph fechaGen = new Paragraph("Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), FONT_ITALIC);
        fechaGen.setSpacingBefore(5);
        doc.add(fechaGen);
        doc.add(new Paragraph(" "));

        double granTotalInversion = 0;
        int totalRegistros = 0;

        for (MateriaPrima producto : productos) {
            
            List<MovimientoInventario> movimientos = movimientoDAO.listarPorProducto(producto.getId());
            
            // Filtrar solo INGRESOS
            List<MovimientoInventario> ingresos = new ArrayList<>();
            for (MovimientoInventario m : movimientos) {
                if ("INGRESO".equals(m.getTipoMovimiento())) {
                    ingresos.add(m);
                }
            }
            
            if (ingresos.isEmpty()) {
                continue;
            }
            
            doc.add(crearSeccionProducto(producto));
            
            PdfPTable tabla = crearTablaIngresos(ingresos);
            doc.add(tabla);
            
            double totalProducto = ingresos.stream().mapToDouble(MovimientoInventario::getSubtotal).sum();
            double cantidadProducto = ingresos.stream().mapToDouble(MovimientoInventario::getCantidad).sum();
            granTotalInversion += totalProducto;
            totalRegistros += ingresos.size();
            
            doc.add(crearResumenProducto(producto, ingresos.size(), cantidadProducto, totalProducto));
            
            doc.add(new Paragraph(" "));
            doc.add(new LineSeparator(0.5f, 100f, COLOR_VERDE_CLARO, Element.ALIGN_CENTER, -5));
            doc.add(new Paragraph(" "));
        }

        doc.add(crearGranTotal(totalRegistros, granTotalInversion));

        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    private static Paragraph crearSeccionProducto(MateriaPrima producto) {
        Paragraph p = new Paragraph();
        p.setFont(FONT_SECTION);
        p.add("📦 ");
        p.add(producto.getNombre());
        p.add(" (Código: ");
        p.add(producto.getCodigo() != null ? producto.getCodigo() : "Sin código");
        p.add(")");
        p.setSpacingBefore(15);
        p.setSpacingAfter(5);
        return p;
    }

    private static PdfPTable crearTablaIngresos(List<MovimientoInventario> ingresos) {
        float[] columnWidths = {0.6f, 1.2f, 0.9f, 1.0f, 0.8f, 1.0f, 1.0f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        
        String[] headers = {"#", "Fecha", "Hora", "Cantidad", "Unidad", "Precio/Unid.", "Subtotal"};
        for (String h : headers) {
            addHeaderCell(table, h);
        }
        
        boolean alt = false;
        int contador = 1;
        
        for (MovimientoInventario m : ingresos) {
            BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
            
            addDataCell(table, String.valueOf(contador++), bg);
            addDataCell(table, m.getFecha(), bg);
            addDataCell(table, m.getHora(), bg);
            addDataCell(table, String.format("%.2f", m.getCantidad()), bg);
            addDataCell(table, m.getUnidad(), bg);
            addDataCell(table, String.format("$%.4f", m.getPrecioPorUnidad()), bg);
            addDataCell(table, String.format("$%.2f", m.getSubtotal()), bg);
            
            alt = !alt;
        }
        
        return table;
    }

    private static PdfPTable crearResumenProducto(MateriaPrima producto, int totalIngresos, double cantidadTotal, double inversionTotal) {
        PdfPTable table = new PdfPTable(new float[]{1.5f, 1.0f, 1.5f, 1.0f, 1.5f, 1.0f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        
        PdfPCell cell = new PdfPCell(new Phrase("RESUMEN DEL PRODUCTO", FONT_BOLD));
        cell.setColspan(6);
        cell.setBackgroundColor(COLOR_VERDE_CLARO);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        
        addDataCell(table, "Total ingresos:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalIngresos), BaseColor.WHITE);
        addDataCell(table, "Cantidad total:", BaseColor.WHITE);
        addDataCell(table, String.format("%.2f %s", cantidadTotal, producto.getUnidad()), BaseColor.WHITE);
        addDataCell(table, "Inversión total:", BaseColor.WHITE);
        addDataCell(table, String.format("$%.2f", inversionTotal), BaseColor.WHITE);
        
        return table;
    }

    private static PdfPTable crearGranTotal(int totalRegistros, double granTotal) {
        PdfPTable table = new PdfPTable(new float[]{2f, 1f, 2f, 1f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        PdfPCell cell = new PdfPCell(new Phrase("📊 GRAN TOTAL GENERAL", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_VERDE_OSCURO)));
        cell.setColspan(4);
        cell.setBackgroundColor(new BaseColor(200, 230, 200));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        
        addDataCell(table, "Total de registros de ingreso:", BaseColor.WHITE);
        addDataCell(table, String.valueOf(totalRegistros), BaseColor.WHITE);
        addDataCell(table, "INVERSIÓN TOTAL EN MATERIAS PRIMAS:", BaseColor.WHITE);
        
        PdfPCell totalCell = new PdfPCell(new Phrase("$" + String.format("%.2f", granTotal), 
            new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_VERDE_OSCURO)));
        totalCell.setBackgroundColor(new BaseColor(220, 255, 220));
        totalCell.setPadding(6);
        totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(totalCell);
        
        return table;
    }

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

    private static void agregarPiePagina(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
        doc.add(new LineSeparator(0.5f, 100f, COLOR_VERDE, Element.ALIGN_CENTER, -5));
        Paragraph pie = new Paragraph("Feed Formulator Pro | Sistema de Gestión de Alimentos Balanceados | Reporte de Ingresos Detallado", 
            new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(5);
        doc.add(pie);
    }

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