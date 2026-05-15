MERGE INTO contracts
    (contract_no, name, contract_type, party_a, party_b, amount, status, source, created_at, updated_at, deleted)
KEY(contract_no) VALUES
    ('LX-2026-001', '2026年度云服务框架采购合作协议', '采购合同', '科创甲公司', '云端乙科技', 1550000.00, 'UNDER_REVIEW', '演示数据导入', TIMESTAMP '2026-03-28 10:24:00', TIMESTAMP '2026-03-28 10:24:00', FALSE),
    ('LX-2026-002', '新办公园区弱电工程施工合同', '工程合同', '园区建设方', '弱电工程公司', 890000.00, 'SIGNED', '演示数据导入', TIMESTAMP '2026-03-27 15:30:00', TIMESTAMP '2026-03-27 15:30:00', FALSE),
    ('LX-2026-003', '测试用人事外包补充协议', '服务合同', '甲方人力', '外包服务商', 50000.00, 'DRAFT', '演示数据导入', TIMESTAMP '2026-03-12 09:00:00', TIMESTAMP '2026-03-12 09:00:00', FALSE),
    ('LX-2026-004', '高管竞业限制与保密期权协议', '保密/期权', '甲方集团', '高管员工', 0.00, 'UNDER_REVIEW', '演示数据导入', TIMESTAMP '2026-03-10 14:00:00', TIMESTAMP '2026-03-10 16:00:00', FALSE),
    ('LX-2026-005', '核心机房第二季度软硬件代维合同', '运维服务', '甲方数据中心', '代维厂商', 245000.00, 'IN_PROGRESS', '演示数据导入', TIMESTAMP '2026-03-01 11:00:00', TIMESTAMP '2026-03-20 11:00:00', FALSE),
    ('LX-2026-006', '年度法律咨询服务框架协议', '服务合同', '甲方实业', '律师事务所', 120000.00, 'COMPLETED', '演示数据导入', TIMESTAMP '2026-01-15 10:00:00', TIMESTAMP '2026-02-28 17:00:00', FALSE),
    ('LX-2026-007', '仓储场地租赁合同（已终止示例）', '租赁合同', '甲方物流', '乙方物业', 360000.00, 'TERMINATED', '演示数据导入', TIMESTAMP '2025-12-01 09:00:00', TIMESTAMP '2026-03-01 09:00:00', FALSE),
    ('LX-2026-008', '软件定制开发合同', '技术开发', '甲方互联网', '乙方软件工作室', 480000.00, 'SIGNED', '演示数据导入', TIMESTAMP '2026-03-25 18:00:00', TIMESTAMP '2026-03-26 10:00:00', FALSE);

MERGE INTO workspace_tasks
    (task_no, title, type, related_id, initiator, status, created_at)
KEY(task_no) VALUES
    ('WF-2026-001', '合同审查待人工确认 · LX-2026-004 · 高管竞业限制与保密期权协议', 'CONTRACT_REVIEW', '4', '演示用户-王五', 'PENDING', TIMESTAMP '2026-03-28 09:10:00'),
    ('WF-2026-002', '合同审查待人工确认 · LX-2026-001 · 2026年度云服务框架采购合作协议', 'CONTRACT_REVIEW', '1', '演示用户-王五', 'IN_PROGRESS', TIMESTAMP '2026-03-28 07:20:00'),
    ('WF-2026-003', '合同审查通过 · LX-2026-002 · 新办公园区弱电工程施工合同', 'CONTRACT_REVIEW', '2', '演示用户-钱七', 'COMPLETED', TIMESTAMP '2026-03-26 14:00:00'),
    ('WF-2026-004', '合同审查退回修改 · LX-2026-003 · 测试用人事外包补充协议', 'CONTRACT_REVIEW', '3', '演示用户-赵六', 'REJECTED', TIMESTAMP '2026-03-20 11:00:00');
