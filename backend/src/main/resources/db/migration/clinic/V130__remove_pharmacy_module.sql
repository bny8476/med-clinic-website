-- V130__remove_pharmacy_module.sql
-- Complete removal of Pharmacy-only database objects and constraints

-- Drop foreign keys referencing pharmacy tables from prescriptions or other tables safely
ALTER TABLE IF EXISTS prescriptions DROP CONSTRAINT IF EXISTS fk_prescriptions_assigned_pharmacy_user;

-- Drop Pharmacy tables in safe cascade order
DROP TABLE IF EXISTS pharmacy_prescription_items CASCADE;
DROP TABLE IF EXISTS pharmacy_prescriptions CASCADE;
DROP TABLE IF EXISTS prescription_dispensed_items CASCADE;
DROP TABLE IF EXISTS prescriptions_dispensed CASCADE;

DROP TABLE IF EXISTS pharmacy_goods_receipt_note_items CASCADE;
DROP TABLE IF EXISTS pharmacy_goods_receipt_notes CASCADE;

DROP TABLE IF EXISTS pharmacy_supplier_invoice_items CASCADE;
DROP TABLE IF EXISTS pharmacy_supplier_invoices CASCADE;

DROP TABLE IF EXISTS pharmacy_supplier_performance CASCADE;

DROP TABLE IF EXISTS pharmacy_purchase_order_items CASCADE;
DROP TABLE IF EXISTS pharmacy_po_line_items CASCADE;
DROP TABLE IF EXISTS pharmacy_purchase_orders CASCADE;

DROP TABLE IF EXISTS pharmacy_return_to_supplier_items CASCADE;
DROP TABLE IF EXISTS pharmacy_return_to_suppliers CASCADE;
DROP TABLE IF EXISTS pharmacy_batch_return_to_supplier CASCADE;

DROP TABLE IF EXISTS pharmacy_ward_replacement_request_items CASCADE;
DROP TABLE IF EXISTS pharmacy_ward_replacement_requests CASCADE;

DROP TABLE IF EXISTS pharmacy_ward_replacement_return_items CASCADE;
DROP TABLE IF EXISTS pharmacy_ward_replacement_returns CASCADE;

DROP TABLE IF EXISTS pharmacy_insurance_claim_line_items CASCADE;
DROP TABLE IF EXISTS pharmacy_insurance_claims CASCADE;
DROP TABLE IF EXISTS pharmacy_insurance_medicine_coverage CASCADE;
DROP TABLE IF EXISTS pharmacy_insurance_providers CASCADE;

DROP TABLE IF EXISTS pharmacy_medicine_stocks CASCADE;
DROP TABLE IF EXISTS medicine_batches CASCADE;
DROP TABLE IF EXISTS pharmacy_stock_batches CASCADE;
DROP TABLE IF EXISTS pharmacy_stock_adjustments CASCADE;
DROP TABLE IF EXISTS pharmacy_medicines CASCADE;

DROP TABLE IF EXISTS inventory_movements CASCADE;
DROP TABLE IF EXISTS pharmacy_stock_alerts CASCADE;
DROP TABLE IF EXISTS pharmacy_storage_units CASCADE;
DROP TABLE IF EXISTS pharmacy_barcode_scan_logs CASCADE;
DROP TABLE IF EXISTS pharmacy_temperature_logs CASCADE;

DROP TABLE IF EXISTS pharmacy_drug_interaction_checks CASCADE;
DROP TABLE IF EXISTS pharmacy_drug_interactions CASCADE;

DROP TABLE IF EXISTS pharmacy_narcotic_monthly_reconciliation CASCADE;
DROP TABLE IF EXISTS pharmacy_narcotic_register CASCADE;
DROP TABLE IF EXISTS controlled_substance_register CASCADE;

DROP TABLE IF EXISTS pharmacy_pharmacy_advances CASCADE;
DROP TABLE IF EXISTS pharmacy_clearances CASCADE;
DROP TABLE IF EXISTS pharmacy_report_schedules CASCADE;
DROP TABLE IF EXISTS pharmacy_activity_logs CASCADE;
DROP TABLE IF EXISTS pharmacy_outbox_events CASCADE;
DROP TABLE IF EXISTS pharmacy_doctors CASCADE;
DROP TABLE IF EXISTS pharmacy_patients CASCADE;

DROP TABLE IF EXISTS pharmacy_user_roles CASCADE;
DROP TABLE IF EXISTS pharmacy_users CASCADE;
DROP TABLE IF EXISTS pharmacy_roles CASCADE;
