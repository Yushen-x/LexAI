UPDATE contracts SET status='SIGNED' WHERE id=2;
UPDATE contracts SET status='DRAFT' WHERE id=3;
SELECT id, contract_no, status FROM contracts WHERE id IN (2,3);
