package com.healthcare.clinic.backoffice.inventory.service;

import com.healthcare.clinic.backoffice.inventory.entity.*;
import com.healthcare.clinic.backoffice.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BackofficeInventoryService {

    private final WarehouseRepository warehouseRepository;
    private final StockItemRepository stockItemRepository;
    private final BackofficeSupplierRepository supplierRepository;
    private final BackofficePurchaseOrderRepository purchaseOrderRepository;
    private final StockTransferRepository stockTransferRepository;

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public List<StockItem> getAllStockItems() {
        return stockItemRepository.findAll();
    }

    @Transactional
    public StockItem saveStockItem(StockItem item) {
        return stockItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<BackofficeSupplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional
    public BackofficeSupplier createSupplier(BackofficeSupplier supplier) {
        return supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<BackofficePurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByOrderDateDesc();
    }

    @Transactional
    public BackofficePurchaseOrder createPurchaseOrder(BackofficePurchaseOrder po) {
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public BackofficePurchaseOrder updatePoStatus(Long poId, String status) {
        BackofficePurchaseOrder po = purchaseOrderRepository.findById(poId).orElseThrow();
        po.setStatus(status);
        return purchaseOrderRepository.save(po);
    }

    @Transactional(readOnly = true)
    public List<StockTransfer> getAllStockTransfers() {
        return stockTransferRepository.findAllByOrderByTransferredAtDesc();
    }

    @Transactional
    public StockTransfer createStockTransfer(StockTransfer transfer, Long userId) {
        transfer.setTransferredBy(userId);
        if (transfer.getTransferredAt() == null) {
            transfer.setTransferredAt(java.time.ZonedDateTime.now());
        }
        return stockTransferRepository.save(transfer);
    }
}
