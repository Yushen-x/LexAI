SELECT 'before_legacy_tasks' AS section, COUNT(*) AS n
FROM workspace_tasks
WHERE type IN ('LEGAL_CONSULTATION', 'CASE_ANALYSIS', 'CONTRACT_DRAFT');

SELECT 'before_public_verify_contracts' AS section, COUNT(*) AS n
FROM contracts
WHERE source IN ('PUBLIC_VERIFY', 'AI_DRAFT');

DELETE FROM workspace_tasks
WHERE type IN ('LEGAL_CONSULTATION', 'CASE_ANALYSIS', 'CONTRACT_DRAFT');

DELETE FROM contracts
WHERE source IN ('PUBLIC_VERIFY', 'AI_DRAFT');

SELECT 'after_legacy_tasks' AS section, COUNT(*) AS n
FROM workspace_tasks
WHERE type IN ('LEGAL_CONSULTATION', 'CASE_ANALYSIS', 'CONTRACT_DRAFT');

SELECT 'after_public_verify_contracts' AS section, COUNT(*) AS n
FROM contracts
WHERE source IN ('PUBLIC_VERIFY', 'AI_DRAFT');

SELECT 'final_tasks_total' AS section, COUNT(*) AS n FROM workspace_tasks;
SELECT 'final_contracts_total' AS section, COUNT(*) AS n FROM contracts;

SELECT 'tasks_by_type_status' AS section;
SELECT type, status, COUNT(*) AS n
FROM workspace_tasks
GROUP BY type, status
ORDER BY type, status;
