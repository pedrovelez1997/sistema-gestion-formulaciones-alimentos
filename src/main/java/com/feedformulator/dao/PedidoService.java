package com.feedformulator.dao;

import com.feedformulator.model.DetallePedido;
import com.feedformulator.model.MateriaPrima;

/**
 * Servicio de cálculos de negocio para pedidos.
 * Centraliza la lógica que antes estaba duplicada en ProveedoresPanel y PedidosPanel.
 */
public class PedidoService {

    /**
     * Calcula la cantidad total de un ítem del pedido.
     * Fórmula: (numEnvases × pesoEnvase) + sueltosAdicionales
     */
    public static double calcularCantidadTotal(int numEnvases, double pesoEnvase, double sueltos) {
        return (numEnvases * pesoEnvase) + sueltos;
    }

    /**
     * Calcula el precio por unidad (lb o litro).
     * Si pesoEnvase es 0, retorna 0 para evitar división por cero.
     */
    public static double calcularPrecioUnitario(double precioEnvase, double pesoEnvase) {
        return pesoEnvase > 0 ? precioEnvase / pesoEnvase : 0;
    }

    /**
     * Calcula el subtotal de un ítem.
     * Fórmula: numEnvases × precioEnvase (precio del lote completo).
     */
    public static double calcularSubtotal(int numEnvases, double precioEnvase) {
        return numEnvases * precioEnvase;
    }

    /**
     * Construye un DetallePedido con todos los campos calculados.
     * Elimina la necesidad de repetir el cálculo en cada panel.
     *
     * @param mp           materia prima a la que pertenece el detalle
     * @param numEnvases   cantidad de sacos/bidones
     * @param precioEnvase precio por saco/bidón
     * @param pesoEnvase   peso o volumen por saco/bidón
     * @param sueltos      cantidad adicional suelta (libras o litros sin envasar)
     */
    public static DetallePedido construirDetalle(MateriaPrima mp,
                                                  int numEnvases,
                                                  double precioEnvase,
                                                  double pesoEnvase,
                                                  double sueltos) {
        double cantidadTotal  = calcularCantidadTotal(numEnvases, pesoEnvase, sueltos);
        double precioUnitario = calcularPrecioUnitario(precioEnvase, pesoEnvase);
        double subtotal       = calcularSubtotal(numEnvases, precioEnvase);

        DetallePedido detalle = new DetallePedido();
        detalle.setMateriaPrimaId(mp.getId());
        detalle.setNombreProducto(mp.getNombre());
        detalle.setUnidad(mp.getUnidad());
        detalle.setCantidad(cantidadTotal);
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setSubtotal(subtotal);
        detalle.setNumeroSacos(numEnvases);
        detalle.setPesoSaco(pesoEnvase);
        detalle.setLibrasSueltas(sueltos);
        return detalle;
    }

    /**
     * Valida que los valores numéricos de un ítem son coherentes antes de agregarlo.
     * Lanza IllegalArgumentException con un mensaje descriptivo si hay problema.
     */
    public static void validarItem(String nombreProducto, int numEnvases,
                                    double precioEnvase, double pesoEnvase, double sueltos) {
        if (nombreProducto == null || nombreProducto.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un producto.");
        }
        if (numEnvases < 0) {
            throw new IllegalArgumentException("La cantidad de envases no puede ser negativa.");
        }
        if (precioEnvase < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (pesoEnvase < 0) {
            throw new IllegalArgumentException("El peso/volumen no puede ser negativo.");
        }
        if (sueltos < 0) {
            throw new IllegalArgumentException("Las unidades sueltas no pueden ser negativas.");
        }
        if (numEnvases == 0 && sueltos == 0) {
            throw new IllegalArgumentException("Debe ingresar al menos una unidad (envases o sueltos).");
        }
        if (pesoEnvase == 0 && numEnvases > 0) {
            throw new IllegalArgumentException("El peso/volumen por envase no puede ser 0 si hay envases.");
        }
    }
}