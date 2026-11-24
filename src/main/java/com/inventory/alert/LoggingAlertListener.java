package com.inventory.alert;

import com.inventory.model.Product;
import java.util.logging.Logger;

public class LoggingAlertListener implements StockAlertListener {
    private static final Logger LOG = Logger.getLogger(LoggingAlertListener.class.getName());

    @Override public void onLowStock(Product p) {
        LOG.warning(() -> "LOW STOCK: " + p.sku() + " (" + p.name() + ") qty=" + p.quantity()
                + " reorderLevel=" + p.reorderLevel());
    }
}
