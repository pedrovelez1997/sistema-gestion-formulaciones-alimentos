package com.feedformulator.report;

import com.feedformulator.model.DetalleFormulacion;
import com.feedformulator.model.Formulacion;
import com.feedformulator.model.MateriaPrima;
import com.feedformulator.model.Venta;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReporteGenerator {
    private static final BaseColor COLOR_VERDE = new BaseColor(34, 139, 34);
    private static final BaseColor COLOR_VERDE_CLARO = new BaseColor(144, 238, 144);
    private static final BaseColor COLOR_GRIS = new BaseColor(245, 245, 245);
    private static final Font FONT_TITLE = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_VERDE);
    private static final Font FONT_HEADER = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
    private static final Font FONT_SECTION = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_VERDE);
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font FONT_BOLD = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

    // ========== REPORTE DE FORMULACIÓN ==========
    public static String generarReporteFormulacion(Formulacion f, String rutaSalida) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        agregarEncabezado(doc, "REPORTE DE FORMULACIÓN");

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        addInfoCell(infoTable, "Producto:", f.getNombre());
        addInfoCell(infoTable, "Fecha:", f.getFecha() + "  " + f.getHora());
        addInfoCell(infoTable, "Total preparado:", String.format("%.2f lb", f.getTotalLibras()));
        addInfoCell(infoTable, "Costo total:", String.format("$%.2f", f.getCostoTotal()));
        doc.add(infoTable);

        doc.add(new Paragraph(" "));

        Paragraph seccion = new Paragraph("INGREDIENTES UTILIZADOS", FONT_SECTION);
        seccion.setSpacingBefore(10);
        doc.add(seccion);

        PdfPTable tabla = new PdfPTable(new float[]{3f, 1.5f, 1.5f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);
        addHeaderCell(tabla, "Ingrediente");
        addHeaderCell(tabla, "Cantidad");
        addHeaderCell(tabla, "Unidad");
        addHeaderCell(tabla, "Precio/Unidad");
        addHeaderCell(tabla, "Subtotal");

        boolean alt = false;
        for (DetalleFormulacion d : f.getDetalles()) {
            BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
            addDataCell(tabla, d.getNombreIngrediente(), bg);
            addDataCell(tabla, String.format("%.2f", d.getCantidadUsada()), bg);
            addDataCell(tabla, d.getUnidad(), bg);
            addDataCell(tabla, String.format("$%.4f", d.getPrecioUnitario()), bg);
            addDataCell(tabla, String.format("$%.2f", d.getSubtotal()), bg);
            alt = !alt;
        }
        
        PdfPCell totalLabel = new PdfPCell(new Phrase("COSTO TOTAL DE FORMULACIÓN", FONT_BOLD));
        totalLabel.setColspan(4);
        totalLabel.setBackgroundColor(COLOR_VERDE_CLARO);
        totalLabel.setPadding(5);
        totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(totalLabel);
        
        PdfPCell totalVal = new PdfPCell(new Phrase(String.format("$%.2f", f.getCostoTotal()), FONT_BOLD));
        totalVal.setBackgroundColor(COLOR_VERDE_CLARO);
        totalVal.setPadding(5);
        totalVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(totalVal);

        doc.add(tabla);
        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    // ========== REPORTE DE STOCK ==========
    public static String generarReporteStock(List<MateriaPrima> materias, String rutaSalida) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        agregarEncabezado(doc, "REPORTE DE INVENTARIO - STOCK ACTUAL");

        Paragraph fecha = new Paragraph("Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), FONT_NORMAL);
        fecha.setSpacingBefore(5);
        doc.add(fecha);
        doc.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[]{2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f});
        tabla.setWidthPercentage(100);
        addHeaderCell(tabla, "Código");
        addHeaderCell(tabla, "Materia Prima");
        addHeaderCell(tabla, "Unidad");
        addHeaderCell(tabla, "Total");
        addHeaderCell(tabla, "Disponible");
        addHeaderCell(tabla, "Precio Total");

        boolean alt = false;
        double totalInversion = 0;
        for (MateriaPrima mp : materias) {
            BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
            addDataCell(tabla, mp.getCodigo() != null ? mp.getCodigo() : "---", bg);
            addDataCell(tabla, mp.getNombre(), bg);
            addDataCell(tabla, mp.getUnidad(), bg);
            addDataCell(tabla, String.format("%.2f", mp.getCantidadTotal()), bg);
            addDataCell(tabla, String.format("%.2f", mp.getCantidadDisponible()), bg);
            addDataCell(tabla, String.format("$%.2f", mp.getPrecioTotal()), bg);
            totalInversion += mp.getPrecioTotal();
            alt = !alt;
        }

        PdfPCell lbl = new PdfPCell(new Phrase("INVERSIÓN TOTAL EN MATERIAS PRIMAS", FONT_BOLD));
        lbl.setColspan(5);
        lbl.setBackgroundColor(COLOR_VERDE_CLARO);
        lbl.setPadding(5);
        lbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(lbl);
        
        PdfPCell val = new PdfPCell(new Phrase(String.format("$%.2f", totalInversion), FONT_BOLD));
        val.setBackgroundColor(COLOR_VERDE_CLARO);
        val.setPadding(5);
        val.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(val);

        doc.add(tabla);
        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    // ========== REPORTE DE VENTAS ==========
    public static String generarReporteVentas(List<Venta> ventas, String rutaSalida) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        agregarEncabezado(doc, "REPORTE DE VENTAS");

        PdfPTable tabla = new PdfPTable(new float[]{2.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2f, 2f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        addHeaderCell(tabla, "Producto");
        addHeaderCell(tabla, "Libras");
        addHeaderCell(tabla, "Inversión");
        addHeaderCell(tabla, "Precio Venta");
        addHeaderCell(tabla, "Ganancia");
        addHeaderCell(tabla, "Fecha");
        addHeaderCell(tabla, "Hora");

        boolean alt = false;
        double totalInv = 0, totalVenta = 0, totalGan = 0;
        for (Venta v : ventas) {
            BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
            addDataCell(tabla, v.getNombreProducto(), bg);
            addDataCell(tabla, String.format("%.2f lb", v.getLibrasVendidas()), bg);
            addDataCell(tabla, String.format("$%.2f", v.getCostoInversion()), bg);
            addDataCell(tabla, String.format("$%.2f", v.getPrecioVenta()), bg);
            
            BaseColor ganBg = v.getGanancia() >= 0 ? bg : new BaseColor(255, 200, 200);
            PdfPCell ganCell = new PdfPCell(new Phrase(String.format("$%.2f", v.getGanancia()), 
                v.getGanancia() >= 0 ? FONT_NORMAL : new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.RED)));
            ganCell.setBackgroundColor(ganBg);
            ganCell.setPadding(4);
            ganCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabla.addCell(ganCell);
            
            addDataCell(tabla, v.getFecha(), bg);
            addDataCell(tabla, v.getHora(), bg);
            totalInv += v.getCostoInversion();
            totalVenta += v.getPrecioVenta();
            totalGan += v.getGanancia();
            alt = !alt;
        }

        addSumCell(tabla, "TOTALES");
        addSumCell(tabla, "");
        addSumCell(tabla, String.format("$%.2f", totalInv));
        addSumCell(tabla, String.format("$%.2f", totalVenta));
        addSumCell(tabla, String.format("$%.2f", totalGan));
        addSumCell(tabla, "");
        addSumCell(tabla, "");

        doc.add(tabla);
        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    // ========== REPORTE ANALÍTICO COMPLETO (usado por ReportesPanel) ==========
    public static String generarReporteAnalitico(
            String inicioStr, String finStr,
            String totalVentasTxt, String ventasCreditoTxt, String gananciaTxt,
            String promedioTxt, String diasTxt,
            List<Object[]> filasTopProductos,
            List<Object[]> filasTopMateriales,
            java.awt.image.BufferedImage imgBarras, java.awt.image.BufferedImage imgLinea,
            List<String> lineasComparativa,
            String rutaSalida) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
        doc.open();

        agregarEncabezado(doc, "REPORTE DE VENTAS - ANÁLISIS DEL PERÍODO");

        Paragraph rango = new Paragraph("Período: " + inicioStr + "  a  " + finStr, FONT_NORMAL);
        rango.setAlignment(Element.ALIGN_CENTER);
        rango.setSpacingBefore(6);
        doc.add(rango);
        doc.add(new Paragraph(" "));

        // ---- Resumen general ----
        Paragraph seccionResumen = new Paragraph("RESUMEN GENERAL", FONT_SECTION);
        seccionResumen.setSpacingBefore(6);
        doc.add(seccionResumen);

        PdfPTable resumenTabla = new PdfPTable(5);
        resumenTabla.setWidthPercentage(100);
        resumenTabla.setSpacingBefore(6);
        addResumenCell(resumenTabla, "Total Ventas", totalVentasTxt);
        addResumenCell(resumenTabla, "Ventas a Crédito", ventasCreditoTxt);
        addResumenCell(resumenTabla, "Ganancia Neta", gananciaTxt);
        addResumenCell(resumenTabla, "Promedio Diario", promedioTxt);
        addResumenCell(resumenTabla, "Días", diasTxt);
        doc.add(resumenTabla);

        // ---- Gráfico de barras ----
        if (imgBarras != null) {
            Paragraph sGraf = new Paragraph("VENTAS DIARIAS", FONT_SECTION);
            sGraf.setSpacingBefore(14);
            doc.add(sGraf);
            Image imagenBarras = Image.getInstance(imgBarras, null);
            imagenBarras.scaleToFit(500, 260);
            imagenBarras.setSpacingBefore(6);
            doc.add(imagenBarras);
        }

        // ---- Gráfico de línea ----
        if (imgLinea != null) {
            Paragraph sTend = new Paragraph("TENDENCIA", FONT_SECTION);
            sTend.setSpacingBefore(14);
            doc.add(sTend);
            Image imagenLinea = Image.getInstance(imgLinea, null);
            imagenLinea.scaleToFit(500, 230);
            imagenLinea.setSpacingBefore(6);
            doc.add(imagenLinea);
        }

        doc.newPage();

        // ---- Top productos ----
        Paragraph sTop = new Paragraph("PRODUCTOS MÁS VENDIDOS", FONT_SECTION);
        doc.add(sTop);

        if (filasTopProductos == null || filasTopProductos.isEmpty()) {
            Paragraph vacio = new Paragraph("No hay ventas registradas en este período.", FONT_NORMAL);
            vacio.setSpacingBefore(6);
            doc.add(vacio);
        } else {
            PdfPTable tablaTop = new PdfPTable(new float[]{0.7f, 2.5f, 1.2f, 1.5f, 1.5f, 1.3f});
            tablaTop.setWidthPercentage(100);
            tablaTop.setSpacingBefore(6);
            addHeaderCell(tablaTop, "#");
            addHeaderCell(tablaTop, "Producto");
            addHeaderCell(tablaTop, "Ventas");
            addHeaderCell(tablaTop, "Ingresos");
            addHeaderCell(tablaTop, "Ganancia");
            addHeaderCell(tablaTop, "% del Total");

            boolean alt = false;
            for (Object[] fila : filasTopProductos) {
                BaseColor bg = alt ? COLOR_GRIS : BaseColor.WHITE;
                for (Object valor : fila) {
                    addDataCell(tablaTop, String.valueOf(valor), bg);
                }
                alt = !alt;
            }
            doc.add(tablaTop);
        }

        // ---- Materiales más usados ----
        doc.add(new Paragraph(" "));
        Paragraph sMat = new Paragraph("MATERIALES MÁS USADOS", FONT_SECTION);
        sMat.setSpacingBefore(14);
        doc.add(sMat);

        if (filasTopMateriales == null || filasTopMateriales.isEmpty()) {
            Paragraph vacioMat = new Paragraph("No hay consumo de materiales registrado en este período.", FONT_NORMAL);
            vacioMat.setSpacingBefore(6);
            doc.add(vacioMat);
        } else {
            PdfPTable tablaMat = new PdfPTable(new float[]{0.7f, 2.5f, 1.8f, 1.5f, 1.8f});
            tablaMat.setWidthPercentage(100);
            tablaMat.setSpacingBefore(6);
            addHeaderCell(tablaMat, "#");
            addHeaderCell(tablaMat, "Material");
            addHeaderCell(tablaMat, "Cantidad Usada");
            addHeaderCell(tablaMat, "Costo Total");
            addHeaderCell(tablaMat, "% del Costo");

            boolean altMat = false;
            for (Object[] fila : filasTopMateriales) {
                BaseColor bg = altMat ? COLOR_GRIS : BaseColor.WHITE;
                for (Object valor : fila) {
                    addDataCell(tablaMat, String.valueOf(valor), bg);
                }
                altMat = !altMat;
            }
            doc.add(tablaMat);
        }

        // ---- Comparativa con período anterior ----
        if (lineasComparativa != null && !lineasComparativa.isEmpty()) {
            doc.add(new Paragraph(" "));
            Paragraph sComp = new Paragraph("COMPARATIVA CON EL PERÍODO ANTERIOR", FONT_SECTION);
            sComp.setSpacingBefore(14);
            doc.add(sComp);
            for (String linea : lineasComparativa) {
                Paragraph p = new Paragraph(linea, FONT_NORMAL);
                p.setSpacingBefore(4);
                doc.add(p);
            }
        }

        agregarPiePagina(doc);
        doc.close();
        return rutaSalida;
    }

    private static void addResumenCell(PdfPTable t, String label, String valor) {
        PdfPCell c = new PdfPCell();
        c.setPadding(6);
        c.setBackgroundColor(COLOR_GRIS);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.GRAY)));
        p.add(new Chunk(valor, FONT_BOLD));
        c.addElement(p);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    // ========== MÉTODOS AUXILIARES ==========
    
    private static void agregarEncabezado(Document doc, String titulo) throws DocumentException {
        Paragraph t = new Paragraph("🌿 FEED FORMULATOR PRO", FONT_TITLE);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);
        
        Paragraph sub = new Paragraph(titulo, new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        doc.add(sub);
        
        doc.add(new LineSeparator(1f, 100f, COLOR_VERDE, Element.ALIGN_CENTER, -5));
    }

    private static void agregarPiePagina(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
        doc.add(new LineSeparator(0.5f, 100f, COLOR_VERDE, Element.ALIGN_CENTER, -5));
        Paragraph pie = new Paragraph("Feed Formulator Pro  |  Sistema de Gestión de Alimentos Balanceados", 
            new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(5);
        doc.add(pie);
    }

    private static void addHeaderCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_HEADER));
        c.setBackgroundColor(COLOR_VERDE);
        c.setPadding(6);
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

    private static void addSumCell(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FONT_BOLD));
        c.setBackgroundColor(COLOR_VERDE_CLARO);
        c.setPadding(5);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private static void addInfoCell(PdfPTable t, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, FONT_BOLD));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(3);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, FONT_NORMAL));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);
        t.addCell(v);
    }
}